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
package com.aayushatharva.brotli4j;

import com.aayushatharva.brotli4j.encoder.Encoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

class FileCompressAndDecompressTest {

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    void alphabeticalWordsTest() throws IOException {
        InputStream in = Brotli4jLoader.class.getResourceAsStream("/Words.txt");
        byte[] words = getBytes(in);
        byte[] compressed = Encoder.compress(words, new Encoder.Parameters().setQuality(4));

        Assertions.assertTrue(compressed.length < 2000);
    }

    @Test
    void randomCharactersTest() throws IOException {
        Random random = new Random();

        byte[] chars = new byte[1024 * 100];
        random.nextBytes(chars); // Random bytes cannot be compressed

        byte[] compressed = Encoder.compress(chars, new Encoder.Parameters().setQuality(4));
        Assertions.assertTrue(chars.length < compressed.length);
    }

    static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }
}
