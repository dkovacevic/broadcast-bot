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
import com.wire.bots.sdk.models.AudioMessage;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import com.wire.bots.sdk.models.VideoMessage;
import com.wire.bots.sdk.server.model.User;
import com.wire.bots.sdk.tools.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class Broadcaster {
    private final ClientRepo repo;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(Service.CONFIG.threads);

    Broadcaster(ClientRepo repo) {
        this.repo = repo;
        warmup();
    }

    private void broadcastLocally(Channel channel, final TextMessage msg) throws Exception {
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(Service.CONFIG.threads);
        final AtomicInteger success = new AtomicInteger(0);

        ArrayList<WireClient> subscribers = getSubscribers(channel);
        Date s = new Date();
        for (final WireClient client : subscribers) {
            executor.execute(() -> {
                try {
                    client.sendText(msg.getText());
                    success.incrementAndGet();
                } catch (Exception e) {
                    Logger.warning("Bot: %s. Error: %s", client.getId(), e.getMessage());
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Date e = new Date();

        float elapse = (e.getTime() - s.getTime()) / 1000f;
        float avg = subscribers.size() / elapse;
        String log = String.format("Delivered to %d subscribers, in: %.2f sec, avg: %.2f msg/sec",
                success.get(),
                elapse,
                avg);
        Logger.info(log);
        //sendToAdminConv(channel.admin, log);
    }

    private void broadcastForward(Channel channel, final TextMessage msg) throws Exception {
        ArrayList<String> ids = getSubscriberIds(channel);
        ids.remove(channel.admin);

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(Service.CONFIG.threads);

        Date s = new Date();
        final AtomicInteger success = new AtomicInteger(0);
        for (Collection<String> slice : slice(ids, Service.CONFIG.batch)) {
            executor.execute(() -> {
                int status = ForwardClient.forward(slice, msg);
                if (status == 200)
                    success.addAndGet(slice.size());
                else
                    Logger.warning("Failed to forward slice %d", slice.size());
            });
        }
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.MINUTES);
        Date e = new Date();

        float elapse = (e.getTime() - s.getTime()) / 1000f;
        float avg = ids.size() / elapse;
        String log = String.format("Delivered to %d subscribers, in: %.2f sec, avg: %.2f msg/sec",
                success.get(),
                elapse,
                avg);
        Logger.info(log);
        //sendToAdminConv(channel.admin, log);
    }

    void broadcast(Channel channel, TextMessage msg) throws Exception {
        if (msg.getText().startsWith("http")) {
            broadcastUrl(channel, msg);
        } else {
            broadcastLocally(channel, msg);
        }
    }

    private void warmup() {
        for (Channel channel : Service.CONFIG.channels.values()) {
            try {
                ArrayList<WireClient> wireClients = getSubscribers(channel);
                for (final WireClient client : wireClients) {
                    executor.execute(() -> {
                        try {
                            client.sendReaction("", "");
                        } catch (Exception e) {
                            Logger.error("warmup: bot: %s, %s", client.getId(), e);
                        }
                    });
                }
            } catch (Exception e) {
                Logger.warning("warmup %s", e);
            }
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
        if (adminClient != null) {
            String userName = getUserName(adminClient, msg.getUserId());
            adminClient.sendText(String.format("**@%s** wrote: _%s_", userName, msg.getText()));
        }
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
        if (adminClient != null) {

            String userName = getUserName(adminClient, msg.getUserId());

            adminClient.sendText(String.format("**@%s** has sent:", userName));
            adminClient.sendAudio(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
        }
    }

    void sendToAdminConv(String adminBot, VideoMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        if (adminClient != null) {

            String userName = getUserName(adminClient, msg.getUserId());

            adminClient.sendText(String.format("**@%s** has sent:", userName));
            adminClient.sendVideo(msg.getData(),
                    msg.getName(),
                    msg.getMimeType(),
                    msg.getDuration(),
                    msg.getHeight(),
                    msg.getWidth());
        }
    }

    void sendToAdminConv(String adminBot, String text) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        if (adminClient != null) {
            adminClient.sendText(text);
        }
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
        for (String botId : getSubscriberIds(channel)) {
            if (botId.equals(channel.admin))
                continue;

            WireClient wireClient = repo.getWireClient(botId);
            if (wireClient != null)
                ret.add(wireClient);
        }
        return ret;
    }

    private ArrayList<String> getSubscriberIds(Channel channel) throws Exception {
        Database database = new Database(Service.CONFIG.getPostgres());
        return database.getSubscribers(channel.id);
    }

    private String getUserName(WireClient client, String userId) throws IOException {
        Collection<User> users = client.getUsers(Collections.singletonList(userId));
        return users.iterator().next().handle;
    }

    private Collection<Collection<String>> slice(List<String> ids, int batch) {
        Collection<Collection<String>> ret = new ArrayList<>();
        for (int i = 0; i < ids.size(); i += batch) {
            int toIndex = i + batch;
            if (toIndex > ids.size()) {
                toIndex = ids.size();
            }
            List<String> slice = ids.subList(i, toIndex);
            ret.add(slice);
        }
        return ret;
    }
}
