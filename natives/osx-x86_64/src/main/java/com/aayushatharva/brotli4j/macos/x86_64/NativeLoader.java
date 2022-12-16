package com.aayushatharva.brotli4j.macos.x86_64;

import com.aayushatharva.brotli4j.BrotliNativeProvider;

/**
 * Service class to access the native lib in a JPMS context
 */
public class NativeLoader implements BrotliNativeProvider {

    @Override
    public String platformName() {
        return "osx-x86_64";
    }
}
