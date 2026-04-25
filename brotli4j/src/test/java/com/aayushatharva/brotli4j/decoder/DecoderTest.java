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
import com.aayushatharva.brotli4j.encoder.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecoderTest {

    private static final byte[] compressedData = new byte[]{-117, 1, -128, 77, 101, 111, 119, 3};

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
}
