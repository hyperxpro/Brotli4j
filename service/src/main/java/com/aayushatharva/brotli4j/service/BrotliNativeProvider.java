/*
 *    Copyright (c) 2020-2025, Aayush Atharva
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
package com.aayushatharva.brotli4j.service;

/**
 * A service interface that signals that an implementor jar contains a native lib.
 */
public interface BrotliNativeProvider {

    /**
     * Gives the name of the platform that this provider contains a native brotli lib for
     *
     * @return The name of the native, e.g. linux-x86_64 or osx-aarch64
     */
    String platformName();
}
