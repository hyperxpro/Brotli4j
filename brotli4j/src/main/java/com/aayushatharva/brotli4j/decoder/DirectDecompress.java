/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020-2022 Aayush Atharva
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

import com.aayushatharva.brotli4j.common.annotations.Local;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Directly decompresses data using {@link Decoder#decompress(byte[])}
 */
@Local
public final class DirectDecompress {
    private final DecoderJNI.Status resultStatus;
    private byte[] decompressedData;
    private ByteBuffer byteBuffer;

    DirectDecompress(DecoderJNI.Status resultStatus, byte[] decompressedData, ByteBuffer byteBuffer) {
        this.resultStatus = resultStatus;
        this.decompressedData = decompressedData;
        this.byteBuffer = byteBuffer;
    }

    /**
     * Initiate direct decompression of data
     *
     * @param compressedData Compressed data as Byte Array
     * @return {@link DirectDecompress} Instance
     * @throws IOException In case of some error during decompression
     */
    public static DirectDecompress decompress(byte[] compressedData) throws IOException {
        return Decoder.decompress(compressedData);
    }

    /**
     * Get the result of decompression.
     *
     * @return {@link DecoderJNI.Status}
     */
    public DecoderJNI.Status getResultStatus() {
        return resultStatus;
    }

    /**
     * Get decompressed data.
     *
     * @return {@code byte} array if decompression was successful else {@code null}
     */
    public byte[] getDecompressedData() {
        // If byte array is null but bytebuffer is not null
        // then convert bytebuffer to byte array and return.
        if (decompressedData == null && byteBuffer != null) {
            byte[] data = new byte[byteBuffer.remaining()];
            byteBuffer.get(data);
            this.decompressedData = data;
        }
        return decompressedData;
    }

    /**
     * Get decompressed data.
     *
     * @return {@link ByteBuffer} if decompression was successful else {@code null}
     */
    public ByteBuffer getDecompressedDataByteBuffer() {
        if (byteBuffer == null && decompressedData != null) {
            this.byteBuffer = ByteBuffer.wrap(decompressedData);
        }
        return byteBuffer;
    }
}
