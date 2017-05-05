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
import com.wire.bots.sdk.server.model.NewBot;

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
            Statement statement = connection.createStatement();

            int update = statement.executeUpdate("CREATE TABLE IF NOT EXISTS Message " +
                    "(Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " BotId STRING," +
                    " UserId STRING," +
                    " Text STRING," +
                    " Asset_key STRING," +
                    " Token STRING," +
                    " Otr_key BLOB," +
                    " Mime_type STRING," +
                    " Size INTEGER," +
                    " Sha256 BLOB," +
                    " Time INTEGER)");
            if (update > 0)
                Logger.info("CREATED TABLE Message");

            update = statement.executeUpdate("CREATE TABLE IF NOT EXISTS Broadcast " +
                    "(Id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " Text STRING," +
                    " Asset BLOB," +
                    " Url STRING," +
                    " Title STRING," +
                    " Asset_key STRING," +
                    " Token STRING," +
                    " Otr_key BLOB," +
                    " Mime_type STRING," +
                    " Size INTEGER," +
                    " Sha256 BLOB," +
                    " Time INTEGER)");
            if (update > 0)
                Logger.info("CREATED TABLE Broadcast");

            try {
                update = statement.executeUpdate("ALTER TABLE Broadcast ADD MessageId STRING");
                if (update > 0)
                    Logger.info("ALTERED TABLE Broadcast");
            } catch (SQLException ignore) {
                //ignore
            }

            update = statement.executeUpdate("CREATE TABLE IF NOT EXISTS User " +
                    "(BotId STRING PRIMARY KEY," +
                    " UserId STRING," +
                    " Locale STRING," +
                    " Name STRING," +
                    " Time INTEGER)");

            if (update > 0)
                Logger.info("CREATED TABLE User");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Channel " +
                            "(Name STRING PRIMARY KEY NOT NULL," +
                            " Welcome STRING," +
                            " Admin STRING," +
                            " Origin STRING," +
                            " Token STRING NOT NULL)"
            );

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Bot2Channel " +
                            "(BotId STRING PRIMARY KEY," +
                            " Channel STRING)"
            );
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    public int insertMessage(Message msg) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Message " +
                    "(BotId, UserId, Text, Asset_key, Token, Otr_key, Mime_type, Size , Sha256, Time) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            return stm.executeUpdate();
        }
    }

    public ArrayList<Message> getMessages() throws SQLException {
        ArrayList<Message> ret = new ArrayList<>();
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM Message");
            while (rs.next()) {
                Message msg = new Message();
                msg.setBotId(rs.getString("BotId"));
                msg.setUserId(rs.getString("UserId"));
                msg.setText(rs.getString("Text"));
                msg.setAssetKey(rs.getString("Asset_key"));
                msg.setToken(rs.getString("Token"));
                msg.setTime(rs.getInt("Time"));
                ret.add(msg);
            }
        }
        return ret;
    }

    public int insertBroadcast(Broadcast broadcast) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Broadcast " +
                    "(Text, Asset, Url, Title, Asset_key, Token, Otr_key, Mime_type, Size , Sha256, Time, MessageId) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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

            return stm.executeUpdate();
        }
    }

    public ArrayList<Broadcast> getBroadcast(long from) throws SQLException {
        ArrayList<Broadcast> ret = new ArrayList<>();
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            String sql = String.format("SELECT * FROM Broadcast WHERE Time > %d ORDER by Time ASC",
                    from);
            ResultSet rs = statement.executeQuery(sql);
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
                ret.add(b);
            }
        }
        return ret;
    }

    public ArrayList<Broadcast> getLastAsset(int limit) throws SQLException {
        ArrayList<Broadcast> ret = new ArrayList<>();
        try (Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            String sql = String.format("SELECT * FROM Broadcast " +
                            "WHERE asset IS NOT NULL " +
                            "ORDER BY Time DESC " +
                            "LIMIT %d",
                    limit);
            ResultSet rs = statement.executeQuery(sql);
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
                ret.add(b);
            }
        }
        return ret;
    }

    public int insertUser(NewBot newBot) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO User " +
                    "(BotId, UserId, Locale, Name, Time) " +
                    "VALUES(?, ?, ?, ?, ?)";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, newBot.id);
            stm.setString(2, newBot.origin.id);
            stm.setString(3, newBot.locale);
            stm.setString(4, newBot.origin.name);
            stm.setLong(5, new Date().getTime() / 1000);

            return stm.executeUpdate();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(String.format("jdbc:sqlite:%s", path));
    }

    public int deleteBroadcast(String messageId) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "DELETE FROM Broadcast WHERE messageId = ?";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, messageId);
            return stm.executeUpdate();
        }
    }

    public void insertBot2Channel(String channelName, String botId) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "INSERT INTO Bot2Channel " +
                    "(BotId, Channel) " +
                    "VALUES(?, ?)";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, botId);
            stm.setString(2, channelName);
            stm.executeUpdate();
        }
    }

    public ArrayList<String> getSubscribers(String channelName) throws SQLException {
        ArrayList<String> ret = new ArrayList<>();
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("SELECT BotId FROM Bot2Channel WHERE Channel = ?");
            stm.setString(1, channelName);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                String botId = rs.getString("BotId");
                ret.add(botId);
            }
            return ret;
        }
    }

    public void updateChannel(String channelName, String token, String origin) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "REPLACE INTO Channel " +
                    "(Name, Token, Origin) " +
                    "VALUES(?, ?, ?)";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, channelName);
            stm.setString(2, token);
            stm.setString(3, origin);
            stm.executeUpdate();
        }
    }

    public void setAdminChannel(String channelName, String admin) throws SQLException {
        try (Connection connection = getConnection()) {
            String cmd = "UPDATE Channel " +
                    "SET Admin = ? " +
                    "WHERE Name = ?";
            PreparedStatement stm = connection.prepareStatement(cmd);
            stm.setString(1, admin);
            stm.setString(2, channelName);
            stm.executeUpdate();
        }
    }

    public Channel getChannel(String name) throws SQLException {
        Channel ret = new Channel();
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("SELECT * FROM Channel WHERE Name = ?");
            stm.setString(1, name);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                ret.name = rs.getString("Name");
                ret.token = rs.getString("Token");
                ret.admin = rs.getString("Admin");
                ret.origin = rs.getString("Origin");
            }
        }
        return ret;
    }

    public String getChannelName(String botId) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("SELECT Channel FROM Bot2Channel WHERE BotId = ?");
            stm.setString(1, botId);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                return rs.getString("Channel");
            }
            return null;
        }
    }

    public boolean removeSubscriber(String botId) throws SQLException {
        try (Connection conn = getConnection()) {
            PreparedStatement stm = conn.prepareStatement("DELETE FROM Bot2Channel WHERE BotId = ?");
            stm.setString(1, botId);
            return stm.execute();
        }
    }
}
