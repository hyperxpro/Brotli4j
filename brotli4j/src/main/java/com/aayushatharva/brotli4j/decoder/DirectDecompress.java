/*
 *    Copyright (c) 2020-2023, Aayush Atharva
 *
 *    Brotli4j licenses this file to you under the
 *    Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
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
