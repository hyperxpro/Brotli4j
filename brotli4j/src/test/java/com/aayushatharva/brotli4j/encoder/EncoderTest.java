/*
 *   Copyright 2021, Aayush Atharva
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
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EncoderTest {

    private static final byte[] compressedData = new byte[]{-117, 1, -128, 77, 101, 111, 119, 3};

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    void compress() throws IOException {
        assertArrayEquals(compressedData, Encoder.compress("Meow".getBytes()));
    }

    @Test
    void compressWithQuality() throws IOException {
        assertArrayEquals(compressedData, Encoder.compress("Meow".getBytes(), new Encoder.Parameters().setQuality(6)));
    }

    @Test
    void compressWithQualityAndByteBuffer() throws IOException {
        ByteBuffer src = ByteBuffer.wrap("Meow".getBytes(StandardCharsets.UTF_8));
        ByteBuffer dst = ByteBuffer.allocate(16);
        Encoder.compress(src, dst, new Encoder.Parameters());

        byte[] arr = new byte[dst.remaining()];
        dst.get(arr);

        assertArrayEquals(compressedData, arr);
    }

    @Test
    void compressWithModes() throws IOException {
        final byte[] text = "Some long text, very long text".getBytes();
        final Encoder.Parameters parameters = new Encoder.Parameters();

        final byte[] compressedGeneric = Encoder.compress(text, parameters.setMode(Encoder.Mode.GENERIC));
        assertEquals(34, compressedGeneric.length);

        final byte[] compressedText = Encoder.compress(text, parameters.setMode(Encoder.Mode.TEXT));
        assertEquals(34, compressedText.length);

        final byte[] compressedFont = Encoder.compress(text, parameters.setMode(Encoder.Mode.FONT));
        assertEquals(31, compressedFont.length);
    }
}
