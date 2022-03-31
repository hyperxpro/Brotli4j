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
import io.netty.buffer.PooledByteBufAllocator;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Multiple decoding methods using Netty Buffer.
 * Make sure to add it as dependency before using this class
 *
 * @see <a href="https://search.maven.org/artifact/io.netty/netty-buffer/">Netty Buffer</a>
 */
@Local
public final class Decoders {

    /**
     * Decodes the given data buffer.
     *
     * @param compressed   {@link ByteBuffer} source
     * @param decompressed {@link ByteBuffer} destination
     * @return {@link DirectDecompress} instance
     * @throws IOException Thrown in case of error during encoding
     */
    @Local
    public static DirectDecompress decompress(ByteBuffer compressed, ByteBuffer decompressed) throws IOException {
        ByteBuf src = PooledByteBufAllocator.DEFAULT.directBuffer();
        ByteBuf dst = PooledByteBufAllocator.DEFAULT.buffer();

        try {
            src.writeBytes(compressed);
            dst.writeBytes(decompressed);
            return decompress(src, dst);
        } finally {
            src.release();
            dst.release();
        }
    }

    /**
     * Decodes the given data buffer.
     *
     * @param compressed   {@link ByteBuf} source
     * @param decompressed {@link ByteBuf} destination
     * @return {@link DirectDecompress} instance
     * @throws IOException Thrown in case of error during encoding
     */
    @Local
    public static DirectDecompress decompress(ByteBuf compressed, ByteBuf decompressed) throws IOException {
        int compressedBytes = compressed.readableBytes();
        DecoderJNI.Wrapper decoder = new DecoderJNI.Wrapper(compressedBytes);
        try {
            decoder.getInputBuffer().put(compressed.nioBuffer());
            decoder.push(compressedBytes);
            while (decoder.getStatus() != DecoderJNI.Status.DONE) {
                switch (decoder.getStatus()) {
                    case OK:
                        decoder.push(0);
                        break;

                    case NEEDS_MORE_OUTPUT:
                        ByteBuffer buffer = decoder.pull();
                        decompressed.writeBytes(buffer);
                        break;

                    case NEEDS_MORE_INPUT:
                        // Give decoder a chance to process the remaining of the buffered byte.
                        decoder.push(0);
                        // If decoder still needs input, this means that stream is truncated.
                        if (decoder.getStatus() == DecoderJNI.Status.NEEDS_MORE_INPUT) {
                            return new DirectDecompress(decoder.getStatus(), null, null);
                        }
                        break;

                    default:
                        return new DirectDecompress(decoder.getStatus(), null, null);
                }
            }
        } finally {
            decoder.destroy();
        }
        return new DirectDecompress(decoder.getStatus(), null, decompressed);
    }
}
