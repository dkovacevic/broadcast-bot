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

import com.wire.bots.channels.clients.ForwardClient;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.models.AudioMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.models.VideoMessage;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.storage.Storage;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

class Broadcaster {
    private final ClientRepo repo;
    private final StorageFactory storageFactory;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(20);

    Broadcaster(ClientRepo repo, StorageFactory storageFactory) {
        this.repo = repo;
        this.storageFactory = storageFactory;
    }

    private void broadcastText(Channel channel, final TextMessage msg) throws Exception {
        int success = 0;
        int failed = 0;
        ArrayList<String> ids = getSubscriberIds(channel);

        Date s = new Date();
        for (String botId : ids) {
            int status = ForwardClient.forward(botId, msg);
            success += status == 200 ? 1 : 0;
            failed += status != 200 ? 1 : 0;
        }
        Date e = new Date();

        float elapse = (e.getTime() - s.getTime()) / 1000f;
        float avg = ids.size() / elapse;
        sendToAdminConv(channel.admin, String.format("Delivered: %d, failed: %d in: %.2f sec, avg: %.2f msg/sec",
                success,
                failed,
                elapse,
                avg));
    }

    private void broadcastText2(Channel channel, final TextMessage msg) throws Exception {
        ArrayList<String> ids = getSubscriberIds(channel);

        Date s = new Date();
        int status = ForwardClient.forward(ids, msg);
        Date e = new Date();

        float elapse = (e.getTime() - s.getTime()) / 1000f;
        float avg = ids.size() / elapse;
        sendToAdminConv(channel.admin, String.format("Delivered: %d, in: %.2f sec, avg: %.2f msg/sec",
                ids.size(),
                elapse,
                avg));
    }

    void broadcast(Channel channel, TextMessage msg) throws Exception {
        if (msg.getText().startsWith("http")) {
            broadcastUrl(channel, msg);
        } else {
            broadcastText2(channel, msg);
        }
    }

    void broadcast(Channel channel, ImageMessage msg) throws Exception {
        final Picture picture = new Picture(msg.getData(), msg.getMimeType());
        picture.setSize((int) msg.getSize());
        picture.setWidth(msg.getWidth());
        picture.setHeight(msg.getHeight());
        picture.setAssetKey(msg.getAssetKey());
        picture.setAssetToken(msg.getAssetToken());
        picture.setOtrKey(msg.getOtrKey());
        picture.setSha256(msg.getSha256());
        picture.setMessageId(msg.getMessageId());

        for (final WireClient client : getSubscribers(channel)) {
            executor.execute(() -> {
                try {
                    client.sendPicture(picture);
                } catch (Exception e) {
                    Logger.warning("Bot: %s. Error: %s", client.getId(), e.getMessage());
                }
            });
        }
    }

    void broadcast(Channel channel, final AudioMessage msg) throws Exception {
        for (final WireClient client : getSubscribers(channel)) {
            executor.execute(() -> {
                try {
                    client.sendAudio(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
                } catch (Exception e) {
                    Logger.warning("Bot: %s. Error: %s", client.getId(), e.getMessage());
                }
            });
        }
    }

    void broadcast(Channel channel, final VideoMessage msg) throws Exception {
        for (final WireClient client : getSubscribers(channel)) {
            executor.execute(() -> {
                try {
                    client.sendVideo(msg.getData(),
                            msg.getName(),
                            msg.getMimeType(),
                            msg.getDuration(),
                            msg.getHeight(),
                            msg.getWidth());
                } catch (Exception e) {
                    Logger.warning("Bot: %s. Error: %s", client.getId(), e.getMessage());
                }
            });
        }
    }

    void revokeBroadcast(Channel channel, final String messageId) throws Exception {
        for (final WireClient client : getSubscribers(channel)) {
            executor.execute(() -> {
                try {
                    client.deleteMessage(messageId);
                } catch (Exception e) {
                    Logger.error("Bot: %s. Error: %s", client.getId(), e.getMessage());
                }
            });
        }
    }


    void sendToAdminConv(String adminBot, TextMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        String userName = getUserName(adminClient, msg.getUserId());

        adminClient.sendText(String.format("**@%s** wrote: _%s_", userName, msg.getText()));
    }

    void sendToAdminConv(String adminBot, ImageMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        String userName = getUserName(adminClient, msg.getUserId());

        Picture picture = new Picture();
        picture.setMimeType(msg.getMimeType());
        picture.setSize((int) msg.getSize());
        picture.setWidth(msg.getWidth());
        picture.setHeight(msg.getHeight());
        picture.setAssetKey(msg.getAssetKey());
        picture.setAssetToken(msg.getAssetToken());
        picture.setOtrKey(msg.getOtrKey());
        picture.setSha256(msg.getSha256());

        adminClient.sendText(String.format("**@%s** has sent:", userName));
        adminClient.sendPicture(picture);
    }

    void sendToAdminConv(String adminBot, AudioMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        String userName = getUserName(adminClient, msg.getUserId());

        adminClient.sendText(String.format("**@%s** has sent:", userName));
        adminClient.sendAudio(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
    }

    void sendToAdminConv(String adminBot, VideoMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        String userName = getUserName(adminClient, msg.getUserId());

        adminClient.sendText(String.format("**@%s** has sent:", userName));
        adminClient.sendVideo(msg.getData(),
                msg.getName(),
                msg.getMimeType(),
                msg.getDuration(),
                msg.getHeight(),
                msg.getWidth());
    }

    void sendToAdminConv(String adminBot, String text) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        adminClient.sendText(text);
    }

    private void broadcastUrl(Channel channel, final TextMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(channel.admin);

        final String url = msg.getText();
        final String title = UrlUtil.extractPageTitle(url);
        final Picture preview = Cache.getPicture(adminClient, UrlUtil.extractPagePreview(url));

        for (final WireClient client : getSubscribers(channel)) {
            executor.execute(() -> {
                try {
                    client.sendLinkPreview(url, title, preview);
                } catch (Exception e) {
                    Logger.error("Bot: %s. Error: %s", client.getId(), e.getMessage());
                }
            });
        }
    }

    private ArrayList<WireClient> getSubscribers(Channel channel) throws Exception {
        ArrayList<WireClient> ret = new ArrayList<>();
        for (WireClient client : repo.listClients()) {
            String botId = client.getId();
            Storage storage = storageFactory.create(botId);
            String id = storage.readFile(".channel");
            if (channel.id.equalsIgnoreCase(id))
                ret.add(client);
        }
        return ret;
    }

    private ArrayList<String> getSubscriberIds(Channel channel) throws Exception {
        ArrayList<String> ret = new ArrayList<>();
        for (NewBot bot : storageFactory.create("").listAllStates()) {
            if (bot.id.equals(channel.admin))
                continue;

            Storage storage = storageFactory.create(bot.id);
            String id = storage.readFile(".channel");
            if (channel.id.equalsIgnoreCase(id))
                ret.add(bot.id);
        }
        return ret;
    }

    private String getUserName(WireClient client, String userId) throws IOException {
        Collection<User> users = client.getUsers(Collections.singletonList(userId));
        return users.iterator().next().handle;
    }
}
