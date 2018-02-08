package com.wire.bots.channels;

import com.wire.bots.channels.model.Channel;
import com.wire.bots.channels.model.Config;
import com.wire.bots.sdk.server.model.Member;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;

public class NewBotHandler {
    private final Config config;

    NewBotHandler(Config config) {
        this.config = config;
    }

    public boolean onNewBot(String channelName, NewBot newBot) {
        try {
            Channel channel = config.getChannels().get(channelName);

            ArrayList<String> whitelist = channel.whitelist;
            if (!whitelist.isEmpty() && !whitelist.contains(newBot.origin.handle)) {
                Logger.warning("Rejecting NewBot. Not Whitelisted");
                return false;
            }

            for (Member member : newBot.conversation.members) {
                if (member.service != null) {
                    Logger.warning("Rejecting NewBot. Provider: %s service: %s",
                            member.service.provider,
                            member.service.id);
                    return false; // we don't want to be in a conv if other bots are there.
                }
            }

            if (channel.admin == null) {
                Logger.warning("Channel `%s` not yet activated. Admin: %s", channel.name, newBot.id);
                return true;
            }

            Logger.info("New Subscriber for Channel: %s. Bot: %s", channel.name, newBot.id);
        } catch (Exception e) {
            Logger.error(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public Channel getChannel(String name) {
        return config.getChannels().get(name);
    }
}
