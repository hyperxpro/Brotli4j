package com.aayushatharva.brotli4j;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Brotli4jLoader {

    private static final Throwable UNAVAILABILITY_CAUSE;

    static {
        Throwable cause = null;
        try {
            System.loadLibrary("brotli");
        } catch (Throwable t) {
            try {
                String nativeLibName = System.mapLibraryName("brotli");
                String libPath = "lib/" + getPlatform() + "/" + nativeLibName;

                System.out.println(libPath);

                File temporaryDir = new File(System.getProperty("java.io.tmpdir"), "com/aayushatharva/brotli4j" + System.nanoTime());
                temporaryDir.deleteOnExit();

                File temp = new File(temporaryDir, libPath);

                try (InputStream in = Brotli4jLoader.class.getResourceAsStream("/" + libPath)) {
                    Files.copy(in, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Throwable throwable) {
                    temp.delete();
                    throw throwable;
                }

                System.load(temp.getAbsolutePath());

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
            } else {
                return "linux_x86";
            }
        } else {
            throw new UnsupportedOperationException("Unsupported OS and Architecture: " + osName + ", " + archName);
        }
    }
}
