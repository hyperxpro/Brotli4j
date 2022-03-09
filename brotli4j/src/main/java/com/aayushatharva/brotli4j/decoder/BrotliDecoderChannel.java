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
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

/**
 * ReadableByteChannel that wraps native brotli decoder.
 */
@Upstream
public class BrotliDecoderChannel extends Decoder implements ReadableByteChannel {
    /**
     * The default internal buffer size used by the decoder.
     */
    private static final int DEFAULT_BUFFER_SIZE = 16384;

    private final Object mutex = new Object();

    /**
     * Creates a BrotliDecoderChannel.
     *
     * @param source           underlying source
     * @param bufferSize       intermediate buffer size
     */
    public BrotliDecoderChannel(ReadableByteChannel source, int bufferSize) throws IOException {
        super(source, bufferSize);
    }

    public BrotliDecoderChannel(ReadableByteChannel source) throws IOException {
        this(source, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void attachDictionary(ByteBuffer dictionary) throws IOException {
        super.attachDictionary(dictionary);
    }

    @Override
    public boolean isOpen() {
        synchronized (mutex) {
            return !closed;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (mutex) {
            super.close();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        synchronized (mutex) {
            if (closed) {
                throw new ClosedChannelException();
            }
            int result = 0;
            while (dst.hasRemaining()) {
                int outputSize = decode();
                if (outputSize <= 0) {
                    return result == 0 ? outputSize : result;
                }
                result += consume(dst);
            }
            return result;
        }
    }
}
