/*
 *   Copyright (c) 2020-2022, Aayush Atharva
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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {

    private static final byte[] COMPRESSED_DATA = {-117, 1, -128, 77, 101, 111, 119, 3};
    private static final int ORIGINAL_DATA_LENGTH = 4; // 'Meow' length

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    void decompress() throws IOException {
        DirectDecompress directDecompress = Decoder.decompress(COMPRESSED_DATA);
        assertEquals(DecoderJNI.Status.DONE, directDecompress.getResultStatus());
        assertEquals("Meow", new String(directDecompress.getDecompressedData()));
    }

    @Test
    void decompressWithByteBuffer() throws IOException {
        ByteBuf src = Unpooled.wrappedBuffer(COMPRESSED_DATA);
        ByteBuf dst = Unpooled.directBuffer();

        DirectDecompress directDecompress = Decoders.decompress(src, dst);
        assertEquals(DecoderJNI.Status.DONE, directDecompress.getResultStatus());
        assertEquals("Meow", new String(directDecompress.getDecompressedData()));
    }

    @Test
    void decompressKnownLength() throws IOException {
        DirectDecompress directDecompress = Decoder.decompressKnownLength(COMPRESSED_DATA, ORIGINAL_DATA_LENGTH);
        assertEquals(DecoderJNI.Status.DONE, directDecompress.getResultStatus());
        assertEquals("Meow", new String(directDecompress.getDecompressedData()));
    }

    @Test
    void decompressKnownLengthDataTooBig() {
        assertThrows(IllegalArgumentException.class,
                     () -> Decoder.decompressKnownLength(COMPRESSED_DATA, ORIGINAL_DATA_LENGTH - 1));
    }

    @Test
    void decompressKnownLengthDataTooSmall() {
        assertThrows(IllegalArgumentException.class,
                     () -> Decoder.decompressKnownLength(COMPRESSED_DATA, ORIGINAL_DATA_LENGTH + 1));
    }
}
