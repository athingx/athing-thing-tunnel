package io.github.athingx.athing.aliyun.tunnel.impl;

import io.github.athingx.athing.standard.thing.boot.ThingBoot;
import org.junit.Assert;
import org.junit.Test;

public class TunnelThingComTestCase extends ThingSupport {

    @Test
    public void debug$thing$tunnel() throws InterruptedException {
        synchronized (this) {
            this.wait();
        }
    }

    @Test
    public void test$thing$tunnel$boot() {
        final ThingBoot boot = new TunnelThingBoot();
        Assert.assertEquals("athing", boot.getProperties().getProperty("manufacturer"));
        Assert.assertEquals("athing-aliyun-tunnel", boot.getProperties().getProperty("model"));
        Assert.assertEquals("oldmanpushcart@gmail.com", boot.getProperties().getProperty("author"));
        Assert.assertEquals("${project.version}", boot.getProperties().getProperty("version"));
    }

}
