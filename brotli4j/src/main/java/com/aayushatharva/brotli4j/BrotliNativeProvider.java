package com.aayushatharva.brotli4j;

/**
 * A service interface that signals that an implementor jar contains a native lib.
 */
public interface BrotliNativeProvider {

    /**
     * Gives the name of the platform that this provider contains a native brotli lib for
     * @return The name of the native, e.g. linux-x86_64 or osx-aarch64
     */
    String platformName();
}
