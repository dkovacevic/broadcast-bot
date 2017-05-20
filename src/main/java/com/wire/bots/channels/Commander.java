package com.wire.bots.channels;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Message;
import com.wire.bots.sdk.WireClient;

public class Commander {
    public static boolean processAdminCmd(String channelName, String cmd, WireClient adminClient) throws Exception {
        boolean ret = false;
        if (cmd.startsWith("/"))
            ret = true;

        if (cmd.equalsIgnoreCase("/help")) {
            String h = "List of available commands:\n" +
                    "`/intro <text>` Set **Intro Text** for new Subscribers\n" +
                    "`/intro <url>`  Set **Intro Picture** for new Subscribers\n" +
                    "`/mute`         **Mute** all incoming messages from Subscribers\n" +
                    "`/unmute`       **Unmute** all incoming messages from Subscribers\n" +
                    "`/allow @<username>` **White list** user with this @username\n" +
                    "`/block @<username>` **Black list** user with this @username\n" +
                    "`/public`       Clear White and Black lists. Anybody can join\n" +
                    "`/curl`         Show `curl` command for broadcasting\n" +
                    "`/stats`        Show some **statistics**: #posts, #subscribers, #feedbacks ...";

            adminClient.sendText(h);
            return ret;
        }
        if (cmd.startsWith("/intro")) {
            String intro = cmd.replace("/intro", "").trim();
            if (intro.startsWith("http")) {
                Service.storage.updateChannel(channelName, "intro", intro);
                adminClient.sendText("Updated `intro picture`");
            } else {
                Service.storage.updateChannel(channelName, "welcome", intro);
                adminClient.sendText("Updated `intro text`");
            }
            return ret;
        }
        if (cmd.equalsIgnoreCase("/mute")) {
            Service.storage.updateChannel(channelName, "muted", 1);
            adminClient.sendText("You won't receive info about subscribers' activity anymore. Type `/unmute` to resume");
            return ret;
        }
        if (cmd.equalsIgnoreCase("/unmute")) {
            Service.storage.updateChannel(channelName, "muted", 0);
            adminClient.sendText("Resumed. Type `/mute` to mute");
            return ret;
        }
        if (cmd.startsWith("/allow")) {
            String handle = cmd.replace("/allow", "").trim().toLowerCase();
            if (handle.startsWith("@")) {
                Service.storage.insertWhitelist(channelName, handle.replace("@", ""), Storage.State.WHITE);
                adminClient.sendText(handle + " added to White List");
            }
            return ret;
        }
        if (cmd.startsWith("/block")) {
            String handle = cmd.replace("/block", "").trim().toLowerCase();
            if (handle.startsWith("@")) {
                Service.storage.insertWhitelist(channelName, handle.replace("@", ""), Storage.State.BLACK);
                adminClient.sendText(handle + " added to Black List");
            }
            return ret;
        }
        if (cmd.equals("/public")) {
            Service.storage.clearWhitelist(channelName);
            adminClient.sendText("Channel made **public** again");
            return ret;
        }
        if (cmd.equalsIgnoreCase("/curl")) {
            Channel channel = Service.storage.getChannel(channelName);
            Message message = new Message();
            message.setText("Hi there!");
            String obj = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(message);

            String msg = String.format("```\ncurl -ikXPOST https://%s/channels/%s/broadcast -d'%s' " +
                            "-H'Authorization:%s' " +
                            "-H'Content-Type:application/json'\n```",
                    Service.CONFIG.getHost(),
                    channelName,
                    obj,
                    channel.token);
            adminClient.sendText(msg);
            return ret;
        }
        if (cmd.equalsIgnoreCase("/stats")) {
            int subscribers = Service.storage.getSubscribers(channelName).size();
            int posts = Service.storage.getBroadcasts(channelName, Integer.MAX_VALUE, Integer.MAX_VALUE).size();
            int messages = Service.storage.getMessageCount(channelName);

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
}
