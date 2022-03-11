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

import com.aayushatharva.brotli4j.decoder.Decoders;
import com.aayushatharva.brotli4j.encoder.Encoder;
import com.aayushatharva.brotli4j.encoder.Encoders;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class FileCompressAndDecompressTest {

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    void bigFileCompressionAndDecompressionTest() {
        String fileName = "sample_data.txt";

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(fileName)) {
            assert in != null;
            String result = new BufferedReader(new InputStreamReader(in))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Amplify the data 1024 times
            int amplification = 1024;
            StringBuilder sb = new StringBuilder(result.length() * amplification);
            for (int i = 0; i < amplification; i++) {
                sb.append(result);
            }

            byte[] data = sb.toString().getBytes();
            ByteBuf originalData = Unpooled.wrappedBuffer(data);
            ByteBuf compressedData = PooledByteBufAllocator.DEFAULT.directBuffer();
            ByteBuf uncompressedResultData = PooledByteBufAllocator.DEFAULT.directBuffer(data.length);

            Encoders.compress(originalData, compressedData);
            Decoders.decompress(compressedData, uncompressedResultData);

            assertArrayEquals(data, ByteBufUtil.getBytes(uncompressedResultData));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Test
    void randomCharactersTest() throws IOException {
        Random random = new Random();

        byte[] chars = new byte[1024 * 100];
        random.nextBytes(chars); // Random bytes cannot be compressed

        byte[] compressed = Encoder.compress(chars, new Encoder.Parameters().setQuality(4));
        Assertions.assertTrue(chars.length < compressed.length);
    }
}
