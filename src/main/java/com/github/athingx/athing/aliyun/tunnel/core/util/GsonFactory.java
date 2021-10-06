package com.github.athingx.athing.aliyun.tunnel.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonFactory {

    private static final Gson gson = new GsonBuilder()
            .create();

    public static Gson getGson() {
        return gson;
    }

}
