package org.browser.automation.core.main;

import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.BrowserLauncher;
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
    public static void main(String[] args) throws EssentialFieldsNotSetException, NoBrowserConfiguredException {

        BrowserLauncher launcher = BrowserLauncher.builder()
                .withInstalledBrowsers()  // Set the default browser to be used
                .withDefaultOptions()
                .urls(List.of("https://example.com", "https://www.google.com"))  // Define the URLs to be opened
                .withNewBrowserManager()  // Use a new BrowserManager instance
                .autoCleanUp()  // Enable automatic cleanup
                .build();  // useNewWindow defaults to true

        // Execute the configured browser operations
        List<WebDriver> drivers = launcher.validateAndExecute();

        // Output and close
        drivers.forEach(driver -> log.info(driver.toString()));
    }
}
