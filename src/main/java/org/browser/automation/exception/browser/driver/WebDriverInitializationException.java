package org.browser.automation.exception.browser.driver;

import org.browser.automation.exception.base.LocalizedExceptionBase;

import java.io.Serializable;

/**
 * Exception thrown when a WebDriver instance cannot be initialized or retrieved.
 * This exception is intended to indicate that the browser automation process
 * encountered an issue due to the inability to locate or create a WebDriver instance.
 */
public class WebDriverInitializationException extends LocalizedExceptionBase implements Serializable {

  /**
   * Constructs a new {@code WebDriverInitializationException} with a detailed message
   * indicating that the WebDriver instance could not be found or created.
   *
   * @param driverName the name of the WebDriver instance that could not be found or created.
   */
  public WebDriverInitializationException(String driverName) {
    super("exception.webdriver.initialization", driverName);
  }

  /**
   * Constructs a new {@code WebDriverInitializationException} with a custom message
   * and the specified driver name that could not be initialized.
   *
   * @param driverName    the name of the WebDriver instance that could not be found or created.
   * @param customMessage a custom message explaining the cause of the exception.
   */
  public WebDriverInitializationException(String driverName, String customMessage) {
    super(customMessage, "exception.webdriver.initialization.custom", driverName);
  }
}