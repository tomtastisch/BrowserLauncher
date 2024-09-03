package org.browser.automation.exception.browser.component;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when an attempt is made to open a new tab in a browser that is not currently open.
 * This exception indicates that a new tab cannot be created because the browser session does not exist.
 */
public class TabNotOpenedException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code TabNotOpenedException} with a detailed message
     * indicating that a new tab could not be opened because the browser is not open.
     *
     * @param browserName the name of the browser where the tab could not be opened.
     */
    public TabNotOpenedException(String browserName) {
        super("exception.tab.not.opened", browserName);
    }

    /**
     * Constructs a new {@code TabNotOpenedException} with a custom message
     * and the specified browser name where the tab could not be opened.
     *
     * @param browserName the name of the browser where the tab could not be opened.
     * @param customMessage  a custom message explaining the cause of the exception.
     */
    public TabNotOpenedException(String browserName, String customMessage) {
        super(customMessage, "exception.tab.not.opened.custom", browserName);
    }
}