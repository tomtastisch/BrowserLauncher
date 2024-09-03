package org.browser.automation.exception.browser;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when the BrowserManager is not initialized before performing operations that require it.
 * This exception is designed to indicate that the BrowserManager must be set using the appropriate methods
 * before invoking operations that depend on it.
 */
public class BrowserManagerNotInitializedException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code BrowserManagerNotInitializedException} with a detailed message
     * indicating that the BrowserManager was not initialized.
     */
    public BrowserManagerNotInitializedException() {
        super("exception.browser.manager.not.initialized");
    }

    /**
     * Constructs a new {@code BrowserManagerNotInitializedException} with a custom message
     * explaining the cause of the exception.
     *
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public BrowserManagerNotInitializedException(String customMessage) {
        super(customMessage, "exception.browser.manager.not.initialized.custom");
    }
}