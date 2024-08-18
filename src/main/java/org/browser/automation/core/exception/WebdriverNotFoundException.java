package org.browser.automation.core.exception;

import org.browser.automation.core.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when a specific WebDriver class cannot be found during the dynamic resolution process.
 * This exception is intended to indicate that a fully qualified class name (e.g., "org.openqa.selenium.chrome.ChromeDriver")
 * was provided, but no matching WebDriver class could be located.
 */
public class WebdriverNotFoundException extends LocalizedExceptionBase {

    /**
     * Constructs a new {@code WebdriverNotFoundException} with a detailed message
     * indicating that the specified WebDriver class could not be found.
     *
     * @param className the fully qualified class name that was expected but not found.
     */
    public WebdriverNotFoundException(String className) {
        super("exception.webdriver.not.found", className);
    }

    /**
     * Constructs a new {@code WebdriverNotFoundException} with a custom message
     * and the specified class name that could not be found.
     *
     * @param className     the fully qualified class name that was expected but not found.
     * @param customMessage a custom message explaining the cause of the exception.
     */
    public WebdriverNotFoundException(String className, String customMessage) {
        super(customMessage, "exception.webdriver.not.found.custom", className);
    }
}