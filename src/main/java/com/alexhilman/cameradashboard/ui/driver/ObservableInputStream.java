package com.alexhilman.cameradashboard.ui.driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Stream which can be observed by any number of other observers (other input streams).
 */
public class ObservableInputStream extends InputStream {
    private static final Logger LOG = LogManager.getLogger(ObservableInputStream.class);

    private final List<PipedOutputStream> observerStreams = new LinkedList<>();
    private final Object lock = new Object();

    private final InputStream realInputStream;

    public ObservableInputStream(final InputStream realInputStream) {
        this.realInputStream = realInputStream;
    }

    @Override
    public int read() throws IOException {
        return observedRead();
    }

    @Override
    public int read(final byte[] b) throws IOException {
        return observedRead(b, 0, b.length);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        return observedRead(b, off, len);
    }

    @Override
    public long skip(final long n) throws IOException {
        return realInputStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return realInputStream.available();
    }

    @Override
    public void close() throws IOException {
        realInputStream.close();
    }

    private synchronized int observedRead() throws IOException {
        LOG.warn("Possible terrible performance: read single byte");

        final int byteRead = realInputStream.read();

        final List<PipedOutputStream> streamsToClose = new LinkedList<>();

        if (byteRead == -1) {
            streamsToClose.addAll(observerStreams);
        } else {
            observerStreams.forEach(pipedOutputStream -> {
                try {
                    pipedOutputStream.write(byteRead);
                } catch (IOException e) {
                    LOG.error("Could not write to observer stream; closing stream", e);
                    streamsToClose.add(pipedOutputStream);
                }
            });
        }

        streamsToClose.forEach(stream -> {
            try {
                stream.close();
            } catch (IOException e) {
                LOG.error("Could not close stream", e);
            } finally {
                observerStreams.remove(stream);
            }
        });
        return byteRead;
    }

    private synchronized int observedRead(final byte[] b, final int off, final int len) throws IOException {
        final byte[] localCopy = new byte[len];
        final int read = realInputStream.read(localCopy, 0, localCopy.length);

        final List<PipedOutputStream> streamsToClose = new LinkedList<>();
        observerStreams.forEach(pipedOutputStream -> {
            LOG.info("Sending {} bytes to observer {}", read, pipedOutputStream);
            final byte[] observerCopy = new byte[localCopy.length];
            System.arraycopy(localCopy, 0, observerCopy, 0, read);
            try {
                pipedOutputStream.write(observerCopy, 0, read);
            } catch (IOException e) {
                LOG.error("Could not write to observer stream; closing stream", e);
                streamsToClose.add(pipedOutputStream);
            }
        });

        System.arraycopy(localCopy, 0, b, off, read);

        streamsToClose.forEach(stream -> {
            try {
                stream.close();
            } catch (IOException e) {
                LOG.error("Could not close stream", e);
            } finally {
                observerStreams.remove(stream);
            }
        });
        return read;
    }

    /**
     * Spawn a new observer to the existing stream in its current state - no bytes are replayed.
     *
     * @return
     * @throws IOException
     */
    public synchronized InputStream newObserver() throws IOException {
        final PipedOutputStream src = new PipedOutputStream();
        observerStreams.add(src);
        return new PipedInputStream(src);
    }
}
