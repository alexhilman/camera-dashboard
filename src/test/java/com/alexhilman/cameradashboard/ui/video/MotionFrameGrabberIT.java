package com.alexhilman.cameradashboard.ui.video;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MotionFrameGrabberIT {
    private final MotionFrameGrabber motionFrameGrabber = new MotionFrameGrabber();

    @Test
    @Ignore("I'm not going to commit a file with personal info in it just to test the bytes I get out of it")
    public void shouldGrabFrame() throws Exception {
        final byte[] bytes = motionFrameGrabber.grabJpgFrame(new File("/tmp/2017-09-24 12:51:01.mp4"), 3000);

        assertThat(bytes, is(notNullValue()));
        assertThat(bytes.length, greaterThan(0));
    }
}