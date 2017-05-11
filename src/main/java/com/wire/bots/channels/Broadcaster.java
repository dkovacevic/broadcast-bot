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
import com.wire.bots.sdk.models.AssetKey;
import com.wire.bots.sdk.models.ImageMessage;
import com.wire.bots.sdk.models.TextMessage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class Broadcaster {
    private final ClientRepo repo;
    private final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(20);

    public Broadcaster(ClientRepo repo) {
        this.repo = repo;
    }

    public void broadcast(Channel channel, TextMessage msg) throws Exception {
        String text = msg.getText();
        String messageId = msg.getMessageId();

        if (text.startsWith("http")) {
            broadcastUrl(channel, text);
        } else {
            broadcastText(channel.name, messageId, text);
        }
    }

    public void broadcast(String channelName, ImageMessage msg, byte[] imgData) throws Exception {
        Picture picture = new Picture(imgData, msg.getMimeType());
        picture.setSize((int) msg.getSize());
        picture.setWidth(msg.getWidth());
        picture.setHeight(msg.getHeight());
        picture.setAssetKey(msg.getAssetKey());
        picture.setAssetToken(msg.getAssetToken());
        picture.setOtrKey(msg.getOtrKey());
        picture.setSha256(msg.getSha256());
        picture.setMessageId(msg.getMessageId());

        broadcastPicture(channelName, picture);
    }

    public void broadcastUrl(Channel channel, final String url) throws Exception {
        WireClient adminClient = repo.getWireClient(channel.admin);

        final String title = extractPageTitle(url);
        final Picture preview = uploadPreview(adminClient, extractPagePreview(url));

        saveBroadcast(url, title, preview, channel.name);

        for (final String botId : getSubscribers(channel.name)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendLink(url, title, preview, botId);
                }
            });
        }
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
                            client.sendPicture(picture);
                        }
                        Thread.sleep(1500);
                    }
                } catch (Exception e) {
                    Logger.warning(e.getLocalizedMessage());
                }
            }
        });
    }

    private void broadcastText(String channelName, final String messageId, final String text) throws SQLException {
        Broadcast b = new Broadcast();
        b.setMessageId(messageId);
        b.setText(text);
        b.setChannel(channelName);

        Service.storage.insertBroadcast(b);

        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendText(messageId, text, botId);
                }
            });
        }
    }

    private void broadcastPicture(String channelName, final Picture picture) throws SQLException {
        saveBroadcast(null, null, picture, channelName);

        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendPicture(picture, botId);
                }
            });
        }
    }

    public void revokeBroadcast(String channelName, final String messageId) throws SQLException {
        Service.storage.deleteBroadcast(messageId);

        for (final String botId : getSubscribers(channelName)) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    deleteMessage(messageId, botId);
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

        adminClient.sendText("User sent:");
        adminClient.sendPicture(picture);
    }

    public void sendToAdminConv(String adminBot, String text) throws Exception {
        WireClient adminClient = repo.getWireClient(adminBot);
        adminClient.sendText(text);
    }

    private void saveBroadcast(String url, String title, Picture picture, String channel) {
        try {
            // save to db
            Broadcast broadcast = new Broadcast();
            broadcast.setAssetData(picture.getImageData());
            broadcast.setAssetKey(picture.getAssetKey());
            broadcast.setToken(picture.getAssetToken());
            broadcast.setOtrKey(picture.getOtrKey());
            broadcast.setSha256(picture.getSha256());
            broadcast.setSize(picture.getSize());
            broadcast.setMimeType(picture.getMimeType());
            broadcast.setUrl(url);
            broadcast.setTitle(title);
            broadcast.setMessageId(picture.getMessageId());
            broadcast.setChannel(channel);

            Service.storage.insertBroadcast(broadcast);
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
        }
    }

    private void sendLink(String url, String title, Picture img, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            if (client != null)
                client.sendLinkPreview(url, title, img);
        } catch (Exception e) {
            Logger.error("Bot: %s. Error: %s", botId, e.getMessage());
        }
    }

    private void sendText(String messageId, String text, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            if (client != null)
                client.sendText(text, 0, messageId);
        } catch (Exception e) {
            Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
        }
    }

    private void sendPicture(Picture picture, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            if (client != null)
                client.sendPicture(picture);
        } catch (Exception e) {
            Logger.warning("Bot: %s. Error: %s", botId, e.getMessage());
        }
    }

    private Picture uploadPreview(WireClient client, String imgUrl) throws Exception {
        Picture preview = new Picture(imgUrl);
        preview.setPublic(true);

        AssetKey assetKey = client.uploadAsset(preview);
        preview.setAssetKey(assetKey.key);
        return preview;
    }

    private String extractPagePreview(String url) throws IOException {
        Connection con = Jsoup.connect(url);
        Document doc = con.get();

        Elements metaOgImage = doc.select("meta[property=og:image]");
        if (metaOgImage != null) {
            return metaOgImage.attr("content");
        }
        return null;
    }

    private String extractPageTitle(String url) throws IOException {
        Connection con = Jsoup.connect(url);
        Document doc = con.get();

        Elements title = doc.select("meta[property=og:title]");
        if (title != null) {
            return title.attr("content");
        }
        return doc.title();
    }

    private void deleteMessage(String messageId, String botId) {
        try {
            WireClient client = repo.getWireClient(botId);
            client.deleteMessage(messageId);
        } catch (Exception e) {
            Logger.error("Bot: %s. Error: %s", botId, e.getMessage());
        }
    }

    private ArrayList<String> getSubscribers(String channelName) {
        ArrayList<String> ret = new ArrayList<>();
        try {
            Channel channel = Service.storage.getChannel(channelName);
            ArrayList<String> whitelist = Service.storage.getWhitelist(channelName, Storage.State.WHITE);
            Logger.info("%s White list: %s", channelName, whitelist);

            ArrayList<String> blacklist = Service.storage.getWhitelist(channelName, Storage.State.BLACK);
            Logger.info("%s Black list: %s", channelName, blacklist);

            for (Subscriber subscriber : Service.storage.getSubscribers(channelName)) {
                if (subscriber.botId.equals(channel.admin))
                    continue;

                if (!whitelist.isEmpty()) {
                    if (whitelist.contains(subscriber.handle))
                        ret.add(subscriber.botId);
                    else
                        Logger.info("Not white listed: %s", subscriber.handle);
                }

                if (whitelist.isEmpty()) {
                    if (blacklist.contains(subscriber.handle))
                        Logger.info("Black listed: %s", subscriber.handle);
                    else
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
