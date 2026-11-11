package de.lardtom.justmoreparts.dev.client;

import de.lardtom.justmoreparts.command.client.CovCodeChatSelfTests;
import de.lardtom.justmoreparts.dev.SelfTest;
import de.lardtom.justmoreparts.frame.client.FrameSelfTests;
import de.lardtom.justmoreparts.motor.client.CovWhoopMotorSelfTests;
import de.lardtom.justmoreparts.motor.client.MotorSelfTests;
import de.lardtom.justmoreparts.power.client.PowerPlugSelfTests;
import de.lardtom.justmoreparts.power.client.PowerSelfTests;
import de.lardtom.justmoreparts.prop.client.PropSelfTests;

public final class DevClientBootstrap {
    private DevClientBootstrap() {
    }

    public static void init() {
        if (SelfTest.enabled()) {
            CoreSelfTests.register();
            SchoolSelfTests.register();
            MotorSelfTests.register();
            PropSelfTests.register();
            PowerSelfTests.register();
            FrameSelfTests.register();
            StyleSelfTests.register();
            FrameSelfTests.registerPresets();
            ShowcaseShots.register();
            PowerPlugSelfTests.register();
            CovWhoopMotorSelfTests.register();
            CovCodeChatSelfTests.register();
            SelfTestRunner.install();
        } else if (SelfTest.smokeEnabled()) {
            SelfTestRunner.installSmoke();
        }
    }
}
