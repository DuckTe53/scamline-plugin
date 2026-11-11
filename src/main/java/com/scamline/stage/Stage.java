package com.scamline.stage;

import java.util.UUID;

public interface Stage {

    String name();

    void start();

    void end();

    default void onPlayerLeave(UUID id) {
    }
}
