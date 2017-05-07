//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//
package com.wire.bots.channels.storage;

import com.wire.bots.channels.model.Broadcast;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Message;
import com.wire.bots.sdk.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;

public class DbManager {
    private final String path;

    public DbManager(String path) {
        this.path = path;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try (Connection connection = getConnection()) {
            Statement stm = connection.createStatement();

            stm.executeUpdate("CREATE TABLE IF NOT EXISTS Message " +
                    "(Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " BotId STRING," +
                    " Channel STRING," +
                    " UserId STRING," +
                    " Text STRING," +
                    " Asset_key STRING," +
                    " Token STRING," +
                    " Otr_key BLOB," +
                    " Mime_type STRING," +
                    " Size INTEGER," +
                    " Sha256 BLOB," +
                    " Time INTEGER)");

            stm.executeUpdate("CREATE TABLE IF NOT EXISTS Broadcast " +
                    "(Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " Channel STRING," +
                    " Text STRING," +
                    " Asset BLOB," +
                    " Url STRING," +
                    " Title STRING," +
                    " Asset_key STRING," +
                    " MessageId STRING" +
                    " Token STRING," +
                    " Otr_key BLOB," +
                    " Mime_type STRING," +
                    " Size INTEGER," +
                    " Sha256 BLOB," +
                    " Time INTEGER)");

            stm.executeUpdate("CREATE TABLE IF NOT EXISTS Channel " +
                            "(Name STRING PRIMARY KEY NOT NULL," +
                            " Welcome STRING," +
                            " Intro STRING," +
                            " Admin STRING," +
                            " Muted INTEGER," +
                            " Origin STRING NOT NULL," +
                            " Token STRING NOT NULL)"
            );

            stm.executeUpdate("CREATE TABLE IF NOT EXISTS Bots " +
                            "(BotId STRING PRIMARY KEY, " +
                            "Origin STRING NOT NULL, " +
                            "UserName STRING NOT NULL, " +
                            "Conversation STRING NOT NULL, " +
                            "Channel STRING NOT NULL)"
            );
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    public int insertMessage(Message msg) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Message " +
                    "(BotId, UserId, Text, Asset_key, Token, Otr_key, Mime_type, Size , Sha256, Time, Channel) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, msg.getBotId());
            stm.setString(2, msg.getUserId());
            stm.setString(3, msg.getText());
            stm.setString(4, msg.getAssetKey());
            stm.setString(5, msg.getToken());
            stm.setBytes(6, msg.getOtrKey());
            stm.setString(7, msg.getMimeType());
            stm.setLong(8, msg.getSize());
            stm.setBytes(9, msg.getSha256());
            stm.setLong(10, new Date().getTime() / 1000);
            stm.setString(11, msg.getChannel());

            return stm.executeUpdate();
        }
    }

