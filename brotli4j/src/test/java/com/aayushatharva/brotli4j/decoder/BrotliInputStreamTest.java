/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020 Aayush Atharva
 *
 * Brotli4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Brotli4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Brotli4j.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BrotliInputStreamTest {

    private static final byte[] compressedData = new byte[]{-117, 1, -128, 77, 101, 111, 119, 3};

    @BeforeAll
    static void load() throws Throwable {
        Brotli4jLoader.ensureAvailability();
    }

    @Test
    public void decompress() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        BrotliInputStream brotliInputStream = new BrotliInputStream(bais);

        int read = brotliInputStream.read();
        while(read > -1) { // -1 means EOF
            baos.write(read);
            read = brotliInputStream.read();
        }

        baos.close();
        bais.close();
        brotliInputStream.close();

        assertEquals("Meow", new String(baos.toByteArray()));
    }
}
