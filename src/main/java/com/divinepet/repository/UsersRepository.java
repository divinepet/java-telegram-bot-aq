package com.divinepet.repository;

import com.divinepet.model.State;
import com.divinepet.model.User;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;

public class UsersRepository extends JdbcTemplate {
    private final String usersTable = System.getenv("db_users_table");
    private final String stateTable = System.getenv("db_state_table");
    private final RowMapper<User> USERS_ROW_MAPPER;
    private final RowMapper<State> STATE_ROW_MAPPER;

    public UsersRepository(HikariDataSource dataSource) {
        super.setDataSource(dataSource);
        this.STATE_ROW_MAPPER = (ResultSet rs, int rowNum)
                -> new State(rs.getString("updid"), rs.getString("sender_id"), rs.getString("receiver_id"),
                rs.getBoolean("wait_answer"), rs.getLong("edit_msg_id"), rs.getString("edit_msg"));
        this.USERS_ROW_MAPPER = (ResultSet rs, int rowNum)
                -> new User(rs.getString("id"), rs.getString("username"),
                rs.getString("firstname"), rs.getString("lastname"), rs.getArray("blocked"));
    }

    public Optional<User> findById(String id) {
        try {
            User user = super.queryForObject(format("SELECT * FROM %s WHERE id = '%s';", usersTable, id), USERS_ROW_MAPPER);
            return (Optional.ofNullable(user));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(User user)  {
        super.update(format("INSERT INTO %s (id, username, firstname, lastname)" +
                        "VALUES ('%s', '%s', '%s', '%s');",
                usersTable, user.getId(), user.getUsername(), user.getFirstname(), user.getLastname()));
    }

    public void blockUser(String currentID, String blockID) {
        super.update(format("UPDATE %s " +
                "SET blocked = array_append(blocked, '%s') " +
                "WHERE id = '%s';", usersTable, blockID, currentID));
    }

    public void unblockUser(String currentID, String blockID) {
        super.update(format("UPDATE %s " +
                "SET blocked = array_remove(blocked, '%s') " +
                "WHERE id = '%s';", usersTable, blockID, currentID));
    }

    public boolean isUserBlocked(String currentID, String checkingID) throws SQLException {
        List<String> blockedUsers = Arrays.asList((String[])super.queryForObject(format(
                "select blocked from %s where id='%s'",
                        usersTable, currentID), Array.class)
            .getArray());
        return blockedUsers.contains(checkingID);
    }

    public Optional<State> getState(String updID) {
        try {
            State state = super.queryForObject(format("SELECT * FROM %s WHERE updid = '%s';", stateTable, updID), STATE_ROW_MAPPER);
            return (Optional.ofNullable(state));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void setWaitAnswerState(String updID, long message_id, String answer) {
        super.update(format("UPDATE %s " +
                "SET wait_answer = true, edit_msg_id = %d, edit_msg = '%s' WHERE updid = '%s';",
                stateTable, message_id, answer, updID));
    }

    public void disableWaitAnswer(String updID) {
        super.update(format("UPDATE %s " +
                        "SET wait_answer = false WHERE updid = '%s';",
                stateTable, updID));
    }

    public void removeState(String updID) {
        super.update(format("DELETE FROM %s WHERE updid = '%s';",
                stateTable, updID));
    }

    public String getCorrectUpdId(String receiverID) {
        List<String> data = super.queryForList(format("SELECT updid FROM %s WHERE (receiver_id = '%s' AND wait_answer = %b);", stateTable, receiverID, true),String.class);
        return data.get(data.size() - 1);
    }

    public void setState(String updID, String senderID, String receiverID, boolean wait_answer, long editMsgID, String editMsg) {
        super.update(format("INSERT INTO %s (updid, sender_id, receiver_id, wait_answer, edit_msg_id, edit_msg)" +
                        "VALUES ('%s', '%s', '%s', %b, %d, '%s');",
                stateTable, updID, senderID, receiverID, wait_answer, editMsgID, editMsg));
    }
}
