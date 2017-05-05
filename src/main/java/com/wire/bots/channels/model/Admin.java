package com.wire.bots.channels.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Admin {
    @JsonProperty
    public String admin;
    @JsonProperty
    public String token;
    @JsonProperty
    public String origin;

    public String getAdmin() {
        return admin;
    }

    public String getToken() {
        return token;
    }

    public String getOrigin() {
        return origin;
    }
}
