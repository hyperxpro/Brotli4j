/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.decoder;

import com.aayushatharva.brotli4j.internal.NativeBackend;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM implementation (JDK 22+), a line-by-line port of {@code decoder_jni.cc}.
 * One instance per {@link DecoderJNI.Wrapper}: its fields are the C {@code DecoderHandle}.
 */
final class FfmNativeFunctionCaller implements NativeFunctionCaller {

    private static final String CREATE_INSTANCE = "BrotliDecoderCreateInstance";
    private static final String DESTROY_INSTANCE = "BrotliDecoderDestroyInstance";
    private static final String DECOMPRESS_STREAM = "BrotliDecoderDecompressStream";
    private static final String HAS_MORE_OUTPUT = "BrotliDecoderHasMoreOutput";
    private static final String TAKE_OUTPUT = "BrotliDecoderTakeOutput";
    private static final String IS_FINISHED = "BrotliDecoderIsFinished";
    private static final String ATTACH_DICTIONARY = "BrotliDecoderAttachDictionary";

    // BrotliDecoderResult (brotli/decode.h)
    private static final int RESULT_SUCCESS = 1;
    private static final int RESULT_NEEDS_MORE_INPUT = 2;
    private static final int RESULT_NEEDS_MORE_OUTPUT = 3;
    // BrotliSharedDictionaryType (brotli/shared_dictionary.h)
    private static final int SHARED_DICTIONARY_RAW = 0;

    private static final int MAX_DICTIONARIES = 15;

    private static final String[] SYMBOLS = {CREATE_INSTANCE, DESTROY_INSTANCE, DECOMPRESS_STREAM, HAS_MORE_OUTPUT,
            TAKE_OUTPUT, IS_FINISHED, ATTACH_DICTIONARY};
    // Benign race: at worst two threads both look the symbols up.
    private static boolean linked;

    // DecoderHandle. Explicit native ownership permits sequential thread handoff.
    /** One malloc'ed block: the out-parameter cells (C keeps them on its stack), then the input buffer. */
    private MemorySegment nativeMemory = MemorySegment.NULL;
    private MemorySegment state = MemorySegment.NULL;
    private long inputOffset;
    private long inputLength;
    /**
     * JNI global references: keep attached dictionaries alive until {@link #nativeDestroy}. Created on the first
     * attach; most instances never attach one.
     */
    private List<ByteBuffer> dictionaryRefs;

    @Override
    public ByteBuffer nativeCreate(long[] context) {
        // Only here: the other methods run on a created decoder.
        if (!linked) {
            NativeBackend.ensureLinked(
                    "'java.nio.ByteBuffer com.aayushatharva.brotli4j.decoder.DecoderJNI.nativeCreate(long[])'", SYMBOLS);
            linked = true;
        }
        long inputSize = Bindings.toSizeT(context[1]);
        context[0] = 0;
        context[2] = 0;
        // Java only: NewDirectByteBuffer accepts sizes above MAX_BUFFER_SIZE, asByteBuffer() below doesn't.
        if (inputSize == 0 || Long.compareUnsigned(inputSize, NativeBackend.MAX_BUFFER_SIZE) > 0) {
            return null;
        }
        // Cannot overflow size_t: inputSize is at most MAX_BUFFER_SIZE.
        long allocationSize = Bindings.CELLS.byteSize() + inputSize;
        boolean created = false;
        try {
            MemorySegment allocation = (MemorySegment) Bindings.MALLOC.invokeExact(allocationSize);
            if (allocation.address() == 0) {
                return null;
            }
            // malloc aligns the cells; input bytes need no additional alignment or zeroing. A local, not the field:
            // the JIT then removes this segment; if reinterpret fails, nativeMemory is still NULL, so free it here.
            try {
                nativeMemory = allocation.reinterpret(allocationSize);
            } catch (RuntimeException | Error failure) {
                Bindings.FREE.invokeExact(allocation);
                throw failure;
            }
            state = (MemorySegment) Bindings.CREATE_INSTANCE.invokeExact(
                    MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL);
            if (state.address() == 0) {
                return null;
            }
            ByteBuffer inputBuffer = nativeMemory.asSlice(Bindings.CELLS.byteSize(), inputSize).asByteBuffer();
            context[0] = state.address();
            created = true;
            return inputBuffer;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            if (!created) {
                release();
            }
        }
    }

