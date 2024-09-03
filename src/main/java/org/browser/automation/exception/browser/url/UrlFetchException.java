package org.browser.automation.exception.browser.url;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when there is an error fetching the blacklist.
 */
public class UrlFetchException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code UrlFetchException} with a detailed message
     * indicating that an error occurred while fetching the blacklist.
     *
     * @param url the URL from which the blacklist was being fetched.
     * @param detail a description or context of the error.
     */
    public UrlFetchException(String url, String detail) {
        super("exception.url.fetch.error", url, detail);
    }

    /**
     * Constructs a new {@code UrlFetchException} with a custom message
     * and the specified detail about the error.
     *
     * @param url the URL from which the blacklist was being fetched.
     * @param detail a description or context of the error.
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public UrlFetchException(String url, String detail, String customMessage) {
        super(customMessage, "exception.url.fetch.error.custom", url, detail);
    }
}