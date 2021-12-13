package com.divinepet.logic;

import com.divinepet.model.State;
import com.divinepet.model.User;
import com.divinepet.model.Pair;
import com.divinepet.repository.UsersRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.StrictMath.toIntExact;

public class Bot extends TelegramLongPollingBot {
    HikariDataSource dataSource;
    UsersRepository usersRepository;
    static HashMap<String, Pair> waitForQueState = new HashMap<>();
    static HashMap<String, String> currentReceiver = new HashMap<>();
    static final String welcomeMsg = "Привет, Я бот HaskMe!\n" +
                                        "Я умею получать и отправлять анонимные вопросы";
    static final String helpMsg = "▪️ Если будет нужна помощь во время работы с ботом, воспользуйся командой /help\n" +
                                    "▪️ Команда для получения своей ссылки для вопросов - /getlink\n";
    static final String startMsg = "Чтобы начать, просто размести свою ссылку в публичном доступе.";

    public Bot(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.usersRepository = new UsersRepository(dataSource);
    }


    @Override
    @SneakyThrows
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().hasText()) {
            saveUser(update.getMessage().getFrom());
            Message incomeMsg = update.getMessage();
            SendMessage message = new SendMessage();
            String senderID = incomeMsg.getChatId().toString();


            if (incomeMsg.hasEntities()) {
                message.setChatId(senderID);
                if (incomeMsg.getEntities().get(0).getText().equals("/start")) {
                    if (incomeMsg.getText().length() > 7)
                        askUserForQuestion(senderID, message, incomeMsg);
                    else
                        sendWelcomeMessage(message);
                } else if (incomeMsg.getEntities().get(0).getText().equals("/getlink")) {
                    sendLinkForQuestions(message, senderID);
                } else if (incomeMsg.getEntities().get(0).getText().equals("/help")) {
                    sendHelpMessages(message);
                }
            } else if (waitForQueState.get(senderID) != null
                    && waitForQueState.get(senderID).getKey()) {
                confirmSentQuestion(update, senderID, incomeMsg);
            } else {
                String UpdID = usersRepository.getCorrectUpdId(senderID);
                if (UpdID != null) {
                    confirmAnswerOnQuestion(UpdID, senderID, incomeMsg);
                }
            }
        } else if (update.hasCallbackQuery()) {
            saveUser(update.getCallbackQuery().getFrom());
            String call_data = update.getCallbackQuery().getData();
            long message_id = update.getCallbackQuery().getMessage().getMessageId();
            String chat_id = update.getCallbackQuery().getMessage().getChatId().toString();

            if (call_data.startsWith("callbackdata")) {
                answerOnQuestionHandle(call_data, update, message_id, chat_id);
            } else if (call_data.startsWith("reply")) {
                reSendQuestionHandle(call_data, update, chat_id);
            } else if (call_data.startsWith("block")) {
                blockUserHandle(call_data, update, message_id, chat_id);
            } else if (call_data.startsWith("unblock")) {
                unblockUserHandler(call_data, update, message_id, chat_id);
            }
        }
    }

    @SneakyThrows
    private void sendHelpMessages(SendMessage message) {
        message.setText("▪️ Чтобы получить свою ссылку, воспользуйся командой /getlink\n" +
                        "▪️ Чтобы начать получать вопросы, размести ссылку в любом публичном месте, например, в инстаграме\n" +
                        "▪️ Вопросы всегда анонимные\n" +
                        "▪️ После ответа, человек, задавший вопрос, получит уведомление о твоём ответе\n" +
                        "▪️ После блокировки вам не будут приходить вопросы от этого человека\n" +
                        "▪️ Если остались вопросы - напиши автору @divinepet"
        );
        execute(message);
    }

    @SneakyThrows
    private void unblockUserHandler(String call_data, Update update, long message_id, String chat_id) {
        String updID = call_data.substring(call_data.indexOf("#") + 1);
        String answer = update.getCallbackQuery().getMessage().getText();

        State state = usersRepository.getState(updID).get();
        usersRepository.unblockUser(state.getReceiverID(), state.getSenderID());


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton answerBtn = new InlineKeyboardButton("Ответить");
        answerBtn.setCallbackData("callbackdata" + " #" + updID);

        InlineKeyboardButton blockBtn = new InlineKeyboardButton("Заблокировать");
        blockBtn.setCallbackData("block" + " #" + updID);

        keyboard.add(List.of(answerBtn));
        keyboard.add(List.of(blockBtn));
        inlineKeyboardMarkup.setKeyboard(keyboard);


        EditMessageText new_message = new EditMessageText();


        new_message.setReplyMarkup(inlineKeyboardMarkup);
        new_message.setChatId(chat_id);
        new_message.setMessageId(toIntExact(message_id));
        new_message.setText(answer.substring(0, answer.indexOf("\uD83D\uDEAB")));
        execute(new_message);
    }

    @SneakyThrows
    private void blockUserHandle(String call_data, Update update, long message_id, String chat_id) {
        String updID = call_data.substring(call_data.indexOf("#") + 1);
        String answer = update.getCallbackQuery().getMessage().getText();
        State currentState = usersRepository.getState(updID).get();

        if (!usersRepository.isUserBlocked(currentState.getReceiverID(), currentState.getSenderID()))
            usersRepository.blockUser(currentState.getReceiverID(), currentState.getSenderID());

        EditMessageText new_message = new EditMessageText();

        new_message.setReplyMarkup(setUnblockUserKeyboard(updID));
        new_message.setChatId(chat_id);
        new_message.setMessageId(toIntExact(message_id));
        new_message.setText(answer + "\n" + "\uD83D\uDEAB Пользователь заблокирован");
        execute(new_message);
    }

    @SneakyThrows
    private void reSendQuestionHandle(String call_data, Update update, String chat_id) {
        SendMessage message = new SendMessage();
        message.setChatId(chat_id);
        message.setText("Напиши свой вопрос ⌛");

        currentReceiver.put(update.getCallbackQuery().getMessage().getChatId().toString(),
                call_data.substring(call_data.indexOf("#") + 1));
        Message msg = execute(message);
        waitForQueState.put(update.getCallbackQuery().getMessage().getChatId().toString(), new Pair(true, msg.getMessageId().toString()));
    }

    @SneakyThrows
    private void answerOnQuestionHandle(String call_data, Update update, long message_id, String chat_id) {
        String updID = call_data.substring(call_data.indexOf("#") + 1);
        String answer = update.getCallbackQuery().getMessage().getText();
        EditMessageText new_message = new EditMessageText();

        usersRepository.setWaitAnswerState(updID, message_id, answer);

        new_message.setChatId(chat_id);
        new_message.setMessageId(toIntExact(message_id));
        new_message.setText(answer + "\n" + "Напиши ответ сообщением ниже 👇");
        execute(new_message);
    }

    @SneakyThrows
    private void confirmAnswerOnQuestion(String UpdID, String senderID, Message incomeMsg) {
        State state = usersRepository.getState(UpdID).get();
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(senderID);
        editMessageText.setMessageId(toIntExact(state.getEditMsgID()));
        editMessageText.setText(state.getEditMsg() + "\n" + "✅ Вы ответили:\n" + incomeMsg.getText());
        execute(editMessageText);
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(senderID);
        deleteMessage.setMessageId(incomeMsg.getMessageId());
        execute(deleteMessage);
        usersRepository.disableWaitAnswer(UpdID);
//        list.get(UpdID).setWaitAnswer(false);

        SendMessage answer = new SendMessage();
        String username = (incomeMsg.getFrom().getUserName() != null)
                ? "@" + incomeMsg.getFrom().getUserName()
                : incomeMsg.getFrom().getFirstName();

        answer.setChatId(state.getSenderID());
        answer.setText(username + " ответил(а) на твой вопрос:\n" + incomeMsg.getText());
        execute(answer);
        usersRepository.removeState(UpdID);
    }

    @SneakyThrows
    private void confirmSentQuestion(Update update, String senderID, Message incomeMsg) {

        if (!usersRepository.findById(currentReceiver.get(update.getMessage().getChatId().toString())).isPresent()
                || !usersRepository.isUserBlocked(currentReceiver.get(update.getMessage().getChatId().toString()), senderID))
            sendQuestion(update);

        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(senderID);
        deleteMessage.setMessageId(incomeMsg.getMessageId());
        execute(deleteMessage);

        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setChatId(senderID);
        editMessageText.setReplyMarkup(setRepeatQuestionKeyboard(currentReceiver.get(senderID)));
        editMessageText.enableHtml(true);
        editMessageText.setMessageId(Integer.parseInt(waitForQueState.get(senderID).getValue()));
        editMessageText.setText("✅ Вопрос отправлен: <b>" + incomeMsg.getText() + "</b>");
        execute(editMessageText);
        waitForQueState.put(senderID, new Pair(false, null));
    }

    @SneakyThrows
    private void sendLinkForQuestions(SendMessage message, String senderID) {
        message.setText("Твоя ссылка для получения вопросов:\n" +
                "https://t.me/" + getBotUsername() + "?start=" + senderID);
        execute(message);
    }

    @SneakyThrows
    private void sendWelcomeMessage(SendMessage message) {
        message.setText(welcomeMsg);
        execute(message);
        message.setText(helpMsg);
        execute(message);
        message.setText(startMsg);
        execute(message);
    }

    @SneakyThrows
    private void askUserForQuestion(String senderID, SendMessage message, Message incomeMsg) {
        message.setText("Напиши свой вопрос ⌛");
        currentReceiver.put(senderID,
                incomeMsg.getText().substring(incomeMsg.getText().indexOf(" ") + 1));
        Message msg = execute(message);
        waitForQueState.put(senderID, new Pair(true, msg.getMessageId().toString()));
    }

    private void saveUser(org.telegram.telegrambots.meta.api.objects.User user) {
        if (usersRepository.findById(user.getId().toString()).isEmpty()) {
            usersRepository.save(new User(
                    user.getId().toString(),
                    user.getUserName(),
                    user.getFirstName(),
                    user.getLastName(),
                    null
            ));
        }
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }

    @SneakyThrows
    public void sendQuestion(Update upd) {
        SendMessage message = new SendMessage();
        message.setChatId(currentReceiver.get(upd.getMessage().getChatId().toString()));
        message.setText("\uD83D\uDCAC У тебя новый вопрос:\n" + upd.getMessage().getText());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton answerBtn = new InlineKeyboardButton("Ответить");
        answerBtn.setCallbackData("callbackdata" + " #" + upd.getUpdateId());

        InlineKeyboardButton blockBtn = new InlineKeyboardButton("Заблокировать");
        blockBtn.setCallbackData("block" + " #" + upd.getUpdateId());

        keyboard.add(List.of(answerBtn));
        keyboard.add(List.of(blockBtn));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        usersRepository.setState(upd.getUpdateId().toString(), upd.getMessage().getChatId().toString(),
                currentReceiver.get(upd.getMessage().getChatId().toString()), false, 0, null);
        execute(message);
    }

    public InlineKeyboardMarkup setRepeatQuestionKeyboard(String receiverID) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton qBtn = new InlineKeyboardButton("Задать новый вопрос");
        qBtn.setCallbackData("reply" + " #" + receiverID);
        keyboard.add(List.of(qBtn));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    public InlineKeyboardMarkup setUnblockUserKeyboard(String updID) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        InlineKeyboardButton bBtn = new InlineKeyboardButton("Разблокировать");
        bBtn.setCallbackData("unblock" + " #" + updID);
        keyboard.add(List.of(bBtn));
        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    @Override
    public String getBotUsername() {
        return System.getenv("bot_username");
    }

    @Override
    public String getBotToken() {
        return System.getenv("bot_token");
    }

    @Override
    public void onRegister() {
        super.onRegister();
    }
}


