/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.common;

import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.nio.ByteBuffer;

/**
 * JNI wrapper for brotli common.
 */
@Upstream
class CommonJNI {
    // Callers go through this method so the JDK 22+ version can route the call to JNI or FFM.
    static boolean setDictionaryData(ByteBuffer data) {
        return nativeSetDictionaryData(data);
    }

    static native boolean nativeSetDictionaryData(ByteBuffer data);
}
