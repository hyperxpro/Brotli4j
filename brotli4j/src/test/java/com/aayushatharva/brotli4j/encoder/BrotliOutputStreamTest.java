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
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BrotliOutputStreamTest {

    private static final byte[] compressedData = new byte[]{-117, 1, -128, 77, 101, 111, 119, 3};

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    void compress() throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BrotliOutputStream brotliOutputStream = new BrotliOutputStream(byteArrayOutputStream)) {
            brotliOutputStream.write("Meow".getBytes());
            brotliOutputStream.close();
            byteArrayOutputStream.close();

            assertArrayEquals(compressedData, byteArrayOutputStream.toByteArray());
        }
    }
}
