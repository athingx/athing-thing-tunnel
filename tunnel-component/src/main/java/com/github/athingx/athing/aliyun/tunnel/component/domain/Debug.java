package com.github.athingx.athing.aliyun.tunnel.component.domain;

import com.google.gson.annotations.SerializedName;

public class Debug {

    @SerializedName("status")
    private int status;

    public boolean isEnable() {
        return status == 1;
    }

}