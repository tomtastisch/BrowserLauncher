package org.browser.automation.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.BrowserDetector.BrowserInfo;
import org.browser.automation.core.access.cache.AbstractWebDriverCacheManager;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.browser.automation.exception.browser.driver.WebDriverInitializationException;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * The {@code BrowserManager} class is responsible for managing browser operations,
 * < * including opening new windows or tabs. It uses the {@code WebDriverCache} to centrally manage
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
@Getter(AccessLevel.PROTECTED)
@ThreadSafe
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

    protected BrowserManager() {
        this(WebDriverCache.getInstance());
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
     * Retrieves an existing `WebDriver` instance from the cache or creates a new one if not already cached.
     *
     * <p>This method first checks if a `WebDriver` instance for the specified browser (identified by `browserInfo`) is already
     * present in the cache. If so, it retrieves the cached instance. If not, it attempts to create a new `WebDriver` instance using
     * the `createWebDriver` method, caches it for future use, and then returns it. If the creation of a new WebDriver fails, an exception is thrown.</p>
     *
     * @param browserInfo Information about the browser, including its name and other details necessary for creating the WebDriver.
     * @param capabilities Optional capabilities to be used when creating the WebDriver, such as browser-specific options.
     * @param type The type of window (`WindowType`) that specifies whether to create a new tab or window.
     * @return An instance of `WebDriver` that is either retrieved from the cache or newly created.
     * @throws RuntimeException If the creation of a new `WebDriver` instance fails due to a `WebDriverInitializationException`.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Checks if a `WebDriver` instance for the specified browser name is already cached.</li>
     *   <li>If a cached instance is found, it is returned directly.</li>
     *   <li>If no cached instance is found, it attempts to create a new `WebDriver` instance using the `createWebDriver` method.</li>
     *   <li>The newly created `WebDriver` instance is then added to the cache for future use.</li>
     *   <li>If an error occurs while creating the `WebDriver`, a `RuntimeException` is thrown with a message detailing the failure.</li>
     * </ol>
     *
     * <p>The method is synchronized to ensure thread safety when accessing or modifying the WebDriver cache.</p>
     *
     * @see BrowserInfo
     * @see MutableCapabilities
     * @see WindowType
     * @see WebDriverInitializationException
     * @see WebDriverCache
     */
    @Synchronized
    public WebDriver getOrCreateDriver(BrowserInfo browserInfo, @Nullable MutableCapabilities capabilities, WindowType type) {

        WebDriver driver;

        // Check if the WebDriver for the specified browser is already cached
        if (isDriverCachedByName(browserInfo.name())) {
            // Retrieve the WebDriver from the cache
            driver = getWebDriverCache().getDriverByName(browserInfo.name());
        } else {
            try {
                // Create the WebDriver instance if not present in the cache
                driver = createWebDriver(browserInfo, capabilities, type);
                // Add the newly created WebDriver to the cache
                getWebDriverCache().addDriver(driver);
            } catch (WebDriverInitializationException e) {
                // Handle the exception if WebDriver creation fails
                throw new RuntimeException("Failed to create WebDriver for browser: " + browserInfo, e);
            }
        }

        return driver;
    }

    /**
     * Creates and returns an instance of `WebDriver` based on the provided `BrowserInfo`, capabilities, and window type.
     *
     * <p>This method retrieves a list of installed browsers and filters it to find a browser matching the name specified in
     * `browserInfo`. It then uses the `browserDetector` to instantiate a `WebDriver` for the matched browser with the provided
     * capabilities and window type. If no matching browser is found, an exception is thrown.</p>
     *
     * @param browserInfo Information about the browser, including its name and other relevant details.
     * @param capabilities Optional capabilities to be set on the WebDriver, such as browser-specific options or configurations.
     * @param type The type of window (`WindowType`) indicating whether to create a new tab or window.
     * @return An instance of `WebDriver` configured according to the specified `browserInfo`, `capabilities`, and `type`.
     * @throws WebDriverInitializationException If no matching browser is found or if the WebDriver cannot be instantiated.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Retrieves a list of installed browsers from the `browserDetector`.</li>
     *   <li>Filters the list to find a browser that matches the name specified in `browserInfo`.</li>
     *   <li>If a matching browser is found, it uses `browserDetector` to instantiate a WebDriver for the browser, passing in the
     *       provided capabilities and window type.</li>
     *   <li>If no matching browser is found, throws a `WebDriverInitializationException` indicating that the browser is unsupported
     *       or unavailable.</li>
     * </ol>
     *
     * <p>The method is synchronized to ensure thread safety when creating WebDriver instances.</p>
     *
     * @see BrowserInfo
     * @see MutableCapabilities
     * @see WindowType
     * @see WebDriverInitializationException
     * @see BrowserDetector
     */
    @Synchronized
    public WebDriver createWebDriver(BrowserInfo browserInfo, @Nullable MutableCapabilities capabilities, WindowType type) throws WebDriverInitializationException {
        // Instantiate the WebDriver using the provided options
        return browserDetector.getInstalledBrowserInfos().stream()
                .filter(browser -> browser.name().equalsIgnoreCase(browserInfo.name()))
                .findFirst()
                .map(browser -> browserDetector.instantiateDriver(browser, capabilities, type))
                .orElseThrow(() -> new WebDriverInitializationException("Unsupported or unavailable browser: " + browserInfo));
    }
}