/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020 Aayush Atharva
 *
 * ShieldBlaze ExpressGateway is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ShieldBlaze ExpressGateway is distributed in the hope that it will be useful,
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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * JNI wrapper for brotli encoder.
 */
public class EncoderJNI {
    private static native ByteBuffer nativeCreate(long[] context);

    private static native void nativePush(long[] context, int length);

    private static native ByteBuffer nativePull(long[] context);

    private static native void nativeDestroy(long[] context);

    public enum Operation {
        PROCESS,
        FLUSH,
        FINISH
    }

    static class Wrapper {
        protected final long[] context = new long[5];
        private final ByteBuffer inputBuffer;

        Wrapper(int inputBufferSize, int quality, int lgwin) throws IOException {
            if (inputBufferSize <= 0) {
                throw new IOException("buffer size must be positive");
            }
            this.context[1] = inputBufferSize;
            this.context[2] = quality;
            this.context[3] = lgwin;
            this.inputBuffer = nativeCreate(this.context);
            if (this.context[0] == 0) {
                throw new IOException("failed to initialize native brotli encoder");
            }
            this.context[1] = 1;
            this.context[2] = 0;
            this.context[3] = 0;
        }

        void push(Operation op, int length) {
            if (length < 0) {
                throw new IllegalArgumentException("negative block length");
            }
            if (context[0] == 0) {
                throw new IllegalStateException("brotli encoder is already destroyed");
            }
            if (!isSuccess() || hasMoreOutput()) {
                throw new IllegalStateException("pushing input to encoder in unexpected state");
            }
            if (hasRemainingInput() && length != 0) {
                throw new IllegalStateException("pushing input to encoder over previous input");
            }
            context[1] = op.ordinal();
            nativePush(context, length);
        }

        boolean isSuccess() {
            return context[1] != 0;
        }

        boolean hasMoreOutput() {
            return context[2] != 0;
        }

        boolean hasRemainingInput() {
            return context[3] != 0;
        }

        boolean isFinished() {
            return context[4] != 0;
        }

        ByteBuffer getInputBuffer() {
            return inputBuffer;
        }

        ByteBuffer pull() {
            if (context[0] == 0) {
                throw new IllegalStateException("brotli encoder is already destroyed");
            }
            if (!isSuccess() || !hasMoreOutput()) {
                throw new IllegalStateException("pulling while data is not ready");
            }
            return nativePull(context);
        }

        /**
         * Releases native resources.
         */
        void destroy() {
            if (context[0] == 0) {
                throw new IllegalStateException("brotli encoder is already destroyed");
            }
            nativeDestroy(context);
            context[0] = 0;
        }
    }
}
