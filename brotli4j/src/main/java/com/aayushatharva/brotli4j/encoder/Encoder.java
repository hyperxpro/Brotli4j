/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.common.annotations.Local;
import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for OutputStream / Channel implementations.
 */
@Upstream
@Local
public class Encoder {
    private final WritableByteChannel destination;
    private final List<PreparedDictionary> dictionaries;
    private final EncoderJNI.Wrapper encoder;
    private ByteBuffer buffer;
    final ByteBuffer inputBuffer;
    boolean closed;

    /**
     * Creates a Encoder wrapper.
     *
     * @param destination     underlying destination
     * @param params          encoding parameters
     * @param inputBufferSize read buffer size
     */
    public Encoder(WritableByteChannel destination, Parameters params, int inputBufferSize) throws IOException {
        if (inputBufferSize <= 0) {
            throw new IllegalArgumentException("buffer size must be positive");
        }

        if (destination == null) {
            throw new NullPointerException("destination can not be null");
        }
        this.dictionaries = new ArrayList<>();
        this.destination = destination;
        this.encoder = new EncoderJNI.Wrapper(inputBufferSize, params.quality, params.lgwin, params.mode);
        this.inputBuffer = this.encoder.getInputBuffer();
    }

    /*
     * Encodes the given data buffer.
     *
     * @param data   byte array to be compressed
     * @param params {@link Parameters} instance
     * @return compressed byte array
     * @throws IOException If any failure during encoding
     */
    @Upstream
    public static byte[] compress(byte[] data, int offset, int length, Parameters params) throws IOException {
        if (length == 0) {
            byte[] empty = new byte[1];
            empty[0] = 6;
            return empty;
        }
        /* data.length > 0 */
        EncoderJNI.Wrapper encoder = new EncoderJNI.Wrapper(length, params.quality, params.lgwin, params.mode);
        ArrayList<byte[]> output = new ArrayList<>();
        int totalOutputSize = 0;
        try {
            encoder.getInputBuffer().put(data, offset, length);
            encoder.push(EncoderJNI.Operation.FINISH, length);
            while (true) {
                if (!encoder.isSuccess()) {
                    throw new IOException("encoding failed");
                } else if (encoder.hasMoreOutput()) {
                    ByteBuffer buffer = encoder.pull();
                    byte[] chunk = new byte[buffer.remaining()];
                    buffer.get(chunk);
                    output.add(chunk);
                    totalOutputSize += chunk.length;
                } else if (!encoder.isFinished()) {
                    encoder.push(EncoderJNI.Operation.FINISH, 0);
                } else {
                    break;
                }
            }
        } finally {
            encoder.destroy();
        }
        if (output.size() == 1) {
            return output.get(0);
        }
        byte[] result = new byte[totalOutputSize];
        int resultOffset = 0;
        for (byte[] chunk : output) {
            System.arraycopy(chunk, 0, result, resultOffset, chunk.length);
            resultOffset += chunk.length;
        }
        return result;
    }

    @Local
    public static byte[] compress(byte[] data) throws IOException {
        return compress(data, Parameters.DEFAULT);
    }

    @Upstream
    /* Encodes the given data buffer. */
    public static byte[] compress(byte[] data, Parameters params) throws IOException {
        return compress(data, 0, data.length, params);
    }

    @Upstream
    public static byte[] compress(byte[] data, int offset, int length) throws IOException {
        return compress(data, offset, length, new Parameters());
    }

    /**
     * Prepares raw or serialized dictionary for being used by encoder.
     *
     * @param dictionary           raw / serialized dictionary data; MUST be direct
     * @param sharedDictionaryType dictionary data type
     * @return {@link PreparedDictionary} instance
     */
    @Upstream
    public static PreparedDictionary prepareDictionary(ByteBuffer dictionary, int sharedDictionaryType) {
        return EncoderJNI.prepareDictionary(dictionary, sharedDictionaryType);
    }

    @Upstream
    private void fail(String message) throws IOException {
        try {
            close();
        } catch (IOException ex) {
            /* Ignore */
        }
        throw new IOException(message);
    }

