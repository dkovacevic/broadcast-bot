package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.tools.Logger;

public class NewBotHandler {
    private final Config config;
    private final Broadcaster broadcaster;

    NewBotHandler(Config config, Broadcaster broadcaster) {
        this.config = config;
        this.broadcaster = broadcaster;
    }

    public boolean onNewBot(String channelId, NewBot newBot) {
        try {
            Database database = new Database(config.getPostgres());
            Channel channel = database.getChannel(channelId);

            if (channel == null) {
                Logger.error("Unknown Channel `%s`, bot: %s", channelId, newBot.id);
                return false;
            }

            if (checkWhitelist(newBot.origin.handle, channel.whitelist))
                return false;

            for (Member member : newBot.conversation.members) {
                if (member.service != null) {
                    Logger.warning("Rejecting NewBot. Provider: %s service: %s",
                            member.service.provider,
                            member.service.id);
                    return false; // we don't want to be in a conv if other bots are there.
                }
            }

            if (channel.admin == null) {
                Logger.warning("Channel `%s` not yet activated. Admin: %s", channel.id, newBot.id);
                return true;
            }

            Logger.info("New Subscriber for Channel: %s. Bot: %s", channel.id, newBot.id);
            if (broadcaster != null) {
                //broadcaster.sendToAdminConv(channel.admin, String.format("**@%s** has joined", newBot.origin.handle));
            }
        } catch (Exception e) {
            Logger.error(e.getMessage());
            e.printStackTrace();
            return true;
        }
        return true;
    }

    private boolean checkWhitelist(String handle, String whitelist) {
        if (whitelist != null && !whitelist.isEmpty()) {
            if (!isWhitelisted(handle, whitelist))
                return true;
        }
        return false;
    }

    private boolean isWhitelisted(String handle, String whitelist) {
        for (String white : whitelist.split(",")) {
            if (white.equalsIgnoreCase(handle)) {
                return true;
            }
        }
        return false;
    }
}
