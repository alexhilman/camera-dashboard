package com.alexhilman.cameradashboard.ui.driver;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ObservableInputStreamTest {
    private ObservableInputStream observableInputStream;

    @Before
    public  void setup() throws UnsupportedEncodingException {
        final String streamContent = "my-bytes";
        observableInputStream = new ObservableInputStream(new ByteArrayInputStream(streamContent.getBytes("utf-8")));
    }

    @Test
    public void observerShouldSeeSameBytesWhenMasterReaderReadsBytes() throws IOException {
        final List<InputStream> observers =
                IntStream.range(0, 5)
                         .mapToObj(i -> {
                             try {
                                 return observableInputStream.newObserver();
                             } catch (IOException e) {
                                 throw new RuntimeException(e);
                             }
                         })
                         .collect(toList());

        int b = 0;
        while ((b = observableInputStream.read()) >= 0) {
            final int masterByte = b;
            observers.forEach(observerStream -> {
                int observerByte = 0;
                try {
                    observerByte = observerStream.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                assertThat(observerByte, is(masterByte));
            });
        }

        observers.forEach(observerStream -> {
            try {
                assertThat(observerStream.read(), is(-1)); //should both end on -1
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void observersShouldNotNeedToObserveImmediately() throws IOException {
        final StringBuilder masterStreamContent = new StringBuilder();

        final List<InputStream> observers = IntStream.range(0, 5)
                                                     .mapToObj(i -> {
                                                         try {
                                                             return observableInputStream.newObserver();
                                                         } catch (IOException e) {
                                                             throw new RuntimeException(e);
                                                         }
                                                     })
                                                     .collect(toList());

        int b = 0;
        while ((b = observableInputStream.read()) > -1) {
            masterStreamContent.append((char) b);
        }

        observers.forEach(observer -> {
            final byte[] buffer = new byte[100];
            final int bytesRead;
            try {
                bytesRead = observer.read(buffer, 0, buffer.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            final String s = new String(buffer, 0, bytesRead);
            assertThat(s, is(masterStreamContent.toString()));
        });
    }
}