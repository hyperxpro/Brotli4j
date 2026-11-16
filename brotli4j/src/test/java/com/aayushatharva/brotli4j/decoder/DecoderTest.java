/*
 *   Copyright (c) 2020-2025, Aayush Atharva
 *
 *   Brotli4j licenses this file to you under the
 *   Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.common.BrotliCommon;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.aayushatharva.brotli4j.encoder.PreparedDictionary;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoderTest {

    private static final byte[] compressedData = new byte[]{-117, 1, -128, 77, 101, 111, 119, 3};
    // Bit 4 of the first byte is a reserved bit that must be 0; here it is 1.
    private static final byte[] corruptData = new byte[]{0x1C, 0, 0, 0};
    private static final byte[] dictionary = "Some dictionary content shared by encoder and decoder".getBytes();
    private static final byte[] dictionaryCompressible =
            "Data that repeats Some dictionary content shared by encoder and decoder".getBytes();

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    void decompress() throws IOException {
        DirectDecompress directDecompress = Decoder.decompress(compressedData);
        assertEquals(DecoderJNI.Status.DONE, directDecompress.getResultStatus());
        assertEquals("Meow", new String(directDecompress.getDecompressedData()));
    }

    @Test
    void decompressWithByteBuffer() throws IOException {
        ByteBuf src = Unpooled.wrappedBuffer(compressedData);
        ByteBuf dst = Unpooled.directBuffer();

        DirectDecompress directDecompress = Decoders.decompress(src, dst);
        assertEquals(DecoderJNI.Status.DONE, directDecompress.getResultStatus());
        assertEquals("Meow", new String(directDecompress.getDecompressedData()));
    }

    @Test
    void boundedPullCapsEachChunk() throws IOException {
        byte[] original = new byte[64 * 1024];
        Arrays.fill(original, (byte) 'a');
        byte[] compressed = Encoder.compress(original);

        int cap = 4096;
        DecoderJNI.Wrapper decoder = new DecoderJNI.Wrapper(compressed.length);
        byte[] result = new byte[original.length];
        int produced = 0;
        try {
            decoder.getInputBuffer().put(compressed);
            decoder.push(compressed.length);
            while (decoder.getStatus() != DecoderJNI.Status.DONE) {
                switch (decoder.getStatus()) {
                    case OK:
                        decoder.push(0);
                        break;
                    case NEEDS_MORE_OUTPUT:
                        ByteBuffer chunk = decoder.pull(cap);
                        assertTrue(chunk.remaining() <= cap,
                                "pull(int) returned " + chunk.remaining() + " > cap " + cap);
                        int n = chunk.remaining();
                        chunk.get(result, produced, n);
                        produced += n;
                        break;
                    default:
                        throw new IOException("unexpected status " + decoder.getStatus());
                }
            }
        } finally {
            decoder.destroy();
        }
        assertEquals(original.length, produced);
        assertArrayEquals(original, result);
    }

    @Test
    void wrapperConstructorAppliesStickyCap() throws IOException {
        byte[] original = new byte[64 * 1024];
        Arrays.fill(original, (byte) 'b');
        byte[] compressed = Encoder.compress(original);

        int cap = 8192;
        DecoderJNI.Wrapper decoder = new DecoderJNI.Wrapper(compressed.length, cap);
        try {
            decoder.getInputBuffer().put(compressed);
            decoder.push(compressed.length);
            while (decoder.getStatus() != DecoderJNI.Status.DONE) {
                switch (decoder.getStatus()) {
                    case OK:
                        decoder.push(0);
                        break;
                    case NEEDS_MORE_OUTPUT:
                        ByteBuffer chunk = decoder.pull();
                        assertTrue(chunk.remaining() <= cap,
                                "sticky cap not applied: chunk=" + chunk.remaining());
                        break;
                    default:
                        throw new IOException("unexpected status " + decoder.getStatus());
                }
            }
        } finally {
            decoder.destroy();
        }
    }

    @Test
    void decompressRejectsOutputExceedingMax() throws IOException {
        byte[] original = new byte[64 * 1024];
        Arrays.fill(original, (byte) 'c');
        byte[] compressed = Encoder.compress(original);

        IOException ex = assertThrows(IOException.class,
                () -> Decoder.decompress(compressed, 1024));
        assertTrue(ex.getMessage().contains("maximum size"),
                "unexpected message: " + ex.getMessage());
    }

    @Test
    void decompressWithMaxOutputAllowsExactFit() throws IOException {
        byte[] original = new byte[8 * 1024];
        Arrays.fill(original, (byte) 'd');
        byte[] compressed = Encoder.compress(original);

        DirectDecompress result = Decoder.decompress(compressed, original.length);
        assertEquals(DecoderJNI.Status.DONE, result.getResultStatus());
        assertArrayEquals(original, result.getDecompressedData());
    }

    @Test
    void createFailsForInvalidBufferSize() {
        // Size 0 is rejected by native code; -1 becomes SIZE_MAX and cannot be allocated.
        assertThrows(IOException.class, () -> new DecoderJNI.Wrapper(0));
        assertThrows(IOException.class, () -> new DecoderJNI.Wrapper(-1));
    }

    @Test
    void corruptStream() throws IOException {
        DecoderJNI.Wrapper decoder = newWrapper(corruptData);
        try {
            decoder.push(corruptData.length);
            assertEquals(DecoderJNI.Status.ERROR, decoder.getStatus());
        } finally {
            decoder.destroy();
        }
    }

    @Test
    void bytesAfterStreamEnd() throws IOException {
        byte[] input = Arrays.copyOf(compressedData, compressedData.length + 1);
        DecoderJNI.Wrapper decoder = newWrapper(input);
        try {
            decoder.push(input.length);
            assertEquals(DecoderJNI.Status.NEEDS_MORE_OUTPUT, decoder.getStatus());
            decoder.pull();
            // The stream is finished but input remains, which is reported as an error.
            assertEquals(DecoderJNI.Status.ERROR, decoder.getStatus());
        } finally {
            decoder.destroy();
        }
    }

    @Test
    void attachDictionary() throws Exception {
        byte[] compressed = compressWithDictionary();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BrotliInputStream decoder = new BrotliInputStream(new ByteArrayInputStream(compressed))) {
            // makeNative leaves the position at the end; native code uses address and capacity.
            decoder.attachDictionary(BrotliCommon.makeNative(dictionary));
            int b;
            while ((b = decoder.read()) != -1) {
                out.write(b);
            }
        }
        assertArrayEquals(dictionaryCompressible, out.toByteArray());

        // Without the dictionary the stream refers to missing data.
        assertThrows(IOException.class, () -> Decoder.decompress(compressed, 0, compressed.length));
    }

    @Test
    void failedAttachStillUsesDictionarySlot() throws IOException {
        DecoderJNI.Wrapper decoder = newWrapper(compressedData);
        try {
            // Zero capacity is rejected by the binding before Brotli, but the slot is counted.
            for (int i = 0; i < 15; i++) {
                assertFalse(decoder.attachDictionary(ByteBuffer.allocateDirect(0)));
            }
            assertFalse(decoder.attachDictionary(BrotliCommon.makeNative(dictionary)));
        } finally {
            decoder.destroy();
        }
    }

    private static DecoderJNI.Wrapper newWrapper(byte[] input) throws IOException {
        DecoderJNI.Wrapper decoder = new DecoderJNI.Wrapper(input.length);
        decoder.getInputBuffer().put(input);
        return decoder;
    }

    private static byte[] compressWithDictionary() throws Exception {
        PreparedDictionary prepared = Encoder.prepareDictionary(BrotliCommon.makeNative(dictionary), 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (BrotliOutputStream encoder = new BrotliOutputStream(out)) {
            encoder.attachDictionary(prepared);
            encoder.write(dictionaryCompressible);
        } finally {
            // Native prepared dictionaries implement AutoCloseable; free only after the encoder is closed.
            ((AutoCloseable) prepared).close();
        }
        return out.toByteArray();
    }
}
