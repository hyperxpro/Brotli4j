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
package com.aayushatharva.brotli4j.common;

/**
 * Utilities class for Brotli4j
 */
public final class Utils {

    /**
     * Returns the maximum compressed size for the given input size.
     * <p></p>
     * This method is based on the original implementation of the Brotli library and works only
     * for direct compression, not stream compression. This is useful to allocating buffers for compressed data.
     *
     * @param input_size The input size.
     * @return The maximum compressed size.
     * @throws IllegalArgumentException If the input size is negative.
     */
    public static int maxCompressedSize(int input_size) {
        if (input_size < 0) {
            throw new IllegalArgumentException("Input size cannot be negative");
        }

        /* [window bits / empty metadata] + N * [uncompressed] + [last empty] */
        int num_large_blocks = input_size >> 14;
        int overhead = 2 + 4 * num_large_blocks + 3 + 1;
        int result = input_size + overhead;
        if (input_size == 0) {
            return 2;
        }
        return result < input_size ? 0 : result;
    }

    private Utils() {
        // Prevent outside initialization
    }
}
