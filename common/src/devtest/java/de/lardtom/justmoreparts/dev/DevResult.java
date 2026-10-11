package de.lardtom.justmoreparts.dev;

import de.lardtom.justmoreparts.JustMoreParts;
import de.lardtom.justmoreparts.platform.Services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class DevResult {
    private DevResult() {
    }

    public static void write(String result) {
        try {
            Files.writeString(Services.PLATFORM.gameDir().resolve(SelfTest.RESULT_FILE), result + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            JustMoreParts.LOG.error("[JMP-Selbsttest] Ergebnisdatei nicht geschrieben", e);
        }
    }
}
