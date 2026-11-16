/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.common;

import com.aayushatharva.brotli4j.internal.NativeBackend;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM implementation (JDK 22+), a line-by-line port of {@code common_jni.cc}.
 */
final class FfmNativeFunctionCaller implements NativeFunctionCaller {

    private static final String SET_DICTIONARY_DATA = "BrotliSetDictionaryData";
    private static final String GET_DICTIONARY = "BrotliGetDictionary";

    private static final String[] SYMBOLS = {SET_DICTIONARY_DATA, GET_DICTIONARY};
    // Benign race: at worst two threads both look the symbols up.
    private static boolean linked;

    /**
     * Keeps the buffer reachable once brotli uses it as dictionary data
     * (the JNI version keeps a global reference: an intended memory leak).
     */
    @SuppressWarnings("unused")
    private static ByteBuffer dictionaryData;

    @Override
    public boolean nativeSetDictionaryData(ByteBuffer buffer) {
        if (!linked) {
            NativeBackend.ensureLinked(
                    "'boolean com.aayushatharva.brotli4j.common.CommonJNI.nativeSetDictionaryData(java.nio.ByteBuffer)'",
                    SYMBOLS);
            linked = true;
        }
        // NewGlobalRef(null) and GetDirectBufferAddress(heap buffer) both yield NULL.
        if (buffer == null || !buffer.isDirect()) {
            return false;
        }
        // Passed to the downcall as a segment: FFM keeps it, and so the buffer, alive during the call.
        MemorySegment data = MemorySegment.ofBuffer(buffer.duplicate().clear());
        if (data.address() == 0) {
            return false;
        }
        try {
            Bindings.SET_DICTIONARY_DATA.invokeExact(data);
            MemorySegment dictionary = (MemorySegment) Bindings.GET_DICTIONARY.invokeExact();
            if (dictionary.get(ADDRESS, Bindings.DICTIONARY_DATA_OFFSET).address() == data.address()) {
                dictionaryData = buffer;
            }
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        return true;
    }

    /**
     * Downcall handles and layouts, initialized on first use after {@link NativeBackend#ensureLinked} succeeded.
     */
    private static final class Bindings {
        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

        /** {@code BrotliDictionary} from {@code common/dictionary.h}; only {@code data} is read. */
        static final MemoryLayout DICTIONARY = MemoryLayout.structLayout(
                MemoryLayout.sequenceLayout(32, JAVA_BYTE).withName("size_bits_by_length"),
                MemoryLayout.sequenceLayout(32, JAVA_INT).withName("offsets_by_length"),
                LINKER.canonicalLayouts().get("size_t").withName("data_size"),
                ADDRESS.withName("data"));
        static final long DICTIONARY_DATA_OFFSET =
                DICTIONARY.byteOffset(MemoryLayout.PathElement.groupElement("data"));

        static final MethodHandle SET_DICTIONARY_DATA = downcall(FfmNativeFunctionCaller.SET_DICTIONARY_DATA,
                FunctionDescriptor.ofVoid(ADDRESS));
        // The target layout sizes the returned segment, so it can be read without reinterpret().
        static final MethodHandle GET_DICTIONARY = downcall(FfmNativeFunctionCaller.GET_DICTIONARY,
                FunctionDescriptor.of(ADDRESS.withTargetLayout(DICTIONARY)));

        private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(LOOKUP.find(name).orElseThrow(), descriptor);
        }
    }
}