    public ArrayList<Message> getMessages(String channel) throws SQLException {
        ArrayList<Message> ret = new ArrayList<>();
        try (Connection connection = getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Message WHERE Channel = ?");
            stm.setString(1, channel);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Message msg = new Message();
                msg.setBotId(rs.getString("BotId"));
                msg.setUserId(rs.getString("UserId"));
                msg.setText(rs.getString("Text"));
                msg.setAssetKey(rs.getString("Asset_key"));
                msg.setToken(rs.getString("Token"));
                msg.setTime(rs.getInt("Time"));
                msg.setChannel(rs.getString("Channel"));
                ret.add(msg);
            }
        }
        return ret;
    }

    public int insertBroadcast(Broadcast broadcast) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Broadcast " +
                    "(Text, Asset, Url, Title, Asset_key, Token, Otr_key, Mime_type, Size , Sha256, Time, MessageId, Channel) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, broadcast.getText());
            stm.setBytes(2, broadcast.getAssetData());
            stm.setString(3, broadcast.getUrl());
            stm.setString(4, broadcast.getTitle());
            stm.setString(5, broadcast.getAssetKey());
            stm.setString(6, broadcast.getToken());
            stm.setBytes(7, broadcast.getOtrKey());
            stm.setString(8, broadcast.getMimeType());
            stm.setLong(9, broadcast.getSize());
            stm.setBytes(10, broadcast.getSha256());
            stm.setLong(11, new Date().getTime() / 1000);
            stm.setString(12, broadcast.getMessageId());
            stm.setString(13, broadcast.getChannel());

            return stm.executeUpdate();
        }
    }

    public ArrayList<Broadcast> getBroadcasts(String channel) throws SQLException {
        ArrayList<Broadcast> ret = new ArrayList<>();
        try (Connection connection = getConnection()) {
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Broadcast WHERE Channel = ?");
            stm.setString(1, channel);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                Broadcast b = new Broadcast();
                b.setId(rs.getInt("Id"));
                b.setText(rs.getString("Text"));
                b.setUrl(rs.getString("Url"));
                b.setTitle(rs.getString("Title"));
                b.setAssetKey(rs.getString("Asset_key"));
                b.setToken(rs.getString("Token"));
                b.setAssetData(rs.getBytes("Asset"));
                b.setOtrKey(rs.getBytes("Otr_key"));
                b.setSha256(rs.getBytes("Sha256"));
                b.setSize(rs.getInt("Size"));
                b.setMimeType(rs.getString("Mime_type"));
                b.setMessageId(rs.getString("MessageId"));
                b.setTime(rs.getInt("Time"));
                b.setChannel(rs.getString("Channel"));
                ret.add(b);
            }
        }
        return ret;
    }

    public int deleteBroadcast(String messageId) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "DELETE FROM Broadcast WHERE messageId = ?";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, messageId);
            return stm.executeUpdate();
        }
    }

    public void insertNewBot(String botId, String channel, String user, String origin, String conv) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Bots " +
                    "(BotId, Channel, UserName, Origin, Conversation) " +
                    "VALUES(?, ?, ?, ?, ?)";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, botId);
            stm.setString(2, channel);
            stm.setString(3, user);
            stm.setString(4, origin);
            stm.setString(5, conv);
            stm.executeUpdate();
        }
    }

    public ArrayList<String> getSubscribers(String channelName) throws SQLException {
        ArrayList<String> ret = new ArrayList<>();
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("SELECT BotId FROM Bots WHERE Channel = ?");
            stm.setString(1, channelName);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String botId = rs.getString("BotId");
                ret.add(botId);
            }
            return ret;
        }
    }

    public void insertChannel(String channelName, String token, String origin) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Channel " +
                    "(Name, Token, Origin, Welcome) " +
                    "VALUES(?, ?, ?, ?)";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, channelName);
            stm.setString(2, token);
            stm.setString(3, origin);
            stm.setString(4, String.format("This is **%s** channel", channelName));

            stm.executeUpdate();
        }
    }

    public void updateChannel(String channelName, String param, String value) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = String.format("UPDATE Channel " +
                    "SET %s = ? " +
                    "WHERE Name = ?", param);
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, value);
            stm.setString(2, channelName);
            stm.executeUpdate();
        }
    }

    public void updateChannel(String channelName, String param, int value) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = String.format("UPDATE Channel " +
                    "SET %s = ? " +
                    "WHERE Name = ?", param);
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setInt(1, value);
            stm.setString(2, channelName);
            stm.executeUpdate();
        }
    }

    public Channel getChannel(String channelName) throws SQLException {
        Channel ret = null;
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM Channel WHERE Name = ?");
            stm.setString(1, channelName);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                ret = new Channel();
                ret.name = rs.getString("Name");
                ret.token = rs.getString("Token");
                ret.admin = rs.getString("Admin");
                ret.origin = rs.getString("Origin");
                ret.introText = rs.getString("Welcome");
                ret.introPic = rs.getString("Intro");
                ret.muted = rs.getInt("Muted") != 0;
            }
        }
        return ret;
    }

    public String getChannelName(String botId) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("SELECT Channel FROM Bots WHERE BotId = ?");
            stm.setString(1, botId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getString("Channel");
            }
            return null;
        }
    }

    public boolean removeSubscriber(String botId) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("DELETE FROM Bots WHERE BotId = ?");
            stm.setString(1, botId);
            return stm.execute();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", path));
    }

    public boolean deleteChannel(String channelName) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("DELETE from Channel WHERE Name = ?");
            stm.setString(1, channelName);
            return stm.execute();
        }
    }

    public boolean deleteBots(String channelName) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("DELETE from Bots WHERE Channel = ?");
            stm.setString(1, channelName);
            return stm.execute();
        }
    }
}
