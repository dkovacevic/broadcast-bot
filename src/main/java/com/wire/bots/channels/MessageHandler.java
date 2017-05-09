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
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

class MessageHandler extends MessageHandlerBase {
    protected final Broadcaster broadcaster;

    MessageHandler(ClientRepo repo) {
        broadcaster = new Broadcaster(repo);
    }

    @Override
    public boolean onNewBot(NewBot newBot) {
        try {
            String botId = newBot.id;
            Channel channel = getChannel(botId);
            User origin = newBot.origin;
            Logger.info("onNewSubscriber: channel: %s, bot: %s, origin: %s, %s",
                    channel.name,
                    botId,
                    origin.id,
                    origin.name
            );

            for (Member member : newBot.conversation.members) {
                if (member.service != null) {
                    Logger.warning("Rejecting NewBot. Provider: %s service: %s",
                            member.service.provider,
                            member.service.id);
                    return false; // we don't want to be in a conv if other bots are there.
                }
            }

            int id = Service.dbManager.getLastBroadcast(channel.name);
            Service.dbManager.updateBot(botId, "Last", ++id);

            if (!channel.muted) {
                broadcaster.sendToAdminConv(channel.admin, origin.name);
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
        return true;
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
                                "\n`Type: /help`",
                        channel.name,
                        channel.name,
                        channel.name);
                client.sendText(msg);
                return;
            }

            if (channel.introPic != null) {
                Picture picture = new Picture(channel.introPic);
                AssetKey assetKey = client.uploadAsset(picture);
                picture.setAssetKey(assetKey.key);
                picture.setAssetToken(assetKey.token);
                client.sendPicture(picture);
            }

            String label = channel.introText != null ? channel.introText : String.format("This is **%s** channel", channel.name);
            client.sendText(label + "\n`Type: /help`");
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);

            if (botId.equals(channel.admin)) {
                if (!processAdminCmd(channel.name, msg.getText(), client)) {
                    broadcaster.broadcast(channel, msg);
                }
            } else {
                if (!processSubscriberCmd(msg.getText(), client)) {

                    saveMessage(botId, msg, channel.name);

                    if (!channel.muted)
                        broadcaster.sendToAdminConv(channel.admin, msg);
                }
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
                broadcaster.broadcast(channel.name, msg, bytes);
            } else {
                saveMessage(botId, msg, channel.name);

                if (!channel.muted)
                    broadcaster.sendToAdminConv(channel.admin, msg);
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
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
                broadcaster.sendToAdminConv(channel.admin, "**%s** joined", userIds);
        } catch (SQLException e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        try {
            Channel channel = getChannel(client.getId());
            if (!channel.muted)
                broadcaster.sendToAdminConv(channel.admin, "**%s** left", userIds);
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
            broadcaster.sendToAdminConv(channel.admin, "**%s** liked", userIds);
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

    private boolean processAdminCmd(String channelName, String cmd, WireClient adminClient) throws Exception {
        boolean ret = false;
        if (cmd.startsWith("/"))
            ret = true;

        if (cmd.equalsIgnoreCase("/help")) {
            String h = "List of available commands:\n" +
                    "`/intro <text>` Set **Intro Text** for new Subscribers\n" +
                    "`/intro <url>`  Set **Intro Picture** for new Subscribers\n" +
                    "`/mute`         **Mute** all incoming messages from Subscribers\n" +
                    "`/unmute`       **Unmute** all incoming messages from Subscribers\n" +
                    "`/stats`        Show some **statistics**: #posts, #subscribers, #feedbacks ...";

            adminClient.sendText(h);
            return ret;
        }
        if (cmd.startsWith("/intro")) {
            String intro = cmd.replace("/intro", "").trim();
            if (intro.startsWith("http")) {
                Service.dbManager.updateChannel(channelName, "intro", intro);
                adminClient.sendText("Updated `intro picture`");
            } else {
                Service.dbManager.updateChannel(channelName, "welcome", intro);
                adminClient.sendText("Updated `intro text`");
            }
            return ret;
        }
        if (cmd.equalsIgnoreCase("/mute")) {
            Service.dbManager.updateChannel(channelName, "muted", 1);
            adminClient.sendText("You won't receive info about subscribers' activity anymore. Type `/unmute` to resume");
            return ret;
        }
        if (cmd.equalsIgnoreCase("/unmute")) {
            Service.dbManager.updateChannel(channelName, "muted", 0);
            adminClient.sendText("Resumed. Type `/mute` to mute");
            return ret;
        }
        if (cmd.equalsIgnoreCase("/stats")) {
            int subscribers = Service.dbManager.getSubscribers(channelName).size();
            int posts = Service.dbManager.getBroadcasts(channelName, Integer.MAX_VALUE, Integer.MAX_VALUE).size();
            int messages = Service.dbManager.getMessages(channelName).size();

            String msg = String.format("```\n" +
                            "Subscribers: %,d\n" +
                            "Messages:    %,d\n" +
                            "Posts:       %,d\n" +
                            "```",
                    subscribers,
                    messages,
                    posts);
            adminClient.sendText(msg);
            return ret;
        }

        if (ret) {
            adminClient.sendText("Unknown command: `" + cmd + "`");
        }
        return ret;
    }

    private boolean processSubscriberCmd(String cmd, WireClient client) throws Exception {
        switch (cmd) {
            case "/help": {
                String h = "List of available commands:\n" +
                        "`/prev`   Show 5 previous posts\n" +
                        "`/mute`   Mute all new posts\n" +
                        "`/unmute` Resume posts in this channel";
                client.sendText(h);
                return true;
            }
            case "/prev": {
                broadcaster.followBack(client, 5);
                return true;
            }
            case "/mute": {
                Service.dbManager.updateBot(client.getId(), "Muted", 1);
                client.sendText("You won't receive posts here anymore. Type: `/unmute` to resume");
                return true;
            }
            case "/unmute": {
                Service.dbManager.updateBot(client.getId(), "Muted", 0);
                client.sendText("Posts resumed");
                return true;
            }
            default: {
                if (cmd.startsWith("/")) {
                    client.sendText("Unknown command: `" + cmd + "`");
                    return true;
                }
                return false;
            }
        }
    }

    protected Channel getChannel(String botId) throws SQLException {
        String channelName = Service.dbManager.getChannelName(botId);
        return Service.dbManager.getChannel(channelName);
    }
}
