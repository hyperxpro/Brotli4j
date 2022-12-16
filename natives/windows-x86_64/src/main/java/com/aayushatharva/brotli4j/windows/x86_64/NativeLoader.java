package com.aayushatharva.brotli4j.windows.x86_64;

import com.aayushatharva.brotli4j.BrotliNativeProvider;

/**
 * Service class to access the native lib in a JPMS context
 */
public class NativeLoader implements BrotliNativeProvider {

    @Override
    public String platformName() {
        return "windows-x86_64";
    }
}
