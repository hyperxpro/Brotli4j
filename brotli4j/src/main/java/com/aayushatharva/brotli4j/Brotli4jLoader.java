/*
 *    Copyright (c) 2020-2024, Aayush Atharva
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
package com.aayushatharva.brotli4j;

import com.aayushatharva.brotli4j.common.annotations.Local;
import com.aayushatharva.brotli4j.service.BrotliNativeProvider;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ServiceLoader;

/**
 * Loads Brotli Native Library
 */
@Local
public class Brotli4jLoader {

    private static final Throwable UNAVAILABILITY_CAUSE;

    static {
        Throwable cause = null;

        String customPath = System.getProperty("brotli4j.library.path");

        if (customPath != null) {
            try {
                System.load(customPath);
            } catch (Throwable throwable) {
                cause = throwable;
            }
        } else {
            try {
                System.loadLibrary("brotli");
            } catch (Throwable _throwable) {
                try {
                    String nativeLibName = System.mapLibraryName("brotli");

                    System.out.println(nativeLibName);

                    Path tempDir = Files.createTempDirectory( "com_aayushatharva_brotli4j_").toAbsolutePath();
                    tempDir.toFile().deleteOnExit();

                    Path tempFile = tempDir.resolve(nativeLibName);

                    System.out.println(tempFile);

                    String platform = null;
                    Class<?> loaderClassToUse = Brotli4jLoader.class; // Use this as a fallback for non-JPMS contexts
                    // In Java9+ with JPMS enabled, we need a class in the jar that contains the file to be able to access its content
                    ServiceLoader<BrotliNativeProvider> nativeProviders = ServiceLoader.load(BrotliNativeProvider.class, Brotli4jLoader.class.getClassLoader());
                    for (BrotliNativeProvider nativeProvider : nativeProviders) {
                        if (nativeProvider.isCurrentPlatform()) {
                            platform = nativeProvider.platformName();
                            loaderClassToUse = nativeProvider.getClass();
                            break;
                        }
                    }

                    System.out.println(platform);

                    if (platform == null) {
                        throw new UnsatisfiedLinkError("Failed to find valid Brotli native library in classpath.");
                    }

                    String libPath = "/lib/" + platform + "/" + nativeLibName;

                    System.out.println(libPath);

                    // Copy the native library to a temporary file and load it
                    try (InputStream in = loaderClassToUse.getResourceAsStream(libPath)) {

                        System.out.println(in);

                        // If the library is not found, throw an exception.
                        if (in == null) {
                            throw new UnsatisfiedLinkError("Failed to open Brotli native library: " + libPath);
                        }

                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

                        System.out.println(tempFile);
                        System.out.println(Files.exists(tempFile));

                        System.load(tempFile.toString());
                    }
                } catch (Throwable throwable) {
                    System.out.println(throwable);

                    cause = throwable;
                }
            }
        }

        UNAVAILABILITY_CAUSE = cause;
    }

    /**
     * @return {@code true} if the Brotli native library is available else {@code false}.
     */
    public static boolean isAvailable() {
        return UNAVAILABILITY_CAUSE == null;
    }

    /**
     * Ensure Brotli native library is available.
     *
     * @throws UnsatisfiedLinkError If unavailable.
     */
    public static void ensureAvailability() {
        if (UNAVAILABILITY_CAUSE != null) {
            UnsatisfiedLinkError error = new UnsatisfiedLinkError("Failed to load Brotli native library");
            error.initCause(UNAVAILABILITY_CAUSE);
            throw error;
        }
    }

    public static Throwable getUnavailabilityCause() {
        return UNAVAILABILITY_CAUSE;
    }
}