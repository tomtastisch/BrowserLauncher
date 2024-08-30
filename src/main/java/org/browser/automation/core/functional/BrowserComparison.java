package org.browser.automation.core.functional;

import org.browser.automation.exception.WebDriverComparisonException;
import org.openqa.selenium.WebDriver;

import java.util.List;

@FunctionalInterface
public interface BrowserComparison {
    /**
     * Applies the comparison to a list of WebDriver instances.
     *
     * @param drivers the list of WebDriver instances to compare.
     * @throws WebDriverComparisonException if an error occurs during comparison.
     */
    void apply(List<WebDriver> drivers) throws WebDriverComparisonException;
}
