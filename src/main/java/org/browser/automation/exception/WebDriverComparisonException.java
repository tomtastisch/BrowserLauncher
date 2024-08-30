package org.browser.automation.exception;

import org.browser.automation.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when an error occurs during the comparison of WebDriver instances.
 * This exception is designed to indicate that an unexpected condition was encountered
 * while comparing the results or states of WebDriver instances.
 */
public class WebDriverComparisonException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code WebDriverComparisonException} with a detailed message
     * indicating that an error occurred during WebDriver comparison.
     *
     * @param comparisonDetail a description or identifier of the comparison that failed.
     */
    public WebDriverComparisonException(String comparisonDetail) {
        super("exception.webdriver.comparison.error", comparisonDetail);
    }

    /**
     * Constructs a new {@code WebDriverComparisonException} with a custom message
     * and the specified comparison detail.
     *
     * @param comparisonDetail a description or identifier of the comparison that failed.
     * @param customMessage    a custom message explaining the cause of the exception.
     */
    public WebDriverComparisonException(String comparisonDetail, String customMessage) {
        super(customMessage, "exception.webdriver.comparison.error.custom", comparisonDetail);
    }
}
