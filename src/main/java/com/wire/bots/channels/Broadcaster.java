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

import com.wire.bots.channels.model.Broadcast;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Subscriber;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.Logger;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.assets.Picture;
import com.wire.bots.sdk.models.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.UUID;

public class Broadcaster {
    private final ClientRepo repo;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(20);

    public Broadcaster(ClientRepo repo) {
        this.repo = repo;
    }

    public void broadcast(String channelName, final String text) throws Exception {
        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        if (client != null)
                            client.sendText(text);
                    } catch (Exception e) {
                        Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }
        Broadcast b = new Broadcast();
        b.setMessageId(UUID.randomUUID().toString());
        b.setChannel(channelName);
        b.setText(text);
        Service.storage.insertBroadcast(b);
    }

    public void broadcast(Channel channel, TextMessage msg) throws Exception {
        if (msg.getText().startsWith("http")) {
            broadcastUrl(channel, msg);
        } else {
            broadcastText(channel.name, msg);
        }
    }

    public void broadcast(String channelName, ImageMessage msg) throws Exception {
        final Picture picture = new Picture(msg.getData(), msg.getMimeType());
        picture.setSize((int) msg.getSize());
        picture.setWidth(msg.getWidth());
        picture.setHeight(msg.getHeight());
        picture.setAssetKey(msg.getAssetKey());
        picture.setAssetToken(msg.getAssetToken());
        picture.setOtrKey(msg.getOtrKey());
        picture.setSha256(msg.getSha256());
        picture.setMessageId(msg.getMessageId());

        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        if (client != null)
                            client.sendPicture(picture);
                    } catch (Exception e) {
                        Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }
        Service.storage.insertBroadcast(new Broadcast(channelName, msg));
    }

    public void broadcast(String channelName, final AudioMessage msg) throws Exception {
        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        if (client != null)
                            client.sendAudio(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
                    } catch (Exception e) {
                        Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }
        Service.storage.insertBroadcast(new Broadcast(channelName, msg));
    }

    public void broadcast(String channelName, final VideoMessage msg) throws Exception {
        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        if (client != null)
                            client.sendVideo(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
                    } catch (Exception e) {
                        Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }
        Service.storage.insertBroadcast(new Broadcast(channelName, msg));
    }

    public void broadcast(String name, AttachmentMessage msg) {

    }

    public void followBack(final WireClient client, int limit) throws SQLException {
        final String botId = client.getId();
        String channelName = Service.storage.getChannelName(botId);
        int last = Service.storage.getLast(botId);
        final ArrayList<Broadcast> broadcasts = Service.storage.getBroadcasts(channelName, last, limit);
        Collections.sort(broadcasts, new Comparator<Broadcast>() {
            @Override
            public int compare(Broadcast b1, Broadcast b2) {
                if (b1.getId() < b2.getId()) return -1;
                if (b1.getId() > b2.getId()) return 1;
                return 0;
            }
        });

        if (!broadcasts.isEmpty()) {
            Service.storage.updateBot(botId, "Last", broadcasts.get(0).getId());
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    for (final Broadcast b : broadcasts) {
                        if (b.getUrl() != null) {
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
                            client.sendPicture(picture);
                        } else if (b.getText() != null) {
                            client.sendText(b.getText());
                        }

                        Thread.sleep(1500);
                    }
                } catch (Exception e) {
                    Logger.warning(e.getLocalizedMessage());
                }
            }
        });
    }

    public void revokeBroadcast(String channelName, final String messageId) throws SQLException {
        Service.storage.deleteBroadcast(messageId);

        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        client.deleteMessage(messageId);
                    } catch (Exception e) {
                        Logger.error("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }
    }

    public void sendToAdminConv(String adminBot, ImageMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);

        Picture picture = new Picture();
        picture.setMimeType(msg.getMimeType());
        picture.setSize((int) msg.getSize());
        picture.setWidth(msg.getWidth());
        picture.setHeight(msg.getHeight());
        picture.setAssetKey(msg.getAssetKey());
        picture.setAssetToken(msg.getAssetToken());
        picture.setOtrKey(msg.getOtrKey());
        picture.setSha256(msg.getSha256());

        adminClient.sendText("**Follower has sent:**");
        adminClient.sendPicture(picture);
    }

    public void sendToAdminConv(String adminBot, AudioMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);

        adminClient.sendText("**Follower has sent:**");
        adminClient.sendAudio(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
    }

    public void sendToAdminConv(String adminBot, VideoMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);

        adminClient.sendText("**Follower has sent:**");
        adminClient.sendVideo(msg.getData(), msg.getName(), msg.getMimeType(), msg.getDuration());
    }

    public void sendToAdminConv(String adminBot, AttachmentMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);

        adminClient.sendText("**Follower has sent:**");
        adminClient.sendVideo(msg.getData(), msg.getName(), msg.getMimeType(), 0);
    }

    public void sendToAdminConv(String adminBot, String text) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        adminClient.sendText(text);
    }

    private Picture uploadPreview(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }

    private void broadcastText(String channelName, final TextMessage msg) throws SQLException {
        final String messageId = msg.getMessageId();
        final String text = msg.getText();

        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        if (client != null)
                            client.sendText(text, 0, messageId);
                    } catch (Exception e) {
                        Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }
        Broadcast b = new Broadcast(channelName, msg);
        Service.storage.insertBroadcast(b);
    }

    private void broadcastUrl(Channel channel, final TextMessage msg) throws Exception {
        WireClient adminClient = repo.getWireClient(channel.admin);

        final String url = msg.getText();
        final String title = UrlUtil.extractPageTitle(url);
        final Picture preview = uploadPreview(adminClient, UrlUtil.extractPagePreview(url));

        for (final String botId : getSubscribers(channel.name)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        WireClient client = repo.getWireClient(botId);
                        if (client != null)
                            client.sendLinkPreview(url, title, preview);
                    } catch (Exception e) {
                        Logger.error("Bot: %s. Error: %s", botId, e.getMessage());
                    }
                }
            });
        }

        Broadcast broadcast = new Broadcast(channel.name, msg);
        broadcast.setTitle(title);
        broadcast.setUrl(url);
        Service.storage.insertBroadcast(broadcast);
    }

    private ArrayList<String> getSubscribers(String channelName) {
        ArrayList<String> ret = new ArrayList<>();
        try {
            Channel channel = Service.storage.getChannel(channelName);
            ArrayList<String> whitelist = Service.storage.getWhitelist(channelName, Storage.State.WHITE);
            //Logger.info("%s White list: %s", channelName, whitelist);

            ArrayList<String> blacklist = Service.storage.getWhitelist(channelName, Storage.State.BLACK);
            //Logger.info("%s Black list: %s", channelName, blacklist);

            for (Subscriber subscriber : Service.storage.getSubscribers(channelName)) {
                if (subscriber.botId.equals(channel.admin))
                    continue;

                if (!whitelist.isEmpty() && whitelist.contains(subscriber.handle)) {
                    ret.add(subscriber.botId);
                }

                if (whitelist.isEmpty() && !blacklist.contains(subscriber.handle)) {
                    ret.add(subscriber.botId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.error(e.getMessage());
        }
        return ret;
    }
}
