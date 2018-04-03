package com.wire.bots.channels;

import com.wire.bots.sdk.Configuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class Database {
    private final Configuration.DB conf;

    public Database(Configuration.DB conf) {
        this.conf = conf;
    }

    public boolean insertSubscriber(String botId, String channelName) throws Exception {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("INSERT INTO Channels (botId, channel) VALUES (?, ?) ON CONFLICT (botId) DO NOTHING");
            stmt.setObject(1, UUID.fromString(botId));
            stmt.setString(2, channelName);
            return stmt.executeUpdate() == 1;
        }
    }

    ArrayList<String> getSubscribers(String channelName) throws Exception {
        ArrayList<String> ret = new ArrayList<>();
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT botId FROM Channels WHERE channel = ?");
            stmt.setString(1, channelName);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                ret.add(resultSet.getString("botId"));
            }
        }
        return ret;
    }

    private Connection newConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s", conf.host, conf.port, conf.database);
        return DriverManager.getConnection(url, conf.user, conf.password);
    }

    String getChannel(String botId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT channel FROM Channels WHERE botId = ?");
            stmt.setObject(1, UUID.fromString(botId));
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("channel");
            }
        }
        return null;
    }

    public boolean unsubscribe(String botId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("DELETE FROM Channels WHERE botId = ?");
            stmt.setObject(1, UUID.fromString(botId));
            return stmt.executeUpdate() == 1;
        }
    }
}
