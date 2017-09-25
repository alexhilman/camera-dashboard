package com.alexhilman.cameradashboard.ui;

import com.alexhilman.dlink.dcs936.Dcs936Client;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.alexhilman.dlink.dcs936.model.DcsFileType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class Fixtures {
    private static final File SOURCE_MOVIES = new File(System.getProperty("user.home") + "/.camera-dashboard/rotating/DCS-936L");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static Dcs936Client mock;

    public static DcsFile randomDcsFile() {
        final Instant fileInstant =
                Instant.now()
                       .minusMillis(SECURE_RANDOM.nextInt((int) (System.currentTimeMillis() / 1000)));

        mock = mock(Dcs936Client.class);
        final DcsFile file = DcsFile.builder()
                                    .setFileName(fileInstant.atZone(ZoneId.systemDefault())
                                                            .format(Dcs936Client.FILE_DATE_FORMAT) + ".mp4")
                                    .setFileType(DcsFileType.File)
                                    .setParentPath("/123456/12/")
                                    .setCameraName("cam1")
                                    .build(mock);

        final File realMovieFile = randomRealMovieFile();
        try {
            when(mock.open(file)).thenReturn(new FileInputStream(realMovieFile));
        } catch (IOException e) {
            throw new AssertionError("Could not get stream for real file", e);
        }
        return file;
    }

    public static File randomRealMovieFile() {
        final File[] files = SOURCE_MOVIES.listFiles();
        if (!SOURCE_MOVIES.exists() || files == null) {
            throw new IllegalStateException("Source movies folder does not have any files");
        }

        return files[SECURE_RANDOM.nextInt(files.length)];
    }
}
