package io.github.athingx.athing.thing.tunnel.aliyun;

import io.github.athingx.athing.standard.thing.boot.ThingBoot;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ThingTunnelComTestCase extends ThingSupport {

    @Ignore
    @Test
    public void debug$thing$tunnel() throws InterruptedException {
        synchronized (this) {
            this.wait();
        }
    }

    @Test
    public void test$thing$tunnel$boot() {
        final ThingBoot boot = new ThingTunnelBoot();
        Assert.assertEquals("${project.groupId}", boot.getProperties().getProperty(ThingBoot.PROP_GROUP));
        Assert.assertEquals("${project.artifactId}", boot.getProperties().getProperty(ThingBoot.PROP_ARTIFACT));
        Assert.assertEquals("${project.version}", boot.getProperties().getProperty(ThingBoot.PROP_VERSION));
        Assert.assertEquals("aliyun", boot.getProperties().getProperty(ThingBoot.PROP_PLATFORM_LIMIT));
    }

}