    @Upstream
    public void attachDictionary(PreparedDictionary dictionary) throws IOException {
        if (!encoder.attachDictionary(dictionary.getData())) {
            fail("failed to attach dictionary");
        }
        // Reference to native prepared dictionary wrapper should be held till the end of encoding.
        dictionaries.add(dictionary);
    }

    /**
     * @param force repeat pushing until all output is consumed
     * @return true if all encoder output is consumed
     */
    @Upstream
    boolean pushOutput(boolean force) throws IOException {
        while (buffer != null) {
            if (buffer.hasRemaining()) {
                destination.write(buffer);
            }
            if (!buffer.hasRemaining()) {
                buffer = null;
            } else if (!force) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if there is space in inputBuffer.
     */
    @Local
    @Upstream
    public boolean encode(EncoderJNI.Operation op) throws IOException {
        boolean force = (op != EncoderJNI.Operation.PROCESS);
        if (force) {
            ((Buffer) inputBuffer).limit(inputBuffer.position());
        } else if (inputBuffer.hasRemaining()) {
            return true;
        }
        boolean hasInput = true;
        while (true) {
            if (!encoder.isSuccess()) {
                fail("encoding failed");
            } else if (!pushOutput(force)) {
                return false;
            } else if (encoder.hasMoreOutput()) {
                buffer = encoder.pull();
            } else if (encoder.hasRemainingInput()) {
                encoder.push(op, 0);
            } else if (hasInput) {
                encoder.push(op, inputBuffer.limit());
                hasInput = false;
            } else {
                ((Buffer) inputBuffer).clear();
                return true;
            }
        }
    }

    @Local
    @Upstream
    public void flush() throws IOException {
        encode(EncoderJNI.Operation.FLUSH);
    }

    @Upstream
    void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            encode(EncoderJNI.Operation.FINISH);
        } finally {
            encoder.destroy();
            destination.close();
        }
    }

    /**
     * <a href="https://www.brotli.org/encode.html#aa6f">...</a>
     * See encode.h, typedef enum BrotliEncoderMode
     * <p>
     * <strong>Important</strong>: The ordinal value of the
     * modes should be the same as the constant values in encode.h
     */
    public enum Mode {
        /**
         * Default compression mode.
         * In this mode compressor does not know anything in advance about the properties of the input.
         */
        GENERIC,
        /**
         * Compression mode for UTF-8 formatted text input.
         */
        TEXT,
        /**
         * Compression mode used in WOFF 2.0.
         */
        FONT;

        // see: https://www.gamlor.info/wordpress/2017/08/javas-enum-values-hidden-allocations/
        private static final Mode[] ALL_VALUES = values();

        public static Mode of(int value) {
            return ALL_VALUES[value];
        }
    }

    /**
     * Brotli encoder settings.
     */
    @Upstream
    @Local
    public static final class Parameters {
        @Local
        public static final Parameters DEFAULT = new Parameters();

        private int quality = -1;
        private int lgwin = -1;
        private Mode mode;

        public Parameters() {
        }

        private Parameters(Parameters other) {
            this.quality = other.quality;
            this.lgwin = other.lgwin;
            this.mode = other.mode;
        }

        /**
         * @param quality compression quality, or -1 for default
         * @return this instance
         */
        public Parameters setQuality(int quality) {
            if (quality < -1 || quality > 11) {
                throw new IllegalArgumentException("quality should be in range [0, 11], or -1");
            }
            this.quality = quality;
            return this;
        }

        /**
         * @param lgwin log2(LZ window size), or -1 for default
         * @return this instance
         */
        public Parameters setWindow(int lgwin) {
            if ((lgwin != -1) && ((lgwin < 10) || (lgwin > 24))) {
                throw new IllegalArgumentException("lgwin should be in range [10, 24], or -1");
            }
            this.lgwin = lgwin;
            return this;
        }

        /**
         * @param mode compression mode, or {@code null} for default
         * @return this instance
         */
        public Parameters setMode(Mode mode) {
            this.mode = mode;
            return this;
        }

        public int quality() {
            return quality;
        }

        public int lgwin() {
            return lgwin;
        }

        public Mode mode() {
            return mode;
        }
    }
}
