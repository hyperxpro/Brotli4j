/* Copyright 2018 Google Inc. All Rights Reserved.

   Distributed under MIT license.
   See file LICENSE for detail or copy at https://opensource.org/licenses/MIT
*/
package com.aayushatharva.brotli4j.encoder;

import com.aayushatharva.brotli4j.common.annotations.Upstream;

import java.nio.ByteBuffer;

/**
 * Prepared dictionary data provider.
 */
@Upstream
public interface PreparedDictionary {
    ByteBuffer getData();
}
