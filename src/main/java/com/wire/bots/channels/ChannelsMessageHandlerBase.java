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

package com.wire.bots.channels;

import com.waz.model.Messages;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Message;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.NewBot;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

abstract class ChannelsMessageHandlerBase extends MessageHandlerBase {
    protected final Broadcaster broadcaster;

    protected ChannelsMessageHandlerBase(ClientRepo repo) {
        broadcaster = new Broadcaster(repo);
    }

    abstract protected void broadcast(Channel channel, TextMessage msg) throws Exception;

    abstract protected void broadcast(Channel channel, ImageMessage msg, byte[] bytes) throws Exception;

    abstract protected void onNewSubscriber(Channel channel, NewBot newBot) throws Exception;

    abstract protected void onNewFeedback(Channel channel, TextMessage msg) throws Exception;

    abstract protected void onNewFeedback(Channel channel, ImageMessage msg) throws Exception;

    @Override
    public boolean onNewBot(NewBot newBot) {
        try {
            Channel channel = getChannel(newBot.id);
            onNewSubscriber(channel, newBot);
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
        return true;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);

            if (botId.equals(channel.admin)) {
                if (!processCommand(channel.name, msg.getText(), client)) {
                    broadcast(channel, msg);
                }
            } else {
                saveMessage(botId, msg, channel.name);

                if (!channel.muted)
                    onNewFeedback(channel, msg);
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);

            if (botId.equals(channel.admin)) {
                byte[] bytes = client.downloadAsset(msg.getAssetKey(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
                broadcast(channel, msg, bytes);
            } else {
                saveMessage(botId, msg, channel.name);

                if (!channel.muted)
                    onNewFeedback(channel, msg);
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);
            if (botId.equals(channel.admin)) {
                String msg = String.format("This is Admin Conversation for the Channel: **%s**." +
                                "\nYou should rename this conversation to something like: `Admin %s`." +
                                "\nUse this conversation to broadcast. Don't leave or delete it!" +
                                "\nOthers can subscribe to this channel by clicking on: wire.com/b/%s" +
                                "\nType: /help",
                        channel.name,
                        channel.name,
                        channel.name);
                client.sendText(msg);
                return;
            }

            String label = channel.welcome != null ? channel.welcome : String.format("This is **%s** channel", channel.name);
            client.sendText(label);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onBotRemoved(String botId) {
        try {
            Service.dbManager.removeSubscriber(botId);
            Logger.info("Removed Subscriber: %s", botId);
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        try {
            Channel channel = getChannel(client.getId());
            if (!channel.muted)
                broadcaster.sendOnMemberFeedback(channel.admin, "**%s** joined", userIds);
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        try {
            Channel channel = getChannel(client.getId());
            if (!channel.muted)
                broadcaster.sendOnMemberFeedback(channel.admin, "**%s** left", userIds);
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onEvent(WireClient client, String userId, Messages.GenericMessage msg) {
        String botId = client.getId();
        try {
            Channel channel = getChannel(botId);

            if (msg.hasReaction()) {
                onMemberFeedbackLike(channel, userId);
            }

            if (msg.hasDeleted() && botId.equals(channel.admin)) {
                broadcaster.revokeBroadcast(channel.name, msg.getDeleted().getMessageId());
            }
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        }
    }

    private void onMemberFeedbackLike(Channel channel, String userId) {
        ArrayList<String> userIds = new ArrayList<>();
        userIds.add(userId);
        if (!channel.muted)
            broadcaster.sendOnMemberFeedback(channel.admin, "**%s** liked", userIds);
    }

    private void saveMessage(String botId, ImageMessage msg, String channel) throws SQLException {
        try {
            Message m = new Message();
            m.setBotId(botId);
            m.setUserId(msg.getUserId());
            m.setAssetKey(msg.getAssetKey());
            m.setToken(msg.getAssetToken());
            m.setOtrKey(msg.getOtrKey());
            m.setSha256(msg.getSha256());
            m.setSize(msg.getSize());
            m.setTime(new Date().getTime() / 1000);
            m.setMimeType(msg.getMimeType());
            m.setChannel(channel);

            Service.dbManager.insertMessage(m);
        } catch (SQLException e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void saveMessage(String botId, TextMessage msg, String channel) {
        try {
            Message m = new Message();
            m.setBotId(botId);
            m.setUserId(msg.getUserId());
            m.setText(msg.getText());
            m.setMimeType("text");
            m.setChannel(channel);
            Service.dbManager.insertMessage(m);
        } catch (SQLException e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private boolean processCommand(String channelName, String text, WireClient client) throws Exception {
        boolean ret = false;
        if (text.startsWith("/"))
            ret = true;

        if (text.startsWith("/help")) {
            String h = "List of available commands:\n" +
                    "`/welcome <text>` Update text that is shown to new subscribers when they join\n" +
                    "`/mute` Mute all incoming messages from subscribers\n" +
                    "`/unmute` Unmute all incoming messages from subscribers\n" +
                    "`/stats` Show some statistics: #posts, #subscribers, #feedbacks ...";

            client.sendText(h);
        }
        if (text.startsWith("/welcome")) {
            Service.dbManager.updateChannel(channelName, "welcome", text.replace("/welcome", "").trim());
            client.sendText("Updated `welcome` message");
        }
        if (text.equalsIgnoreCase("/mute")) {
            Service.dbManager.updateChannel(channelName, "muted", 1);
            client.sendText("You won't receive info about subscribers' activity anymore. Type `/unmute` to resume");
        }
        if (text.equalsIgnoreCase("/unmute")) {
            Service.dbManager.updateChannel(channelName, "muted", 0);
            client.sendText("Resumed. Type `/mute` to mute");
        }
        if (text.equalsIgnoreCase("/stats")) {
            int subscribers = Service.dbManager.getSubscribers(channelName).size();
            int posts = Service.dbManager.getBroadcasts(channelName).size();
            int messages = Service.dbManager.getMessages(channelName).size();

            String msg = String.format("```\n" +
                            "Subscribers: %,d\n" +
                            "Messages:    %,d\n" +
                            "Posts:       %,d\n" +
                            "```",
                    subscribers,
                    messages,
                    posts);
            client.sendText(msg);
        }
        return ret;
    }

    protected Channel getChannel(String botId) throws SQLException {
        String channelName = Service.dbManager.getChannelName(botId);
        return Service.dbManager.getChannel(channelName);
    }
}
