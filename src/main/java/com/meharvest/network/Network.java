package com.meharvest.network;

import com.meharvest.MEHarvest;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(MEHarvest.id("main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private Network() {
    }

    public static void init() {
        CHANNEL.registerMessage(0, C2SConfig.class,
                C2SConfig::encode, C2SConfig::decode, C2SConfig::handle);
        CHANNEL.registerMessage(1, C2SToggle.class,
                C2SToggle::encode, C2SToggle::decode, C2SToggle::handle);
    }
}
