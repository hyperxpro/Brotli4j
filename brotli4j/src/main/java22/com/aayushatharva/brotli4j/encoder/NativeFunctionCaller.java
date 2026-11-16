/*
 *   Copyright (c) 2020-2026, Aayush Atharva
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
package com.aayushatharva.brotli4j.encoder;

import java.nio.ByteBuffer;

/**
 * Native calls of {@link EncoderJNI} (JDK 22+), implemented with FFM or JNI. One instance per
 * {@link EncoderJNI.Wrapper}: the FFM implementation keeps the per-encoder state. The dictionary methods use
 * no per-encoder state.
 */
interface NativeFunctionCaller {
    ByteBuffer nativeCreate(long[] context);

    void nativePush(long[] context, int length);

    ByteBuffer nativePull(long[] context);

    void nativeDestroy(long[] context);

    boolean nativeAttachDictionary(long[] context, ByteBuffer dictionary);

    ByteBuffer nativePrepareDictionary(ByteBuffer dictionary, long type);

    void nativeDestroyDictionary(ByteBuffer dictionary);
}
