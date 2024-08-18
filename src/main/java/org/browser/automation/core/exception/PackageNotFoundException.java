package org.browser.automation.core.exception;

import org.browser.automation.core.exception.base.LocalizedExceptionBase;

/**
 * Exception thrown when a specified package path does not contain any valid WebDriver classes.
 * This exception is designed to indicate that the given package was expected to contain WebDriver classes
 * but none were found during the dynamic resolution process.
 */
public class PackageNotFoundException extends LocalizedExceptionBase {

  /**
   * Constructs a new {@code PackageNotFoundException} with a detailed message
   * indicating that the specified package path does not contain any WebDriver classes.
   *
   * @param packageName the package path that was expected to contain WebDriver classes but did not.
   */
  public PackageNotFoundException(String packageName) {
    super("exception.package.not.found", packageName);
  }

  /**
   * Constructs a new {@code PackageNotFoundException} with a custom message
   * and the specified package path that did not contain any WebDriver classes.
   *
   * @param packageName  the package path that was expected to contain WebDriver classes but did not.
   * @param customMessage a custom message explaining the cause of the exception.
   */
  public PackageNotFoundException(String packageName, String customMessage) {
    super(customMessage, "exception.package.not.found.custom", packageName);
  }
}