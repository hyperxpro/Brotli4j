/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.common;

import com.aayushatharva.brotli4j.common.annotations.Upstream;
import com.aayushatharva.brotli4j.internal.NativeBackend;

import java.nio.ByteBuffer;

/**
 * Brotli common wrapper for JDK 22+, calling native code through FFM or JNI as picked by {@link NativeBackend}.
 */
@Upstream
class CommonJNI {
    private static final NativeFunctionCaller CALLER =
            NativeBackend.select(FfmNativeFunctionCaller::new, JniNativeFunctionCaller::new);

    static boolean setDictionaryData(ByteBuffer data) {
        return CALLER.nativeSetDictionaryData(data);
    }

    // Kept here under its original name so the JNI symbol still links.
    static native boolean nativeSetDictionaryData(ByteBuffer data);

    private static final class JniNativeFunctionCaller implements NativeFunctionCaller {
        @Override
        public boolean nativeSetDictionaryData(ByteBuffer data) {
            return CommonJNI.nativeSetDictionaryData(data);
        }
    }
}
