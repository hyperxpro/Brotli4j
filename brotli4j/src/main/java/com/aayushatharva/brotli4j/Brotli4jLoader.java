/*
 * This file is part of Brotli4j.
 * Copyright (c) 2020 Aayush Atharva
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
package com.aayushatharva.brotli4j;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Loads Brotli Native Library
 */
public class Brotli4jLoader {

    private static final Throwable UNAVAILABILITY_CAUSE;

    static {
        Throwable cause = null;
        try {
            System.loadLibrary("brotli");
        } catch (Throwable t) {
            try {
                String nativeLibName = System.mapLibraryName("brotli");
                String libPath = "/lib/" + getPlatform() + "/" + nativeLibName;

                System.out.println(libPath);

                File tempDir = new File(System.getProperty("java.io.tmpdir"), "com_aayushatharva_brotli4j_" + System.nanoTime());
                tempDir.mkdir();
                tempDir.deleteOnExit();

                File tempFile = new File(tempDir, nativeLibName);

                try (InputStream in = Brotli4jLoader.class.getResourceAsStream(libPath)) {
                    Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable throwable) {
                    tempFile.delete();
                    throw throwable;
                }

                System.load(tempFile.getAbsolutePath());

                cause = null;
            } catch (Throwable throwable) {
                cause = throwable;
            }
        }

        UNAVAILABILITY_CAUSE = cause;
    }

    /**
     * Returns {@code true} if the Brotli native library is available else {@code false}.
     */
    public static boolean isAvailable() {
        return UNAVAILABILITY_CAUSE == null;
    }

    /**
     * Ensure Brotli native library is available.
     *
     * @throws Throwable {@link UnsatisfiedLinkError} If unavailable.
     */
    public static void ensureAvailability() throws Throwable {
        if (UNAVAILABILITY_CAUSE != null) {
            throw new UnsatisfiedLinkError("Failed to load Brotli native library").initCause(UNAVAILABILITY_CAUSE);
        }
    }

    public static Throwable getUnavailabilityCause() {
        return UNAVAILABILITY_CAUSE;
    }

    private static String getPlatform() {
        String osName = System.getProperty("os.name");
        String archName = System.getProperty("os.arch");
        if (osName.equalsIgnoreCase("Linux")) {
            if (archName.equalsIgnoreCase("amd64")) {
                return "linux_x86-64";
            }
        } else if (osName.startsWith("Windows")) {
            if (archName.equalsIgnoreCase("amd64")) {
                return "windows_x86-64";
            }
        } else if (osName.startsWith("Mac")) {
            if (archName.equalsIgnoreCase("x86_64")) {
                return "macos_x86-64";
            }
        }
        throw new UnsupportedOperationException("Unsupported OS and Architecture: " + osName + ", " + archName);
    }
}
