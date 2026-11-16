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
package com.aayushatharva.brotli4j.internal;

import com.aayushatharva.brotli4j.common.annotations.Internal;

import java.util.function.Supplier;

/**
 * Picks how native code is called. Only the JDK 22+ version (META-INF/versions/22) is used: it routes native calls
 * to JNI or FFM. This base version exists because a multi-release JAR requires the same public API in both.
 */
@Internal
public final class NativeBackend {

    /**
     * Only used by the JDK 22+ version.
     */
    public static final long MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private NativeBackend() {
    }

    /**
     * @param unlinkedMethod the JNI native, as HotSpot names it in {@link UnsatisfiedLinkError}
     * @param symbols        the C functions the caller downcalls
     * @throws UnsupportedOperationException always: before JDK 22 native calls go straight to JNI
     */
    public static void ensureLinked(String unlinkedMethod, String... symbols) {
        throw new UnsupportedOperationException(
                "NativeBackend is only used on JDK 22+ to route native calls to JNI or FFM");
    }

    /**
     * @param ffm creates the FFM implementation
     * @param jni creates the JNI implementation
     * @param <T> the implemented interface
     * @return never returns
     * @throws UnsupportedOperationException always: before JDK 22 native calls go straight to JNI
     */
    public static <T> T select(Supplier<? extends T> ffm, Supplier<? extends T> jni) {
        throw new UnsupportedOperationException(
                "NativeBackend is only used on JDK 22+ to route native calls to JNI or FFM");
    }
}
