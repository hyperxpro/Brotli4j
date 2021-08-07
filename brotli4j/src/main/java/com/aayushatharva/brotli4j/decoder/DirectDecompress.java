/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020-2021 Aayush Atharva
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

import java.io.IOException;

/**
 * Directly decompresses data using {@link Decoder#decompress(byte[])}
 */
public final class DirectDecompress {
    private final DecoderJNI.Status resultStatus;
    private final byte[] decompressedData;

    DirectDecompress(DecoderJNI.Status resultStatus, byte[] decompressedData) {
        this.resultStatus = resultStatus;
        this.decompressedData = decompressedData;
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
        return decompressedData;
    }
}
