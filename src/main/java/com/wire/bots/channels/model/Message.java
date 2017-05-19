package com.wire.bots.channels.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Message {
    @JsonProperty
    String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
