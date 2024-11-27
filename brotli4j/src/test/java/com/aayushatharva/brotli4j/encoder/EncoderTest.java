/*
 *   Copyright (c) 2020-2023, Aayush Atharva
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
import com.aayushatharva.brotli4j.common.BrotliCommon;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

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

    @Test
    void encodeModeEnumValues() {
        assertEquals(Encoder.Mode.FONT, Encoder.Mode.of(Encoder.Mode.FONT.ordinal()));
        assertEquals(Encoder.Mode.TEXT, Encoder.Mode.of(Encoder.Mode.TEXT.ordinal()));
        assertEquals(Encoder.Mode.GENERIC, Encoder.Mode.of(Encoder.Mode.GENERIC.ordinal()));
    }


    @Test
    void ensureDictionaryDataRemainsAfterGC() throws IOException, InterruptedException {
        // We hard code the compressed data, since the dictionary could also be collected just before our first compression
        final byte[] expectedCompression = new byte[]{27, 43, 0, -8, 37, 0, -62, -104, -40, -63, 0};
        final String dictionaryData = "This is some data to be used as a dictionary";
        final byte[] rawBytes = dictionaryData.getBytes(); // Use dictionary also as data to keep it small
        final PreparedDictionary dic = Encoder.prepareDictionary(BrotliCommon.makeNative(dictionaryData.getBytes()), 0);

        // Create gc pressure to trigger potential collection of dictionary data
        ArrayList<Integer> hashes = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String obj = String.valueOf(Math.random());
            hashes.add(obj.hashCode());
        }
        hashes = null;
        System.gc();

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BrotliOutputStream brotliOutputStream = new BrotliOutputStream(byteArrayOutputStream)) {
            brotliOutputStream.attachDictionary(dic);
            brotliOutputStream.write(rawBytes);
            brotliOutputStream.close();
            byteArrayOutputStream.close();
            assertArrayEquals(expectedCompression, byteArrayOutputStream.toByteArray());  // Otherwise the GC already cleared the data
        }
    }
}
