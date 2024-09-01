package org.browser.automation.core;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.browser.automation.core.access.cache.AbstractWebDriverCacheManager;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.browser.automation.exception.WebDriverInitializationException;
import org.browser.automation.utils.DriverUtils;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Objects;

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
     * Retrieves or creates a {@code WebDriver} instance based on the provided driver name.
     * If a cached instance exists, it is returned; otherwise, a new instance is created,
     * configured with default {@link MutableCapabilities}, added to the cache, and returned.
     *
     * <p>This method acts as a convenience overload that assumes default capabilities
     * for the specified browser.</p>
     *
     * @param driverName the name of the browser (e.g., "chrome", "firefox").
     * @return the cached or newly created {@code WebDriver} instance.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created.
     */
    @Synchronized
    public WebDriver getOrCreateDriver(String driverName) throws WebDriverInitializationException {
        return getOrCreateDriver(driverName, new MutableCapabilities());
    }

    /**
     * Retrieves or creates a {@code WebDriver} instance based on the provided driver name and custom capabilities.
     * If a cached instance exists, it is returned; otherwise, a new instance is created,
     * configured with the specified {@link MutableCapabilities}, added to the cache, and returned.
     *
     * <p>The method first checks the cache for an existing {@code WebDriver} instance associated with the
     * specified browser name. If a matching instance is found, it is returned immediately. If no such instance
     * exists, the method creates a new {@code WebDriver} using browser-specific options, which are determined
     * by merging the provided capabilities with the default settings for that browser.</p>
     *
     * @param driverName   the name of the browser (e.g., "chrome", "firefox").
     * @param capabilities the custom capabilities to configure the {@code WebDriver} instance.
     * @return the cached or newly created {@code WebDriver} instance.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created.
     */
    @Synchronized
    public WebDriver getOrCreateDriver(String driverName, MutableCapabilities capabilities) throws WebDriverInitializationException {
        // Check if existing in the cache
        WebDriver driver = getWebDriverCache().getDriverByName(driverName);

        if (Objects.isNull(driver)) {
            AbstractDriverOptions<?> options = DriverUtils.createOptionsInstance(driverName, capabilities);

            driver = createWebDriver(driverName, options);
            getWebDriverCache().addDriver(driver);
        }

        return driver;
    }

    /**
     * Creates a new {@code WebDriver} instance based on the provided browser name and options.
     *
     * <p>This method dynamically identifies the appropriate {@code WebDriver} class based on the
     * specified {@code driverName}. It then instantiates the driver using the provided
     * {@link AbstractDriverOptions} to configure the browser instance.</p>
     *
     * <p>Before instantiation, the method checks that the provided {@code AbstractDriverOptions} instance
     * is compatible with the {@code WebDriver} being created. Compatibility is determined by checking if the
     * {@code browserName} from the options partially matches the provided {@code driverName}. If the options
     * are not compatible with the specified browser, a {@code WebDriverInitializationException} is thrown.</p>
     *
     * <p>If no installed browser matches the provided {@code driverName}, the method throws a
     * {@code WebDriverInitializationException} indicating that the specified browser is either
     * unsupported or unavailable on the system.</p>
     *
     * @param driverName the name of the browser (e.g., "chrome", "firefox", "edge").
     * @param options    the {@link AbstractDriverOptions} used to configure the {@code WebDriver} instance.
     * @return a new instance of the corresponding {@code WebDriver} for the specified browser.
     * @throws WebDriverInitializationException if the specified browser is unsupported or unavailable,
     *                                          or if the {@code WebDriver} instance cannot be created.
     */
    @Synchronized
    public WebDriver createWebDriver(String driverName, AbstractDriverOptions<?> options) throws WebDriverInitializationException {
        // Check if the provided options are compatible with the WebDriver
        Preconditions.checkArgument(
                StringUtils.containsIgnoreCase(options.getBrowserName(), driverName),
                "Provided options (%s) are not compatible with the %s driver, expected options for %s.",
                options.getClass().getSimpleName(), driverName, options.getBrowserName()
        );

        // Instantiate the WebDriver using the provided options
        return browserDetector.getInstalledBrowsers().stream()
                .filter(browser -> browser.name().equalsIgnoreCase(driverName))
                .findFirst()
                .map(browser -> browserDetector.instantiateDriver(browser.driverClass(), options))
                .orElseThrow(() -> new WebDriverInitializationException("Unsupported or unavailable browser: " + driverName));
    }
}