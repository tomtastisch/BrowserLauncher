package org.browser.automation.core.main;

import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.BrowserLauncher;
import org.browser.automation.exception.BrowserManagerNotInitializedException;
import org.browser.automation.exception.EssentialFieldsNotSetException;
import org.browser.automation.exception.NoBrowserConfiguredException;
import org.openqa.selenium.WebDriver;

import java.util.List;

@Slf4j
class MainClass {

    /**
     * The main method demonstrating how to instantiate and use the {@code BrowserLauncher} class.
     * It configures the browser operations and then executes them.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) throws EssentialFieldsNotSetException, NoBrowserConfiguredException, BrowserManagerNotInitializedException {

        BrowserLauncher launcher = BrowserLauncher.builder()
                .withNewBrowserManager()
                .withDefaultBrowser() // Set the default browser to be used
                .urls(List.of("https://example.com", "https://www.google.com"))  // Define the URLs to be opened
                .withDefaultOptions()
                .autoCleanUp()
                .build();

        // Execute the configured browser operations
        List<WebDriver> drivers = launcher.validateAndExecute();

        // Output and close
        drivers.forEach(driver -> log.info(driver.toString()));

        System.exit(0);
    }
}
