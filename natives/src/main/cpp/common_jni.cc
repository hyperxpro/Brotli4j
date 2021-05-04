/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020-2021 Aayush Atharva
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
/* Copyright 2017 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/

#include <jni.h>

#include "c/common/dictionary.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Set data to be brotli dictionary data.
 *
 * @param buffer direct ByteBuffer
 * @returns false if dictionary data was already set; otherwise true
 */
JNIEXPORT jint JNICALL
Java_com_aayushatharva_brotli4j_common_CommonJNI_nativeSetDictionaryData(
    JNIEnv* env, jobject /*jobj*/, jobject buffer) {
  jobject buffer_ref = env->NewGlobalRef(buffer);
  if (!buffer_ref) {
    return false;
  }
  uint8_t* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(buffer));
  if (!data) {
    env->DeleteGlobalRef(buffer_ref);
    return false;
  }

  BrotliSetDictionaryData(data);

  const BrotliDictionary* dictionary = BrotliGetDictionary();
  if (dictionary->data != data) {
    env->DeleteGlobalRef(buffer_ref);
  } else {
    /* Don't release reference; it is an intended memory leak. */
  }
  return true;
}

#ifdef __cplusplus
}
#endif
