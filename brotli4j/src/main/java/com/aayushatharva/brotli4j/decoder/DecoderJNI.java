/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.common.annotations.Local;
import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * JNI wrapper for brotli decoder.
 */
@Upstream
public class DecoderJNI {
    private static native ByteBuffer nativeCreate(long[] context);

    private static native void nativePush(long[] context, int length);

    private static native ByteBuffer nativePull(long[] context);

    private static native ByteBuffer nativePullBounded(long[] context, int maxBytes);

    private static native void nativeDestroy(long[] context);

    private static native boolean nativeAttachDictionary(long[] context, ByteBuffer dictionary);

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
        private final int maxOutputChunkSize;
        private Status lastStatus = Status.NEEDS_MORE_INPUT;
        private boolean fresh = true;

        public Wrapper(int inputBufferSize) throws IOException {
            this(inputBufferSize, 0);
        }

        /**
         * Creates a Wrapper that caps every {@link #pull()} to at most
         * {@code maxOutputChunkSize} bytes. A zero-or-negative value disables
         * the cap (equivalent to the single-argument constructor).
         *
         * <p>Use this to bound peak memory when streaming potentially-malicious
         * compressed input: each pull returns a {@link ByteBuffer} whose
         * {@code remaining()} is at most {@code maxOutputChunkSize}, with
         * remaining decoded output served by subsequent pulls.
         *
         * @param inputBufferSize     size of the decoder's input buffer
         * @param maxOutputChunkSize  per-pull output cap in bytes; {@code 0} for no cap
         * @throws IOException if native decoder initialization fails
         */
        @Local
        public Wrapper(int inputBufferSize, int maxOutputChunkSize) throws IOException {
            this.maxOutputChunkSize = Math.max(maxOutputChunkSize, 0);
            this.context[1] = inputBufferSize;
            this.inputBuffer = nativeCreate(this.context);
            if (this.context[0] == 0) {
                throw new IOException("failed to initialize native brotli decoder");
            }
        }

        public boolean attachDictionary(ByteBuffer dictionary) {
            if (!dictionary.isDirect()) {
                throw new IllegalArgumentException("only direct buffers allowed");
            }
            if (context[0] == 0) {
                throw new IllegalStateException("brotli decoder is already destroyed");
            }
            if (!fresh) {
                throw new IllegalStateException("decoding is already started");
            }
            return nativeAttachDictionary(context, dictionary);
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
            ByteBuffer result = (maxOutputChunkSize > 0)
                    ? nativePullBounded(context, maxOutputChunkSize)
                    : nativePull(context);
            parseStatus();
            return result;
        }

        /**
         * Pulls decompressed output, returning at most {@code maxBytes} bytes.
         * Use this to bound a single allocation when handling untrusted input;
         * any remaining decoded output is served by subsequent pulls.
         *
         * @param maxBytes positive upper bound on the returned buffer size
         * @return direct {@link ByteBuffer} with {@code remaining() <= maxBytes}
         */
        @Local
        public ByteBuffer pull(int maxBytes) {
            if (maxBytes <= 0) {
                throw new IllegalArgumentException("maxBytes must be positive");
            }
            if (context[0] == 0) {
                throw new IllegalStateException("brotli decoder is already destroyed");
            }
            if (lastStatus != Status.NEEDS_MORE_OUTPUT && !hasOutput()) {
                throw new IllegalStateException("pulling output from decoder in " + lastStatus + " state");
            }
            fresh = false;
            ByteBuffer result = nativePullBounded(context, maxBytes);
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
