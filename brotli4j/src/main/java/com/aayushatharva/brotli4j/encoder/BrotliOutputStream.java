/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020-2022 Aayush Atharva
 *
 * Brotli4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brotli4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brotli4j.  If not, see <https://www.gnu.org/licenses/>.
 */
/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;

/**
 * Output stream that wraps native brotli encoder.
 */
@Upstream
public class BrotliOutputStream extends OutputStream {
    /**
     * The default internal buffer size used by the encoder.
     */
    private static final int DEFAULT_BUFFER_SIZE = 16384;

    private final Encoder encoder;

    /**
     * Creates a BrotliOutputStream.
     *
     * @param destination underlying destination
     * @param params      encoding settings
     * @param bufferSize  intermediate buffer size
     * @throws IOException If any failure during initialization
     */
    public BrotliOutputStream(OutputStream destination, Encoder.Parameters params, int bufferSize)
            throws IOException {
        this.encoder = new Encoder(Channels.newChannel(destination), params, bufferSize);
    }

    /**
     * Creates a BrotliOutputStream.
     *
     * @param destination underlying destination
     * @param params      encoding settings
     * @throws IOException If any failure during initialization
     */
    public BrotliOutputStream(OutputStream destination, Encoder.Parameters params)
            throws IOException {
        this(destination, params, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Creates a BrotliOutputStream.
     *
     * @param destination underlying destination
     * @throws IOException If any failure during initialization
     */
    public BrotliOutputStream(OutputStream destination) throws IOException {
        this(destination, new Encoder.Parameters());
    }

    public void attachDictionary(PreparedDictionary dictionary) throws IOException {
        encoder.attachDictionary(dictionary);
    }

    @Override
    public void close() throws IOException {
        encoder.close();
    }

    @Override
    public void flush() throws IOException {
        if (encoder.closed) {
            throw new IOException("write after close");
        }
        encoder.flush();
    }

    @Override
    public void write(int b) throws IOException {
        if (encoder.closed) {
            throw new IOException("write after close");
        }
        while (!encoder.encode(EncoderJNI.Operation.PROCESS)) {
            // Busy-wait loop.
        }
        encoder.inputBuffer.put((byte) b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (encoder.closed) {
            throw new IOException("write after close");
        }
        while (len > 0) {
            if (!encoder.encode(EncoderJNI.Operation.PROCESS)) {
                continue;
            }
            int limit = Math.min(len, encoder.inputBuffer.remaining());
            encoder.inputBuffer.put(b, off, limit);
            off += limit;
            len -= limit;
        }
    }
}
