package com.alexhilman.cameradashboard.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 */
public class Fixtures {
    private static final File SOURCE_MOVIES = new File(System.getProperty("user.home") + "/.camera-dashboard/rotating/DCS-936L");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static File randomFile() {
        return emptyFile();
    }

    public static File emptyFile() {
        try {
            final File file = new File(tmpFolder(), UUID.randomUUID().toString() + ".mp4");

            if (!file.createNewFile()) {
                throw new RuntimeException("Could not create new file: " + file.getAbsolutePath());
            }

            final BasicFileAttributeView fileAttributeView =
                    Files.getFileAttributeView(file.toPath(),BasicFileAttributeView.class);
            final FileTime fileTime = FileTime.from(randomInstantInThePastYear());
            fileAttributeView.setTimes(fileTime, fileTime, fileTime);
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File tmpFolder() {
        try {
            final File tmpFolder =
                    new File(File.createTempFile(UUID.randomUUID().toString(), "").getParentFile(),
                             "camera-dashboard");
            tmpFolder.mkdirs();
            return tmpFolder;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Instant randomInstantInThePastYear() {
        return Instant.now().minus(SECURE_RANDOM.nextInt(secondsInYear()), ChronoUnit.SECONDS);
    }

    private static int secondsInYear() {
        return 365 * 24 * 60 * 60;
    }
}
