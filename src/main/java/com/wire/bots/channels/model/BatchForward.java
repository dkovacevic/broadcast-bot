package com.wire.bots.channels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class BatchForward {
    @JsonProperty
    public String payload;
    @JsonProperty
    public ArrayList<String> bots;
}
