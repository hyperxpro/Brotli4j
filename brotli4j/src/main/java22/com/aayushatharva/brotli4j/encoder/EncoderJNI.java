/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.common.annotations.Upstream;
import com.aayushatharva.brotli4j.internal.NativeBackend;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Brotli encoder wrapper for JDK 22+, calling native code through FFM or JNI as picked by {@link NativeBackend}.
 */
@Upstream
public class EncoderJNI {
    /**
     * Creates the caller of each {@link Wrapper}: a new FFM one (it holds the encoder state) or the shared JNI one.
     */
    private static final Supplier<NativeFunctionCaller> CALLERS =
            NativeBackend.select(() -> FfmNativeFunctionCaller::new, () -> JniNativeFunctionCaller::instance);

    /**
     * Calls the dictionary methods, which are static and use no per-encoder state.
     */
    private static final NativeFunctionCaller DICTIONARY_CALLER = CALLERS.get();

    // Kept here under their original names so the JNI symbols still link.
    private static native ByteBuffer nativeCreate(long[] context);
    private static native void nativePush(long[] context, int length);
    private static native ByteBuffer nativePull(long[] context);
    private static native void nativeDestroy(long[] context);
    private static native boolean nativeAttachDictionary(long[] context, ByteBuffer dictionary);
    private static native ByteBuffer nativePrepareDictionary(ByteBuffer dictionary, long type);
    private static native void nativeDestroyDictionary(ByteBuffer dictionary);

    public enum Operation {
        PROCESS,
        FLUSH,
        FINISH
    }

    private static class PreparedDictionaryImpl implements PreparedDictionary, AutoCloseable {
        private ByteBuffer data;
        private ByteBuffer rawData;

        private PreparedDictionaryImpl(ByteBuffer data, ByteBuffer rawData) {
            this.data = data;
            this.rawData = rawData;
        }

        @Override
        public ByteBuffer getData() {
            return data;
        }

        @Override
        public void close() {
            ByteBuffer data = this.data;
            this.data = null;
            this.rawData = null;
            DICTIONARY_CALLER.nativeDestroyDictionary(data);
        }
    }

    /**
     * Prepares raw or serialized dictionary for being used by encoder.
     *
     * @param dictionary raw / serialized dictionary data; MUST be direct
     * @param sharedDictionaryType dictionary data type
     */
    static PreparedDictionary prepareDictionary(ByteBuffer dictionary, int sharedDictionaryType) {
        if (!dictionary.isDirect()) {
            throw new IllegalArgumentException("only direct buffers allowed");
        }
        ByteBuffer dictionaryData = DICTIONARY_CALLER.nativePrepareDictionary(dictionary, sharedDictionaryType);
        if (dictionaryData == null) {
            throw new IllegalStateException("OOM");
        }
        return new PreparedDictionaryImpl(dictionaryData, dictionary);
    }

    public static class Wrapper {
        private final NativeFunctionCaller caller = CALLERS.get();
        protected final long[] context = new long[5];
        private final ByteBuffer inputBuffer;
        private boolean fresh = true;

        public Wrapper(int inputBufferSize, int quality, int lgwin, Encoder.Mode mode)
                throws IOException {
            if (inputBufferSize <= 0) {
                throw new IOException("buffer size must be positive");
            }
            this.context[1] = inputBufferSize;
            this.context[2] = quality;
            this.context[3] = lgwin;
            this.context[4] = mode != null ? mode.ordinal() : -1;
            this.inputBuffer = caller.nativeCreate(this.context);
            if (this.context[0] == 0) {
                throw new IOException("failed to initialize native brotli encoder");
            }
            this.context[1] = 1;
            this.context[2] = 0;
            this.context[3] = 0;
            this.context[4] = 0;
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
            return caller.nativeAttachDictionary(context, dictionary);
        }

        public void push(Operation op, int length) {
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
            fresh = false;
            caller.nativePush(context, length);
        }

        public boolean isSuccess() {
            return context[1] != 0;
        }

        public boolean hasMoreOutput() {
            return context[2] != 0;
        }

        public boolean hasRemainingInput() {
            return context[3] != 0;
        }

        public boolean isFinished() {
            return context[4] != 0;
        }

        public ByteBuffer getInputBuffer() {
            return inputBuffer;
        }

        public ByteBuffer pull() {
            if (context[0] == 0) {
                throw new IllegalStateException("brotli encoder is already destroyed");
            }
            if (!isSuccess() || !hasMoreOutput()) {
                throw new IllegalStateException("pulling while data is not ready");
            }
            fresh = false;
            return caller.nativePull(context);
        }

        /**
         * Releases native resources.
         */
        public void destroy() {
            if (context[0] == 0) {
                throw new IllegalStateException("brotli encoder is already destroyed");
            }
            caller.nativeDestroy(context);
            context[0] = 0;
        }

    }

    /**
     * Stateless, so one instance serves every {@link Wrapper}.
     */
    private static final class JniNativeFunctionCaller implements NativeFunctionCaller {
        private static final JniNativeFunctionCaller INSTANCE = new JniNativeFunctionCaller();

        static NativeFunctionCaller instance() {
            return INSTANCE;
        }

        @Override
        public ByteBuffer nativeCreate(long[] context) {
            return EncoderJNI.nativeCreate(context);
        }

        @Override
        public void nativePush(long[] context, int length) {
            EncoderJNI.nativePush(context, length);
        }

        @Override
        public ByteBuffer nativePull(long[] context) {
            return EncoderJNI.nativePull(context);
        }

        @Override
        public void nativeDestroy(long[] context) {
            EncoderJNI.nativeDestroy(context);
        }

        @Override
        public boolean nativeAttachDictionary(long[] context, ByteBuffer dictionary) {
            return EncoderJNI.nativeAttachDictionary(context, dictionary);
        }

        @Override
        public ByteBuffer nativePrepareDictionary(ByteBuffer dictionary, long type) {
            return EncoderJNI.nativePrepareDictionary(dictionary, type);
        }

        @Override
        public void nativeDestroyDictionary(ByteBuffer dictionary) {
            EncoderJNI.nativeDestroyDictionary(dictionary);
        }
    }
}
