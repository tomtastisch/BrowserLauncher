package org.browser.automation.core;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.access.cache.AbstractWebDriverCacheManager;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.browser.automation.exception.WebDriverInitializationException;
import org.openqa.selenium.WebDriver;

/**
 * The {@code BrowserManager} class is responsible for managing browser operations,
    < * including opening new windows or tabs. It uses the {@code WebDriverCache} to centrally manage
 * and retrieve {@code WebDriver} instances. This class follows the Singleton design pattern
 * to ensure a single instance is used throughout the application.
 *
 * <p>Key responsibilities of this class include:
 * <ul>
 *     <li>Opening a new browser window.</li>
 *     <li>Opening a new browser tab.</li>
 *     <li>Retrieving a cached WebDriver instance by session ID.</li>
 *     <li>Managing multiple browser instances efficiently using session IDs as keys.</li>
 * </ul>
 *
 * <p>The class provides flexibility for testability by allowing a custom {@code WebDriverCache}
 * instance to be injected, which can be useful when mocking or spying during unit tests.
 */
@Slf4j
public class BrowserManager extends AbstractWebDriverCacheManager {

    private final BrowserDetector browserDetector; // BrowserDetector as a class-level instance

    /**
     * Singleton Helper class for lazy-loading the Singleton instance.
     */
    private static class SingletonHelper {
        private static final BrowserManager INSTANCE = BrowserManager.getInstance(WebDriverCache.getInstance());
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
     * with the specified {@code WebDriverCache} instance and a shared instance of {@code BrowserDetector}.
     *
     * @param webDriverCache the {@code WebDriverCache} instance to be used.
     */
    private BrowserManager(WebDriverCache webDriverCache) {
        super(webDriverCache);
        this.browserDetector = new BrowserDetector(); // Single instance of BrowserDetector
    }

    /**
     * Retrieves or creates a {@code WebDriver} instance based on the provided driver name.
     * If a cached instance exists, it is returned; otherwise, a new instance is created,
     * added to the cache, and returned.
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @return the cached or newly created {@code WebDriver} instance.
     */
    @SneakyThrows
    public WebDriver getOrCreateDriver(String driverName) {
        WebDriver driver = createWebDriver(driverName);
        getWebDriverCache().addDriver(driver); // Add the driver to the cache with the session ID as the key
        return driver;
    }

    /**
     * Creates a new {@code WebDriver} instance based on the provided browser name.
     *
     * <p>This method retrieves the appropriate {@code WebDriver} class by dynamically identifying
     * the browser configuration from the list of installed browsers provided by the {@code BrowserDetector}.
     * The method uses a stream to filter through the list of available browsers and matches the one
     * that corresponds to the specified {@code driverName}. If a match is found, the associated {@code WebDriver}
     * class is instantiated using reflection, with exception handling managed by Lombok's {@code @SneakyThrows} annotation.</p>
     *
     * <p>If no browser matches the provided {@code driverName}, the method throws a
     * {@code WebDriverInitializationException} indicating that the specified browser is either
     * unsupported or unavailable on the system.</p>
     *
     * <p>Key steps in this method include:
     * <ul>
     *     <li>Using the {@code BrowserDetector} to retrieve a list of installed browsers.</li>
     *     <li>Filtering the list to find a browser that matches the given {@code driverName}, ignoring case.</li>
     *     <li>Instantiating the corresponding {@code WebDriver} class using the {@code BrowserDetector}.</li>
     *     <li>Handling reflection-related exceptions using the {@code @SneakyThrows} annotation to avoid explicit try-catch blocks.</li>
     * </ul>
     *
     * @param driverName the name of the browser (e.g., "chrome", "firefox", "edge").
     * @return a new instance of the corresponding {@code WebDriver} for the specified browser.
     * @throws WebDriverInitializationException if the specified browser is unsupported or unavailable,
     *                                          or if the {@code WebDriver} instance cannot be created.
     */
    public WebDriver createWebDriver(String driverName) throws WebDriverInitializationException {
        return browserDetector.getInstalledBrowsers().stream()
                .filter(browser -> browser.name().equalsIgnoreCase(driverName))
                .findFirst()
                .map(browser -> browserDetector.instantiateDriver(browser.driverClass()))
                .orElseThrow(() -> new WebDriverInitializationException("Unsupported or unavailable browser: " + driverName));
    }
}