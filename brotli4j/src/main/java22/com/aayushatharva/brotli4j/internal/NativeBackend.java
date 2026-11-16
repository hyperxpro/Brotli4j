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

import java.lang.foreign.Linker;
import java.lang.foreign.SymbolLookup;
import java.util.function.Supplier;

/**
 * Picks how native code is called (JDK 22+), from {@code brotli4j.backend}: {@code ffm}, {@code jni},
 * or unset for FFM with a fallback to JNI when this JVM has no FFM linker. Also holds what the FFM
 * implementations of all packages share.
 * <p>
 * Must keep the same public API as the base version, as a multi-release JAR requires.
 */
@Internal
public final class NativeBackend {

    /**
     * Largest size {@link java.lang.foreign.MemorySegment#asByteBuffer()} accepts (the JDK's
     * {@code SOFT_MAX_ARRAY_LENGTH}); {@code NewDirectByteBuffer} accepts up to {@link Integer#MAX_VALUE}.
     */
    public static final long MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    private static final String BACKEND = "brotli4j.backend";

    private NativeBackend() {
    }

    /**
     * Fails like an unlinked JNI native while the library isn't loaded yet, so the call can be retried after
     * loading. Must run before a class looks the symbols up in a static initializer: that failure is permanent.
     *
     * @param unlinkedMethod the JNI native, as HotSpot names it in {@link UnsatisfiedLinkError}
     * @param symbols        the C functions the caller downcalls
     * @throws UnsatisfiedLinkError if a symbol isn't in the libraries loaded by this class loader
     */
    public static void ensureLinked(String unlinkedMethod, String... symbols) {
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        for (String symbol : symbols) {
            if (lookup.find(symbol).isEmpty()) {
                throw new UnsatisfiedLinkError(unlinkedMethod);
            }
        }
    }

    /**
     * Only reads the property and probes the linker: the native library may not be loaded yet.
     *
     * @param ffm creates the FFM implementation
     * @param jni creates the JNI implementation
     * @param <T> the implemented interface
     * @return the selected implementation
     * @throws IllegalArgumentException      if {@code brotli4j.backend} is neither {@code ffm} nor {@code jni}
     * @throws UnsupportedOperationException if {@code ffm} is requested but this JVM has no FFM linker
     */
    public static <T> T select(Supplier<? extends T> ffm, Supplier<? extends T> jni) {
        String backend = System.getProperty(BACKEND);
        if (backend == null) {
            return hasLinker() ? ffm.get() : jni.get();
        }
        return switch (backend) {
            case "ffm" -> {
                if (!hasLinker()) {
                    throw new UnsupportedOperationException(
                            "-D" + BACKEND + "=ffm, but this JVM has no FFM linker; use -D" + BACKEND + "=jni");
                }
                yield ffm.get();
            }
            case "jni" -> jni.get();
            default -> throw new IllegalArgumentException(
                    "Invalid -D" + BACKEND + "=" + backend + "; expected 'ffm' or 'jni'");
        };
    }

    private static boolean hasLinker() {
        try {
            Linker.nativeLinker();
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}
