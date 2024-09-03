package org.browser.automation.exception.browser.url;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when the provided URL is invalid.
 */
public class InvalidUrlException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code InvalidUrlException} with a detailed message
     * indicating that the URL is invalid.
     *
     * @param detail a description or context of the invalid URL.
     */
    public InvalidUrlException(String detail) {
        super("exception.invalid.url", detail);
    }

    /**
     * Constructs a new {@code InvalidUrlException} with a custom message
     * and the specified detail about the invalid URL.
     *
     * @param detail a description or context of the invalid URL.
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public InvalidUrlException(String detail, String customMessage) {
        super(customMessage, "exception.invalid.url.custom", detail);
    }
}