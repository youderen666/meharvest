package com.meharvest.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class Network {
    private static final String VERSION = "1";

    private Network() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(C2SConfig.TYPE, C2SConfig.STREAM_CODEC, C2SConfig::handle);
        registrar.playToServer(C2SToggle.TYPE, C2SToggle.STREAM_CODEC, C2SToggle::handle);
    }
}
