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
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.AudioMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.models.VideoMessage;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MessageHandler extends MessageHandlerBase {
    private final Broadcaster broadcaster;

    MessageHandler(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void onNewConversation(WireClient client) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);
            if (channel.admin == null) {
                String msg = String.format("This is Admin (%s) for the Channel: **%s**." +
                                "\nYou should rename this conversation to something like: `Admin %s`." +
                                "\nUse this conversation to broadcast.\nDon't leave or delete it!",
                        botId,

                        channel.name,
                        channel.name);
                client.sendText(msg);
                return;
            }

            String introPic = channel.introPic;
            if (introPic != null) {
                Picture picture = Cache.getPicture(client, introPic);
                if (picture != null)
                    client.sendPicture(picture);
            }

            String label = channel.introText != null
                    ? channel.introText
                    : String.format("This is **%s** channel", channel.name);
            client.sendText(label);
        } catch (Exception e) {
            Logger.error(e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onText(WireClient client, TextMessage msg) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);

            if (botId.equals(channel.admin)) {
                Logger.info("New broadcast for Channel: %s", channel.name);
                broadcaster.broadcast(channel, msg);
            } else {
                broadcaster.sendToAdminConv(channel.admin, msg);
            }
        } catch (Exception e) {
            Logger.error(e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onImage(WireClient client, ImageMessage msg) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);
            byte[] bytes = client.downloadAsset(msg.getAssetKey(), msg.getAssetToken(), msg.getSha256(), msg.getOtrKey());
            msg.setData(bytes);

            if (botId.equals(channel.admin)) {
                Logger.info("New broadcast for Channel: %s", channel.name);
                broadcaster.broadcast(channel, msg);
            } else {
                broadcaster.sendToAdminConv(channel.admin, msg);
            }
        } catch (Exception e) {
            Logger.error(e.toString());
        }
    }

    @Override
    public void onAudio(WireClient client, AudioMessage msg) {
        Logger.info("OnAudio: %s, %dsec, %s, %s",
                msg.getName(),
                msg.getDuration() / 1000,
                msg.getAssetKey(),
                msg.getAssetToken());

        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);
            byte[] audio = client.downloadAsset(msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey());
            msg.setData(audio);

            if (botId.equals(channel.admin)) {
                broadcaster.broadcast(channel, msg);
            } else {
                broadcaster.sendToAdminConv(channel.admin, msg);
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onVideo(WireClient client, VideoMessage msg) {
        Logger.info("OnVideo: %s, H: %d W: %d, %dsec, %s, %s",
                msg.getName(),
                msg.getHeight(),
                msg.getWidth(),
                msg.getDuration() / 1000,
                msg.getAssetKey(),
                msg.getAssetToken());

        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);
            byte[] video = client.downloadAsset(msg.getAssetKey(),
                    msg.getAssetToken(),
                    msg.getSha256(),
                    msg.getOtrKey());
            msg.setData(video);

            if (botId.equals(channel.admin)) {
                broadcaster.broadcast(channel, msg);
            } else {
                broadcaster.sendToAdminConv(channel.admin, msg);
            }
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    @Override
    public void onBotRemoved(String botId) {
        try {
            Channel channel = getChannel(botId);
            //broadcaster.sendToAdminConv(channel.admin, "**Follower has left**");
            Logger.info("Subscriber has left from the Channel: %s. Bot: %s", channel.name, botId);
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onMemberLeave(WireClient client, ArrayList<String> userIds) {
        try {
            String botId = client.getId();
            Channel channel = getChannel(botId);

            for (String userId : userIds) {
                Logger.info("Subscriber left from the Channel: %s. Bot: %s", channel.name, botId);
                //String userName = getUserName(client, userId);
                //broadcaster.sendToAdminConv(channel.admin, String.format("**@%s** has left", userName));
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onEvent(WireClient client, String userId, Messages.GenericMessage msg) {
        String botId = client.getId();
        try {
            Channel channel = getChannel(botId);

            if (msg.hasReaction()) {
                String userName = getUserName(client, userId);
                broadcaster.sendToAdminConv(channel.admin, String.format("**@%s** liked your post", userName));
            }

            if (msg.hasDeleted() && botId.equals(channel.admin)) {
                broadcaster.revokeBroadcast(channel, msg.getDeleted().getMessageId());
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
        }
    }

    @Override
    public void onEditText(WireClient client, TextMessage msg) {
        onText(client, msg);
    }

    private Channel getChannel(String botId) throws Exception {
        Database database = new Database(Service.CONFIG.db);
        String channelName = database.getChannel(botId);
        return Service.CONFIG.getChannels().get(channelName);
    }

    private String getUserName(WireClient client, String userId) throws IOException {
        Collection<User> users = client.getUsers(Collections.singletonList(userId));
        return users.iterator().next().handle;
    }
}
