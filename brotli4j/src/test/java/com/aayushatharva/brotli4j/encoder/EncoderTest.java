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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
        ByteBuf src = Unpooled.wrappedBuffer("Meow".getBytes());
        ByteBuf dst = Unpooled.directBuffer();
        Encoders.compress(src, dst);

        assertArrayEquals(compressedData, ByteBufUtil.getBytes(dst));
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
