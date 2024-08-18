package org.browser.automation.core;

import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.browser.automation.core.exception.WebDriverInitializationException;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/**
 * The {@code BrowserManager} class is responsible for managing browser operations,
 * including opening new windows or tabs. It uses the {@code WebDriverCache} to centrally manage
 * and retrieve {@code WebDriver} instances. This class follows the Singleton design pattern
 * to ensure a single instance is used throughout the application.
 *
 * <p>Key responsibilities of this class include:
 * <ul>
 *     <li>Opening a new browser window.</li>
 *     <li>Opening a new browser tab.</li>
 *     <li>Retrieving a cached WebDriver instance.</li>
 * </ul>
 *
 * <p>The class provides flexibility for testability by allowing a custom {@code WebDriverCache}
 * instance to be injected, which can be useful when mocking or spying during unit tests.
 */
@Slf4j
public class BrowserManager {

    private final WebDriverCache webDriverCache;

    /**
     * Singleton Helper class for lazy-loading the Singleton instance.
     */
    private static class SingletonHelper {
        private static final BrowserManager INSTANCE = new BrowserManager(WebDriverCache.getInstance());
    }

    /**
     * Retrieves the Singleton instance of {@code BrowserManager} using the default {@code WebDriverCache}.
     *
     * @return the Singleton instance of {@code BrowserManager}.
     */
    public static BrowserManager getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Creates a new instance of {@code BrowserManager} with a custom {@code WebDriverCache}.
     * This is useful for injecting a mock or spy during testing.
     *
     * @param webDriverCache the {@code WebDriverCache} instance to be used by this {@code BrowserManager}.
     * @return a new instance of {@code BrowserManager} with the specified cache.
     */
    public static BrowserManager getInstance(WebDriverCache webDriverCache) {
        return new BrowserManager(webDriverCache);
    }

    /**
     * Private constructor to prevent external instantiation. Initializes the {@code BrowserManager}
     * with the specified {@code WebDriverCache} instance.
     *
     * @param webDriverCache the {@code WebDriverCache} instance to be used.
     */
    private BrowserManager(WebDriverCache webDriverCache) {
        this.webDriverCache = webDriverCache;
    }

    /**
     * Opens a new browser window and returns the associated {@code WebDriver} instance.
     * If the driver is not found in the cache, a {@code WebDriverInitializationException} is thrown.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @return the {@code WebDriver} instance associated with the new window.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewWindow(String driverName) throws WebDriverInitializationException {
        return handleBrowserOperation(driverName, "window");
    }

    /**
     * Opens a new browser tab or a new window based on the provided flag.
     * If {@code openNewWindow} is true, a new window is opened; otherwise, a new tab is opened.
     *
     * @param driverName    the name of the {@code WebDriver} instance.
     * @param openNewWindow if true, opens a new window; otherwise, opens a new tab.
     * @return the {@code WebDriver} instance associated with the operation.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewTab(String driverName, boolean openNewWindow) throws WebDriverInitializationException {
        return openNewWindow ? openNewWindow(driverName) : handleBrowserOperation(driverName, "tab");
    }

    /**
     * Opens a new browser tab by default, without opening a new window.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @return the {@code WebDriver} instance associated with the new tab.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewTab(String driverName) throws WebDriverInitializationException {
        return openNewTab(driverName, false);
    }

    /**
     * Retrieves the cached {@code WebDriver} instance associated with the given driver name.
     * If the driver is not found, {@code null} is returned.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @return the cached {@code WebDriver} instance, or {@code null} if not found.
     */
    public WebDriver getWebDriver(String driverName) {
        return webDriverCache.getDriver(driverName);
    }

    /**
     * Handles the common logic for browser operations (like opening new windows or tabs).
     * Logs the operation being performed and retrieves the corresponding {@code WebDriver} instance
     * from the cache. If the driver is not found, an exception is thrown.
     *
     * @param driverName    the name of the {@code WebDriver} instance.
     * @param operationType the type of operation being performed (e.g., "window" or "tab").
     * @return the {@code WebDriver} instance associated with the operation.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    private WebDriver handleBrowserOperation(String driverName, String operationType) throws WebDriverInitializationException {
        log.info("Performing '{}' operation for driver: {}", operationType, driverName);
        return Optional.ofNullable(webDriverCache.getDriver(driverName))
                .orElseThrow(() -> {
                    log.error("Failed to create or retrieve WebDriver for: {}", driverName);
                    return new WebDriverInitializationException(driverName);
                });
    }
}