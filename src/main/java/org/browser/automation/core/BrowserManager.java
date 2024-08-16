package org.browser.automation.core;

import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.openqa.selenium.WebDriver;

/**
 * Manages browser operations, including opening new windows or tabs.
 * This class utilizes the WebDriverCache to ensure all WebDriver instances are centrally managed.
 */
@Slf4j
public class BrowserManager {

    private final WebDriverCache webDriverCache = WebDriverCache.getInstance();

    // Singleton pattern with the SingletonHelper for thread safety
    private static class SingletonHelper {
        private static final BrowserManager INSTANCE = new BrowserManager();
    }

    public static BrowserManager getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private BrowserManager() {
        // Constructor is private to prevent external instantiation
    }

    /**
     * Opens a new browser window and returns the WebDriver instance.
     *
     * @param driverName The name of the WebDriver instance.
     * @return The WebDriver instance.
     */
    public WebDriver openNewWindow(String driverName) {
        return handleBrowserOperation(driverName, "window");
    }

    /**
     * Opens a new browser tab or a new window based on the provided boolean flag.
     *
     * @param driverName The name of the WebDriver instance.
     * @param openNewWindow If true, opens a new window; otherwise, opens a new tab.
     * @return The WebDriver instance.
     */
    public WebDriver openNewTab(String driverName, boolean openNewWindow) {
        if (openNewWindow) {
            return openNewWindow(driverName);
        }
        return handleBrowserOperation(driverName, "tab");
    }

    /**
     * Opens a new browser tab. By default, it does not open a new window.
     *
     * @param driverName The name of the WebDriver instance.
     * @return The WebDriver instance.
     */
    public WebDriver openNewTab(String driverName) {
        return openNewTab(driverName, false);
    }

    /**
     * Retrieves the WebDriver instance by name.
     *
     * @param driverName The name of the WebDriver instance.
     * @return The WebDriver instance, or null if not found.
     */
    public WebDriver getWebDriver(String driverName) {
        return webDriverCache.getDriver(driverName);
    }

    /**
     * Handles the common logic for browser operations (like opening new windows or tabs).
     *
     * @param driverName The name of the WebDriver instance.
     * @param operationType The type of operation being performed (e.g., "window" or "tab").
     * @return The WebDriver instance.
     */
    private WebDriver handleBrowserOperation(String driverName, String operationType) {
        log.info("Performing operation '{}' for driver: {}", operationType, driverName);
        WebDriver driver = webDriverCache.getDriver(driverName);
        if (driver == null) {
            log.error("Failed to create or retrieve WebDriver for: {}", driverName);
            throw new IllegalStateException("WebDriver could not be created for: " + driverName);
        }
        return driver;
    }
}