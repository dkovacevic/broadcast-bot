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

package com.wire.bots.channels.model;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

public class Config extends com.wire.bots.sdk.Configuration {
    @NotNull
    private String database;
    private ArrayList<String> whitelist;
    private String onNewSubscriberLabel;
    private long fallback;
    private boolean like = true;
    private long expiration;
    private String appSecret;

    public String getDatabase() {
        return database;
    }

    public ArrayList<String> getWhitelist() {
        return whitelist;
    }

    public String getOnNewSubscriberLabel() {
        return onNewSubscriberLabel;
    }

    public long getFallback() {
        return fallback;
    }

    public boolean isLike() {
        return like;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public String getAppSecret() {
        return appSecret;
    }

}
