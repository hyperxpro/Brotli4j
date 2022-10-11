/*
 *    Copyright (c) 2020-2022, Aayush Atharva
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
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.common.annotations.Local;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledDirectByteBuf;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Multiple encoding methods using Netty Buffer
 * Make sure to add it as dependency before using this class
 *
 * @see <a href="https://search.maven.org/artifact/io.netty/netty-buffer/">Netty Buffer</a>
 */
@Local
public final class Encoders {

    /**
     * Encodes the given {@link ByteBuf}
     *
     * @param src    {@link ByteBuf} source
     * @param pooled If set to {@code true} then this method will return
     *               {@code PooledDirectByteBuf} else {@link UnpooledDirectByteBuf}
     * @return If {@code pooled} is set to {@code true} then this method will return
     * {@code PooledDirectByteBuf} else {@link UnpooledDirectByteBuf}
     * @throws IOException Thrown in case of error during encoding
     */
    public static ByteBuf compress(ByteBuf src, boolean pooled) throws IOException {
        ByteBuf dst;
        if (pooled) {
            dst = PooledByteBufAllocator.DEFAULT.directBuffer();
        } else {
            dst = Unpooled.directBuffer();
        }
        compress(src, dst);
        return dst;
    }

    /**
     * Encodes the given {@link ByteBuf}
     *
     * @param src {@link ByteBuf} source
     * @param dst {@link ByteBuf} destination
     * @throws IOException Thrown in case of error during encoding
     */
    public static void compress(ByteBuf src, ByteBuf dst) throws IOException {
        compress(src, dst, Encoder.Parameters.DEFAULT);
    }

    /**
     * Encodes the given {@link ByteBuffer}
     *
     * @param src {@link ByteBuffer} source
     * @param dst {@link ByteBuffer} destination
     * @throws IOException Thrown in case of error during encoding
     */
    public static void compress(ByteBuffer src, ByteBuffer dst) throws IOException {
        ByteBuf srcBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
        ByteBuf dstBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
        try {
            srcBuf.writeBytes(src);
            compress(srcBuf, dstBuf, Encoder.Parameters.DEFAULT);
        } finally {
            dst.put(dstBuf.nioBuffer());
            srcBuf.release();
            dstBuf.release();
        }
    }

    /**
     * Encodes the given {@link ByteBuffer}
     *
     * @param src    {@link ByteBuffer} source
     * @param dst    {@link ByteBuffer} destination
     * @param params {@link Encoder.Parameters} instance
     * @throws IOException Thrown in case of error during encoding
     */
    public static void compress(ByteBuffer src, ByteBuffer dst, Encoder.Parameters params) throws IOException {
        ByteBuf srcBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
        ByteBuf dstBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
        try {
            srcBuf.writeBytes(src);
            compress(srcBuf, dstBuf, params);
        } finally {
            dst.put(dstBuf.nioBuffer());
            srcBuf.release();
            dstBuf.release();
        }
    }

    /**
     * Encodes the given {@link ByteBuf}
     *
     * @param src    {@link ByteBuffer} source
     * @param dst    {@link ByteBuffer} destination
     * @param params {@link Encoder.Parameters} instance
     * @throws IOException Thrown in case of error during encoding
     */
    public static void compress(ByteBuf src, ByteBuf dst, Encoder.Parameters params) throws IOException {
        int readableBytes = src.readableBytes();
        if (readableBytes == 0) {
            dst.writeByte((byte) 6);
            return;
        }

        EncoderJNI.Wrapper encoder = new EncoderJNI.Wrapper(readableBytes, params.quality(), params.lgwin(), params.mode());
        try {
            encoder.getInputBuffer().put(src.nioBuffer());
            encoder.push(EncoderJNI.Operation.PROCESS, readableBytes);
            while (true) {
                if (!encoder.isSuccess()) {
                    throw new IOException("encoding failed");
                } else if (encoder.hasMoreOutput()) {
                    ByteBuffer buffer = encoder.pull();
                    dst.writeBytes(buffer);
                } else if (!encoder.isFinished()) {
                    encoder.push(EncoderJNI.Operation.FINISH, 0);
                } else {
                    break;
                }
            }
        } finally {
            encoder.destroy();
        }
    }
}
