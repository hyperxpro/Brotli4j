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

package com.aayushatharva.brotli4j.decoder;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * JNI wrapper for brotli decoder.
 */
public class DecoderJNI {
    private static native ByteBuffer nativeCreate(long[] context);

    private static native void nativePush(long[] context, int length);

    private static native ByteBuffer nativePull(long[] context);

    private static native void nativeDestroy(long[] context);

    public enum Status {
        ERROR,
        DONE,
        NEEDS_MORE_INPUT,
        NEEDS_MORE_OUTPUT,
        OK
    }

    public static class Wrapper {
        private final long[] context = new long[3];
        private final ByteBuffer inputBuffer;
        private Status lastStatus = Status.NEEDS_MORE_INPUT;
        private boolean fresh = true;

        public Wrapper(int inputBufferSize) throws IOException {
            this.context[1] = inputBufferSize;
            this.inputBuffer = nativeCreate(this.context);
            if (this.context[0] == 0) {
                throw new IOException("failed to initialize native brotli decoder");
            }
        }

        public void push(int length) {
            if (length < 0) {
                throw new IllegalArgumentException("negative block length");
            }
            if (context[0] == 0) {
                throw new IllegalStateException("brotli decoder is already destroyed");
            }
            if (lastStatus != Status.NEEDS_MORE_INPUT && lastStatus != Status.OK) {
                throw new IllegalStateException("pushing input to decoder in " + lastStatus + " state");
            }
            if (lastStatus == Status.OK && length != 0) {
                throw new IllegalStateException("pushing input to decoder in OK state");
            }
            fresh = false;
            nativePush(context, length);
            parseStatus();
        }

        private void parseStatus() {
            long status = context[1];
            if (status == 1) {
                lastStatus = Status.DONE;
            } else if (status == 2) {
                lastStatus = Status.NEEDS_MORE_INPUT;
            } else if (status == 3) {
                lastStatus = Status.NEEDS_MORE_OUTPUT;
            } else if (status == 4) {
                lastStatus = Status.OK;
            } else {
                lastStatus = Status.ERROR;
            }
        }

        public Status getStatus() {
            return lastStatus;
        }

        public ByteBuffer getInputBuffer() {
            return inputBuffer;
        }

        public boolean hasOutput() {
            return context[2] != 0;
        }

        public ByteBuffer pull() {
            if (context[0] == 0) {
                throw new IllegalStateException("brotli decoder is already destroyed");
            }
            if (lastStatus != Status.NEEDS_MORE_OUTPUT && !hasOutput()) {
                throw new IllegalStateException("pulling output from decoder in " + lastStatus + " state");
            }
            fresh = false;
            ByteBuffer result = nativePull(context);
            parseStatus();
            return result;
        }

        /**
         * Releases native resources.
         */
        public void destroy() {
            if (context[0] == 0) {
                throw new IllegalStateException("brotli decoder is already destroyed");
            }
            nativeDestroy(context);
            context[0] = 0;
        }
    }
}
