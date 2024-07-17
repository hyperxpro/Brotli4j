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
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BrotliDecoderChannelTest {
    private static final byte[] compressedData = new byte[]{-117, 1, -128, 77, 101, 111, 119, 3};

    @BeforeAll
    static void load() {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    public void decompress() throws IOException {
        BrotliDecoderChannel channel = new BrotliDecoderChannel(new EntireBufferByteChannel(ByteBuffer.wrap(compressedData)));
        ByteBuffer output = ByteBuffer.allocate(2048);
        channel.read(output);
        output.flip();
        String result = StandardCharsets.UTF_8.decode(output).toString();
        assertEquals("Meow", result);
    }

    @Test
    public void decompressLonger() throws IOException {
        String data = "In ancient times cats were worshipped as gods; they have not forgotten this.";
        ByteBuffer compressed;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BrotliOutputStream output = new BrotliOutputStream(baos)) {
            output.write(data.getBytes(StandardCharsets.UTF_8));
            output.close();
            compressed = ByteBuffer.wrap(baos.toByteArray());
        }

        // Quick verification that it compressed as expected
        assertEquals(59, compressed.remaining());

        BrotliDecoderChannel channel = new BrotliDecoderChannel(new EntireBufferByteChannel(compressed));
        ByteBuffer output = ByteBuffer.allocate(2048);
        channel.read(output);
        output.flip();
        String result = StandardCharsets.UTF_8.decode(output).toString();
        assertEquals(data, result);
    }

    @Test
    public void decompressOneByteAtATime() throws IOException {
        BrotliDecoderChannel channel = new BrotliDecoderChannel(new OneByteAtATimeByteChannel(ByteBuffer.wrap(compressedData)));
        ByteBuffer output = ByteBuffer.allocate(2048);
        channel.read(output);
        output.flip();
        String result = StandardCharsets.UTF_8.decode(output).toString();
        assertEquals("Meow", result);
    }

    @Test
    public void decompressOneByteAtATimeLonger() throws IOException {
        String data = "In ancient times cats were worshipped as gods; they have not forgotten this.";
        ByteBuffer compressed;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BrotliOutputStream output = new BrotliOutputStream(baos)) {
            output.write(data.getBytes(StandardCharsets.UTF_8));
            output.close();
            compressed = ByteBuffer.wrap(baos.toByteArray());
        }

        // Quick verification that it compressed as expected
        assertEquals(59, compressed.remaining());

        BrotliDecoderChannel channel = new BrotliDecoderChannel(new OneByteAtATimeByteChannel(compressed));
        ByteBuffer output = ByteBuffer.allocate(2048);
        channel.read(output);
        output.flip();
        String result = StandardCharsets.UTF_8.decode(output).toString();
        assertEquals(data, result);
    }

    private static class EntireBufferByteChannel implements ReadableByteChannel {
        private final ByteBuffer buffer;

        public EntireBufferByteChannel(ByteBuffer buffer) {
            this.buffer = buffer.slice();
        }

        @Override
        public int read(ByteBuffer dst) {
            if (!buffer.hasRemaining())
                return -1;
            int pos = dst.position();
            dst.put(buffer);
            return dst.position() - pos;
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {
        }
    }

    private static class OneByteAtATimeByteChannel implements ReadableByteChannel {
        private final ByteBuffer buffer;

        public OneByteAtATimeByteChannel(ByteBuffer buffer) {
            this.buffer = buffer.slice();
        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public int read(ByteBuffer dst) {
            if (!buffer.hasRemaining())
                return -1;
            dst.put(buffer.get());
            return 1;
        }
    }
}
