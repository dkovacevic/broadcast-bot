package com.wire.bots.channels.model;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;

public class Channel {
    public String name;
    @NotNull
    public String token;
    public String admin;
    public String introText;
    public String introPic;
    public ArrayList<String> whitelist;
}
