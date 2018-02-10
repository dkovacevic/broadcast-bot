package com.wire.bots.channels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public class BatchForward {
    @JsonProperty
    public String payload;
    @JsonProperty
    public Collection<String> bots;
}