    @Override
    public void nativePush(long[] context, int length) {
        context[1] = 0;  /* ERROR */
        context[2] = 0;

        if (length != 0) {
            /* Still have unconsumed data. Workflow is broken. */
            if (inputOffset < inputLength) {
                return;
            }
            inputOffset = 0;
            inputLength = Bindings.toSizeT(length);
        }

        /* Actual decompression. */
        nativeMemory.set(ADDRESS, Bindings.IN_OFFSET, nativeMemory.asSlice(Bindings.CELLS.byteSize() + inputOffset));
        Bindings.setSize(nativeMemory, Bindings.IN_SIZE_OFFSET, inputLength - inputOffset);
        Bindings.setSize(nativeMemory, Bindings.OUT_SIZE_OFFSET, 0);
        int status;
        try {
            status = (int) Bindings.DECOMPRESS_STREAM.invokeExact(
                    state, cell(Bindings.IN_SIZE_OFFSET), cell(Bindings.IN_OFFSET),
                    cell(Bindings.OUT_SIZE_OFFSET), MemorySegment.NULL, MemorySegment.NULL);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        inputOffset = inputLength - Bindings.getSize(nativeMemory, Bindings.IN_SIZE_OFFSET);
        switch (status) {
            case RESULT_SUCCESS:
                /* Bytes after stream end are not allowed. */
                context[1] = (inputOffset == inputLength) ? 1 : 0;
                break;

            case RESULT_NEEDS_MORE_INPUT:
                context[1] = 2;
                break;

            case RESULT_NEEDS_MORE_OUTPUT:
                context[1] = 3;
                break;

            default:
                context[1] = 0;
                break;
        }
        context[2] = hasMoreOutput() ? 1 : 0;
    }

    /**
     * Pull decompressed data from decoder, optionally capped to maxBytes (0: all available output).
     */
    private ByteBuffer pullImpl(long[] context, long maxBytes) {
        Bindings.setSize(nativeMemory, Bindings.DATA_LENGTH_OFFSET, maxBytes);
        MemorySegment data;
        try {
            data = (MemorySegment) Bindings.TAKE_OUTPUT.invokeExact(state, cell(Bindings.DATA_LENGTH_OFFSET));
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        boolean hasMoreOutput = hasMoreOutput();
        if (hasMoreOutput) {
            context[1] = 3;
        } else if (isFinished()) {
            /* Bytes after stream end are not allowed. */
            context[1] = (inputOffset == inputLength) ? 1 : 0;
        } else {
            /* Can proceed, or more data is required? */
            context[1] = (inputOffset == inputLength) ? 2 : 4;
        }
        context[2] = hasMoreOutput ? 1 : 0;
        // NewDirectByteBuffer: big-endian view of brotli's output, valid until the next call.
        // Brotli owns the output. Like JNI, this view becomes invalid after destroy.
        return data.reinterpret(Bindings.getSize(nativeMemory, Bindings.DATA_LENGTH_OFFSET)).asByteBuffer();
    }

    @Override
    public ByteBuffer nativePull(long[] context) {
        return pullImpl(context, 0);
    }

    @Override
    public ByteBuffer nativePullBounded(long[] context, int maxBytes) {
        return pullImpl(context, Math.max(maxBytes, 0));
    }

    @Override
    public void nativeDestroy(long[] context) {
        release();
    }

    /** Release only memory owned by this caller, including partially constructed instances. */
    private void release() {
        try {
            if (state.address() != 0) {
                Bindings.DESTROY_INSTANCE.invokeExact(state);
                state = MemorySegment.NULL;
            }
        } catch (Throwable t) {
            throw new AssertionError(t);
        } finally {
            // Keep RAW dictionary storage reachable until DestroyInstance has completed.
            dictionaryRefs = null;
            try {
                if (nativeMemory.address() != 0) {
                    Bindings.FREE.invokeExact(nativeMemory);
                    nativeMemory = MemorySegment.NULL;
                }
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    @Override
    public boolean nativeAttachDictionary(long[] context, ByteBuffer dictionary) {
        MemorySegment address = null;
        long capacity = 0;

        boolean ok = true;
        if (dictionary == null) {
            ok = false;
        }
        if (ok && dictionaryRefs != null && dictionaryRefs.size() >= MAX_DICTIONARIES) {
            ok = false;
        }
        // NewGlobalRef of a non-null object only fails when the JVM is out of memory.
        if (ok) {
            if (dictionaryRefs == null) {
                dictionaryRefs = new ArrayList<>(MAX_DICTIONARIES);
            }
            dictionaryRefs.add(dictionary);
            // GetDirectBufferAddress: NULL for a heap buffer.
            if (dictionary.isDirect()) {
                address = MemorySegment.ofBuffer(dictionary.duplicate().clear());
            }
            ok = address != null && address.address() != 0;
        }
        if (ok) {
            capacity = dictionary.capacity();
            ok = (capacity > 0) && (capacity < (1 << 30));
        }
        if (ok) {
            long size = Bindings.toSizeT(capacity);
            try {
                ok = (int) Bindings.ATTACH_DICTIONARY.invokeExact(state, SHARED_DICTIONARY_RAW, size, address) != 0;
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }

        return ok;
    }

    /**
     * A per-call slice of a cell in {@link #nativeMemory}, for a pointer argument. Cells are as wide as
     * {@code size_t} and pointers. The JIT (C2) removes the allocation; storing slices would cost ~200 B per wrapper.
     */
    private MemorySegment cell(long offset) {
        return nativeMemory.asSlice(offset, Bindings.SIZE_T.byteSize());
    }

    private boolean hasMoreOutput() {
        try {
            return (int) Bindings.HAS_MORE_OUTPUT.invokeExact(state) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    private boolean isFinished() {
        try {
            return (int) Bindings.IS_FINISHED.invokeExact(state) != 0;
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    /**
     * Downcall handles and layouts, initialized on first use after {@link NativeBackend#ensureLinked} succeeded.
     */
    private static final class Bindings {
        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();
        private static final Linker.Option CRITICAL = Linker.Option.critical(false);

        /** {@code size_t}: 8 bytes on 64-bit, 4 on 32-bit. */
        static final ValueLayout SIZE_T = sizeT();

        // Allocate and free through the C runtime; Brotli owns its state and output separately.
        private static final SymbolLookup C_RUNTIME = LINKER.defaultLookup();
        static final MethodHandle MALLOC = sizeTAsLong(LINKER.downcallHandle(
                C_RUNTIME.find("malloc").orElseThrow(), FunctionDescriptor.of(ADDRESS, SIZE_T)), 0);
        static final MethodHandle FREE = LINKER.downcallHandle(
                C_RUNTIME.find("free").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));

        /** The out-parameters; {@code size_t} and pointers have the same size, so there is no padding. */
        static final MemoryLayout CELLS = MemoryLayout.structLayout(
                SIZE_T.withName("in_size"),
                ADDRESS.withName("in"),
                SIZE_T.withName("out_size"),
                SIZE_T.withName("data_length"));
        static final long IN_SIZE_OFFSET = offset("in_size");
        static final long IN_OFFSET = offset("in");
        static final long OUT_SIZE_OFFSET = offset("out_size");
        static final long DATA_LENGTH_OFFSET = offset("data_length");

        /** {@code BrotliDecoderState* (brotli_alloc_func, brotli_free_func, void* opaque)} */
        static final MethodHandle CREATE_INSTANCE = downcall(FfmNativeFunctionCaller.CREATE_INSTANCE,
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        /** {@code void (BrotliDecoderState*)} */
        static final MethodHandle DESTROY_INSTANCE = downcall(FfmNativeFunctionCaller.DESTROY_INSTANCE,
                FunctionDescriptor.ofVoid(ADDRESS));
        /** {@code BrotliDecoderResult (state, size_t* available_in, const uint8_t** next_in,
         * size_t* available_out, uint8_t** next_out, size_t* total_out)}; long-running, so not critical. */
        static final MethodHandle DECOMPRESS_STREAM = downcall(FfmNativeFunctionCaller.DECOMPRESS_STREAM,
                FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
        /** {@code BROTLI_BOOL (const BrotliDecoderState*)} */
        static final MethodHandle HAS_MORE_OUTPUT = downcall(FfmNativeFunctionCaller.HAS_MORE_OUTPUT,
                FunctionDescriptor.of(JAVA_INT, ADDRESS), CRITICAL);
        /** {@code const uint8_t* (BrotliDecoderState*, size_t* size)} */
        static final MethodHandle TAKE_OUTPUT = downcall(FfmNativeFunctionCaller.TAKE_OUTPUT,
                FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS), CRITICAL);
        /** {@code BROTLI_BOOL (const BrotliDecoderState*)} */
        static final MethodHandle IS_FINISHED = downcall(FfmNativeFunctionCaller.IS_FINISHED,
                FunctionDescriptor.of(JAVA_INT, ADDRESS), CRITICAL);
        /** {@code BROTLI_BOOL (state, BrotliSharedDictionaryType, size_t data_size, const uint8_t* data)};
         * {@code data_size} is retyped to {@code long} on 32-bit. */
        static final MethodHandle ATTACH_DICTIONARY = sizeTAsLong(downcall(FfmNativeFunctionCaller.ATTACH_DICTIONARY,
                FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, SIZE_T, ADDRESS)), 2);

        /** {@code (size_t) value}: keeps the low 32 bits on 32-bit platforms, like the C cast. */
        static long toSizeT(long value) {
            return SIZE_T.byteSize() == Integer.BYTES ? value & 0xFFFFFFFFL : value;
        }

        static long getSize(MemorySegment segment, long offset) {
            return SIZE_T instanceof ValueLayout.OfLong layout
                    ? segment.get(layout, offset)
                    : Integer.toUnsignedLong(segment.get((ValueLayout.OfInt) SIZE_T, offset));
        }

        static void setSize(MemorySegment segment, long offset, long value) {
            if (SIZE_T instanceof ValueLayout.OfLong layout) {
                segment.set(layout, offset, value);
            } else {
                segment.set((ValueLayout.OfInt) SIZE_T, offset, (int) value);
            }
        }

        /** The helpers below handle only a {@code long} or {@code int} {@code size_t}. */
        private static ValueLayout sizeT() {
            MemoryLayout sizeT = LINKER.canonicalLayouts().get("size_t");
            if (sizeT instanceof ValueLayout.OfLong || sizeT instanceof ValueLayout.OfInt) {
                return (ValueLayout) sizeT;
            }
            throw new UnsupportedOperationException("Unexpected size_t layout " + sizeT);
        }

        private static long offset(String name) {
            return CELLS.byteOffset(MemoryLayout.PathElement.groupElement(name));
        }

        private static MethodHandle downcall(String name, FunctionDescriptor descriptor, Linker.Option... options) {
            return LINKER.downcallHandle(LOOKUP.find(name).orElseThrow(), descriptor, options);
        }

        /** Lets callers pass a {@code long} for a {@code size_t} parameter; a no-op on 64-bit. */
        private static MethodHandle sizeTAsLong(MethodHandle handle, int parameter) {
            return MethodHandles.explicitCastArguments(handle, handle.type().changeParameterType(parameter, long.class));
        }
    }
}
