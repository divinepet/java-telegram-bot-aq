package com.divinepet.app;

import com.divinepet.logic.Bot;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    static Logger logger = Logger.getLogger(Main.class);
    @SneakyThrows
    public static void main(String[] args) {
        try {
            Thread pingThread = new Thread(new PingThread());
            pingThread.start();
            BasicConfigurator.configure();
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new Bot(dataSource()));
            logger.info("Bot started");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static HikariDataSource dataSource() throws SQLException, IOException {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(System.getenv("db_url"));
        dataSource.setUsername(System.getenv("db_user"));
        dataSource.setPassword(System.getenv("db_pass"));
        dataSource.setDriverClassName("org.postgresql.Driver");
        Statement st = dataSource.getConnection().createStatement();
        st.execute(new String(Files.readAllBytes(Paths.get("./src/main/resources/schema.sql"))));
        return dataSource;
    }
}
