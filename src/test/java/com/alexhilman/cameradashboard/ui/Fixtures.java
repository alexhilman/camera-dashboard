package com.alexhilman.cameradashboard.ui;

import com.alexhilman.dlink.dcs936.Dcs936Client;
import com.alexhilman.dlink.dcs936.model.DcsFile;
import com.alexhilman.dlink.dcs936.model.DcsFileType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class Fixtures {
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
        try {
            when(mock.open(file)).thenReturn(new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            throw new AssertionError();
        }
        return file;
    }
}
