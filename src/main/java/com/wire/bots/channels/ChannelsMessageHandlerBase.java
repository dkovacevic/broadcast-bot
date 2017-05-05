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
import com.wire.bots.channels.model.Config;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

abstract class ChannelsMessageHandlerBase extends MessageHandlerBase {
    protected final Timer timer;
    protected final Config config;
    protected final Broadcaster broadcaster;

    protected ChannelsMessageHandlerBase(ClientRepo repo, Config config) {
        this.config = config;

        broadcaster = new Broadcaster(repo);
        timer = new Timer();
    }

    abstract protected void onNewBroadcast(String botId, TextMessage msg) throws Exception;

    abstract protected void onNewBroadcast(String botId, ImageMessage msg, byte[] bytes) throws Exception;

    abstract protected void onNewSubscriber(NewBot newBot) throws Exception;

    abstract protected void onNewFeedback(String botId, TextMessage msg) throws Exception;

    abstract protected void onNewFeedback(String botId, ImageMessage msg) throws Exception;

    @Override
    public boolean onNewBot(NewBot newBot) {
        try {
            saveNewBot(newBot);

            onNewSubscriber(newBot);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
        return true;
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String botId = client.getId();

            if (isAdminBot(botId)) {
                onNewBroadcast(botId, msg);
            } else {
                saveMessage(botId, msg);

                likeMessage(client, msg.getMessageId());

                onNewFeedback(botId, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        try {
            String botId = client.getId();

            if (isAdminBot(botId)) {
                byte[] bytes = client.downloadAsset(msg.getAssetKey(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
                onNewBroadcast(botId, msg, bytes);
            } else {
                saveMessage(botId, msg);

                likeMessage(client, msg.getMessageId());

                onNewFeedback(botId, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);
            if (channel.admin != null && channel.admin.equals(botId)) {
                String msg = String.format("This is Admin Conversation for the Channel: `%s`." +
                                "\nYou should rename this conv to something like: `Admin %s`." +
                                "\nUse this conversation to broadcast. Don't leave or delete this conv." +
                                "\nOthers can subscribe to this channel by clicking on: wire.com/b/%s",
                        channel.name,
                        channel.name,
                        channel.name);
                client.sendText(msg);
                return;
            }

            String label = String.format("This is %s channel", channel.name);
            client.sendText(label);

            /*
            long from = new Date().getTime() - TimeUnit.MINUTES.toMillis(config.getFallback());
            for (Broadcast b : Service.dbManager.getBroadcast(from / 1000)) {
                if (b.getText() != null) {
                    client.sendText(b.getText());
                } else if (b.getUrl() != null) {
                    Picture preview = new Picture(b.getAssetData());
                    preview.setAssetKey(b.getAssetKey());
                    preview.setAssetToken(b.getToken());
                    preview.setOtrKey(b.getOtrKey());
                    preview.setSha256(b.getSha256());

                    client.sendLinkPreview(b.getUrl(), b.getTitle(), preview);
                } else if (b.getAssetData() != null) {
                    Picture picture = new Picture(b.getAssetData());
                    picture.setAssetKey(b.getAssetKey());
                    picture.setAssetToken(b.getToken());
                    picture.setOtrKey(b.getOtrKey());
                    picture.setSha256(b.getSha256());
                    picture.setExpires(TimeUnit.MINUTES.toMillis(config.getExpiration()));

                    client.sendPicture(picture);
                }
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onBotRemoved(String botId) {
        try {
            Service.dbManager.removeSubscriber(botId);
            Logger.info("Removed Subscriber: %s", botId);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberJoin(WireClient client, ArrayList<String> userIds) {
        try {
            Channel channel = getChannel(client.getId());
            broadcaster.sendOnMemberFeedback(channel.name, "**%s** joined", userIds);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        try {
            Channel channel = getChannel(client.getId());
            broadcaster.sendOnMemberFeedback(channel.name, "**%s** left", userIds);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onEvent(WireClient client, String userId, Messages.GenericMessage msg) {
        String botId = client.getId();
        if (msg.hasReaction()) {
            Logger.info(String.format("onEvent (Like) bot: %s, user: %s", botId, userId));
        }

        if (msg.hasDeleted()) {
            Messages.MessageDelete deleted = msg.getDeleted();
            try {
                Channel channel = getChannel(botId);
                if (botId.equals(channel.admin)) {
                    broadcaster.revokeBroadcast(channel.name, deleted.getMessageId());
                }
            } catch (SQLException e) {
                Logger.error("Error revoking broadcast. " + e.getLocalizedMessage());
            }
        }
    }

    private void likeMessage(final WireClient client, final String messageId) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    client.sendReaction(messageId, "❤️");
                } catch (Exception e) {
                    Logger.error(e.getLocalizedMessage());
                }
            }
        }, TimeUnit.SECONDS.toMillis(5));
    }

    private void saveMessage(String botId, ImageMessage msg) throws SQLException {
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
            Service.dbManager.insertMessage(m);
        } catch (SQLException e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void saveMessage(String botId, TextMessage msg) {
        try {
            Message m = new Message();
            m.setBotId(botId);
            m.setUserId(msg.getUserId());
            m.setText(msg.getText());
            m.setMimeType("text");
            Service.dbManager.insertMessage(m);
        } catch (SQLException e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void saveNewBot(NewBot newBot) {
        try {
            Service.dbManager.insertUser(newBot);

        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error(e.getLocalizedMessage());
        }
    }

    private boolean isAdminBot(String botId) {
        try {
            Channel channel = getChannel(botId);
            return botId.equals(channel.admin);
        } catch (SQLException e) {
            Logger.error(e.getMessage());
            return false;
        }
    }

    protected Channel getChannel(String botId) throws SQLException {
        String channelName = Service.dbManager.getChannelName(botId);
        return Service.dbManager.getChannel(channelName);
    }
}
