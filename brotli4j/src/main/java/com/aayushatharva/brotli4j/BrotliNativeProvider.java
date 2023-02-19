package com.aayushatharva.brotli4j;

import com.aayushatharva.brotli4j.common.annotations.Internal;

/**
 * This interface is superseded by {@link com.aayushatharva.brotli4j.service.BrotliNativeProvider}.
 * However, we cannot remove this interface because it is part of the public API.
 * <p>
 * Also, this is an Internal API and should not be used by external users.
 */
@Deprecated
@Internal
public interface BrotliNativeProvider {

    /**
     * Do not use this method. It is superseded by {@link com.aayushatharva.brotli4j.service.BrotliNativeProvider#platformName()}.
     * <p>
     * This method is kept for backward compatibility. It will be removed in the future.
     * <p>
     */
    @Deprecated
    String platformName();
}
