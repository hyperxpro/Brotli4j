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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.IOException;

/**
 * Directly decompresses data using {@link Decoder#decompress(byte[])}
 */
@Local
public final class DirectDecompress {
    private final DecoderJNI.Status resultStatus;
    private byte[] decompressedData;
    private ByteBuf byteBuf;

    DirectDecompress(DecoderJNI.Status resultStatus, byte[] decompressedData, ByteBuf byteBuf) {
        this.resultStatus = resultStatus;
        this.decompressedData = decompressedData;
        this.byteBuf = byteBuf;
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
     * Initiate direct decompression of data which is of a known length
     *
     * @param compressedData Compressed data as byte array
     * @param decompressedLength Expected length of the data after it has been decompressed
     * @return {@link DirectDecompress} Instance containing the status and result of the decompression attempt
     * @throws IOException In case of some error during decompression
     */
    public static DirectDecompress decompressKnownLength(byte[] compressedData, int decompressedLength) throws IOException {
        return Decoder.decompressKnownLength(compressedData, decompressedLength);
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
        if (decompressedData == null && byteBuf != null) {
            this.decompressedData = ByteBufUtil.getBytes(byteBuf);
        }
        return decompressedData;
    }

    /**
     * Get decompressed data.
     *
     * @return {@link ByteBuf} if decompression was successful else {@code null}
     */
    public ByteBuf getDecompressedDataByteBuf() {
        if (byteBuf == null && decompressedData != null) {
            this.byteBuf = Unpooled.wrappedBuffer(decompressedData);
        }
        return byteBuf;
    }
}
