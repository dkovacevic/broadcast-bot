package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.sdk.Configuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class Database {
    private final Configuration.DB conf;

    public Database(Configuration.DB postgres) {
        this.conf = postgres;
    }

    public boolean insertSubscriber(String botId, String channelId) throws Exception {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("INSERT INTO Subscribers (botId, channel) VALUES (?, ?) ON CONFLICT (botId) DO NOTHING");
            stmt.setObject(1, UUID.fromString(botId));
            stmt.setString(2, channelId);
            return stmt.executeUpdate() == 1;
        }
    }

    ArrayList<String> getSubscribers(String channelId) throws Exception {
        ArrayList<String> ret = new ArrayList<>();
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("SELECT botId FROM Subscribers WHERE channel = ?");
            stmt.setString(1, channelId);
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                ret.add(resultSet.getString("botId"));
            }
        }
        return ret;
    }

    Channel getSubscribedChannel(String botId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement(
                    "SELECT c.id, c.name, c.token, c.admin, c.whitelist, c.introText, c.introPicture " +
                            "FROM Subscribers s, Channels c " +
                            "WHERE s.botId = ? AND s.channel = c.id");

            stmt.setObject(1, UUID.fromString(botId));
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                Channel channel = new Channel();
                channel.id = resultSet.getString("id");
                channel.name = resultSet.getString("name");
                channel.token = resultSet.getString("token");
                channel.admin = resultSet.getString("admin");
                channel.whitelist = resultSet.getString("whitelist");
                channel.introText = resultSet.getString("introText");
                channel.introPic = resultSet.getString("introPicture");

                return channel;
            }
        }
        return null;
    }

    public Channel getChannel(String channelId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement(
                    "SELECT id, name, token, admin, whitelist, introText, introPicture " +
                            "FROM Channels " +
                            "WHERE id = ?");

            stmt.setString(1, channelId);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                Channel channel = new Channel();
                channel.id = resultSet.getString("id");
                channel.name = resultSet.getString("name");
                channel.token = resultSet.getString("token");
                channel.admin = resultSet.getString("admin");
                channel.whitelist = resultSet.getString("whitelist");
                channel.introText = resultSet.getString("introText");
                channel.introPic = resultSet.getString("introPicture");

                return channel;
            }
        }
        return null;
    }

    public boolean unsubscribe(String botId) throws SQLException {
        try (Connection c = newConnection()) {
            PreparedStatement stmt = c.prepareStatement("DELETE FROM Subscribers WHERE botId = ?");
            stmt.setObject(1, UUID.fromString(botId));
            return stmt.executeUpdate() == 1;
        }
    }

    private Connection newConnection() throws SQLException {
        String url = String.format("jdbc:postgresql://%s:%d/%s", conf.host, conf.port, conf.database);
        return DriverManager.getConnection(url, conf.user, conf.password);
    }
}
