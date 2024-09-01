package org.browser.automation.exception;

import org.browser.automation.core.BrowserLauncher;
import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * The {@code NoBrowserConfiguredException} is thrown when no browsers have been configured
 * in the {@link BrowserLauncher} instance before calling methods that require browser configurations.
 */
public class NoBrowserConfiguredException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code NoBrowserConfiguredException} with a detailed message
     * indicating that no browsers have been configured.
     *
     * @param detail a description or context of the missing browser configuration.
     */
    public NoBrowserConfiguredException(String detail) {
        super("exception.browser.not.configured", detail);
    }

    /**
     * Constructs a new {@code NoBrowserConfiguredException} with a custom message
     * and the specified detail about the missing browser configuration.
     *
     * @param detail a description or context of the missing browser configuration.
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public NoBrowserConfiguredException(String detail, String customMessage) {
        super(customMessage, "exception.browser.not.configured.custom", detail);
    }
}