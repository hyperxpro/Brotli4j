/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;

/**
 * InputStream that wraps native brotli decoder.
 */
@Upstream
public class BrotliInputStream extends InputStream {
    /**
     * The default internal buffer size used by the decoder.
     */
    private static final int DEFAULT_BUFFER_SIZE = 16384;

    private final Decoder decoder;

    /**
     * Creates a BrotliInputStream
     *
     * @param source     underlying source
     * @param bufferSize intermediate buffer size
     * @throws IOException If any failure during initialization
     */
    public BrotliInputStream(InputStream source, int bufferSize)
            throws IOException {
        this.decoder = new Decoder(Channels.newChannel(source), bufferSize);
    }

    /**
     * Creates a BrotliInputStream
     *
     * @param source underlying source
     * @throws IOException If any failure during initialization
     */
    public BrotliInputStream(InputStream source) throws IOException {
        this(source, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a BrotliInputStream
     *
     * @param dictionary {@link ByteBuffer} containing dictionary
     * @throws IOException If any failure during initialization
     */
    public void attachDictionary(ByteBuffer dictionary) throws IOException {
        decoder.attachDictionary(dictionary);
    }

    public void enableEagerOutput() {
        decoder.enableEagerOutput();
    }

    @Override
    public void close() throws IOException {
        decoder.close();
    }

    @Override
    public int available() {
        return (decoder.buffer != null) ? decoder.buffer.remaining() : 0;
    }

    @Override
    public int read() throws IOException {
        if (decoder.closed) {
            throw new IOException("read after close");
        }
        int decoded;
        // Iterate until at least one byte is decoded, or EOF reached.
        while (true) {
            decoded = decoder.decode();
            if (decoded != 0) {
                break;
            }
            Thread.yield();
        }

        if (decoded == -1) {
            return -1;
        }
        return decoder.buffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (decoder.closed) {
            throw new IOException("read after close");
        }
        if (decoder.decode() == -1) {
            return -1;
        }
        int result = 0;
        while (len > 0) {
            int limit;
            while ((limit = Math.min(len, decoder.buffer.remaining())) == 0) {
                Thread.yield();
            }
            decoder.buffer.get(b, off, limit);
            off += limit;
            len -= limit;
            result += limit;
            if (decoder.decode() == -1) {
                break;
            }
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        if (decoder.closed) {
            throw new IOException("read after close");
        }
        long result = 0;
        while (n > 0) {
            if (decoder.decode() == -1) {
                break;
            }
            int limit = (int) Math.min(n, decoder.buffer.remaining());
            decoder.discard(limit);
            result += limit;
            n -= limit;
        }
        return result;
    }
}
