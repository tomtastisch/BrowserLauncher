package org.browser.automation.core;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.browser.automation.core.BrowserDetector.BrowserInfo;
import org.browser.automation.exception.browser.driver.WebDriverInitializationException;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@code BrowserManager} class is responsible for managing and caching instances of {@code WebDriver}.
 * It follows the Singleton design pattern, ensuring a single instance is available throughout the application.
 *
 * <p>This class provides functionalities such as:
 * <ul>
 *     <li>Caching and retrieving {@code WebDriver} instances based on browser information.</li>
 *     <li>Creating new {@code WebDriver} instances when necessary.</li>
 *     <li>Checking for the existence of browser instances in the cache.</li>
 *     <li>Updating the expiration settings for cached instances.</li>
 * </ul>
 *
 * <p>Key methods include:
 * <ul>
 *     <li>{@link #computeIfAbsent(BrowserInfo, MutableCapabilities, WindowType)}: Retrieves an existing {@code WebDriver}
 *         instance or creates a new one if not already cached.</li>
 *     <li>{@link #containsBrowser(String)}: Checks if a specified browser is present in the cache.</li>
 *     <li>{@link #updateCacheExpiration(long, TimeUnit)}: Updates the expiration time for cached WebDriver instances.</li>
 * </ul>
 *
 * <p>This class is thread-safe, allowing concurrent access to its methods without compromising data integrity.</p>
 *
 * <p>Example Usage:
 * <pre>
 * BrowserManager browserManager = BrowserManager.getInstance();
 * CompletableFuture<WebDriver> driverFuture = browserManager.computeIfAbsent(browserInfo, capabilities, WindowType.TAB);
 * </pre>
 * </p>
 *
 * @see BrowserInfo
 * @see MutableCapabilities
 * @see WindowType
 * @see WebDriverInitializationException
 */
@Slf4j
@Getter(AccessLevel.PROTECTED)
@ThreadSafe
public class BrowserManager {

    private final BrowserDetector browserDetector; // BrowserDetector as a class-level instance

    private AsyncCache<String, WebDriver> cache;

    /**
     * Singleton Helper class for lazy-loading the Singleton instance.
     */
    private static class SingletonHelper {
        private static final BrowserManager INSTANCE = new BrowserManager();
    }

    /**
     * Retrieves the Singleton instance of {@code BrowserManager} using the default {@code WebDriverCache}.
     *
     * @return the Singleton instance of {@code BrowserManager}.
     */
    public static BrowserManager getInstance() {
        return SingletonHelper.INSTANCE;
    }

    protected BrowserManager() {
        this.browserDetector = new BrowserDetector();
        this.cache = createCache(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeAllBrowsers));
    }

    /**
     * Updates the expiration time for the cached {@code WebDriver} instances.
     *
     * <p>This method creates a new cache with the specified expiration settings and
     * migrates existing cached instances to the new cache. The old cache is then replaced
     * with the new one.</p>
     *
     * @param expirationTime The time duration after which cached instances expire.
     * @param unit The time unit for the expiration time.
     */
    @SuppressWarnings("all")
    protected void updateCacheExpiration(long expirationTime, TimeUnit unit) {
        AsyncCache<String, WebDriver> newCache = createCache(expirationTime, unit);

        cache.synchronous().asMap().forEach((key, value) -> {
            newCache.put(key, CompletableFuture.completedFuture(value));
        });

        this.cache = newCache;
    }

    private AsyncCache<String, WebDriver> createCache(long expirationTime, TimeUnit unit) {
        return Caffeine.newBuilder()
                .expireAfterWrite(expirationTime, unit)
                .recordStats()
                .buildAsync();
    }

    /**
     * Retrieves an existing {@code WebDriver} instance from the cache or creates a new one if not already cached.
     *
     * <p>This method first checks if a {@code WebDriver} instance for the specified browser (identified by {@code browserInfo}) is already
     * present in the cache. If so, it retrieves the cached instance. If not, it attempts to create a new {@code WebDriver} instance using
     * the {@code createWebDriver} method, caches it for future use, and then returns it. If the creation of a new WebDriver fails, an exception is thrown.</p>
     *
     * @param browserInfo Information about the browser, including its name and other details necessary for creating the WebDriver.
     * @param capabilities Optional capabilities to be used when creating the WebDriver, such as browser-specific options.
     * @param type The type of window ({@code WindowType}) that specifies whether to create a new tab or window.
     * @return An instance of {@code WebDriver} that is either retrieved from the cache or newly created.
     * @throws RuntimeException If the creation of a new {@code WebDriver} instance fails due to a {@code WebDriverInitializationException}.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Checks if a {@code WebDriver} instance for the specified browser name is already cached.</li>
     *   <li>If a cached instance is found, it is returned directly.</li>
     *   <li>If no cached instance is found, it attempts to create a new {@code WebDriver} instance using the {@code createWebDriver} method.</li>
     *   <li>The newly created {@code WebDriver} instance is then added to the cache for future use.</li>
     *   <li>If an error occurs while creating the {@code WebDriver}, a {@code RuntimeException} is thrown with a message detailing the failure.</li>
     * </ol>
     *
     * <p>The method is synchronized to ensure thread safety when accessing or modifying the WebDriver cache.</p>
     *
     * @see BrowserInfo
     * @see MutableCapabilities
     * @see WindowType
     * @see WebDriverInitializationException
     */
    public CompletableFuture<WebDriver> computeIfAbsent(@NonNull BrowserInfo browserInfo,
                                                        @Nullable MutableCapabilities capabilities,
                                                        WindowType type) {
        return cache.get(browserInfo.name(), driver -> createWebDriver(browserInfo, capabilities, type));
    }

    /**
     * Checks if a specified browser is present in the cache.
     *
     * @param browserName The name of the browser to check.
     * @return {@code true} if the browser is present in the cache; {@code false} otherwise.
     */
    public boolean containsBrowser(@NonNull String browserName) {
        return containsBrowser(browserName, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Checks if a specified browser is present in the cache using a custom comparator.
     *
     * @param browserName The name of the browser to check.
     * @param comparator The comparator used for case-insensitive comparison.
     * @return {@code true} if the browser is present in the cache; {@code false} otherwise.
     */
    public boolean containsBrowser(@NonNull String browserName, @NonNull Comparator<String> comparator) {
        Map<String, WebDriver> caseInsensitiveCache = new TreeMap<>(comparator);
        caseInsensitiveCache.putAll(cache.synchronous().asMap());
        return caseInsensitiveCache.containsKey(browserName);
    }

    /**
     * Creates and returns an instance of {@code WebDriver} based on the provided {@code BrowserInfo}, capabilities, and window type.
     *
     * <p>This method retrieves a list of installed browsers and filters it to find a browser matching the name specified in
     * {@code browserInfo}. It then uses the {@code browserDetector} to instantiate a {@code WebDriver} for the matched browser with the provided
     * capabilities and window type. If no matching browser is found, an exception is thrown.</p>
     *
     * @param browserInfo Information about the browser, including its name and other relevant details.
     * @param capabilities Optional capabilities to be set on the WebDriver, such as browser-specific options or configurations.
     * @param type The type of window ({@code WindowType}) indicating whether to create a new tab or window.
     * @return An instance of {@code WebDriver} configured according to the specified {@code browserInfo}, {@code capabilities}, and {@code type}.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Retrieves a list of installed browsers from the {@code browserDetector}.</li>
     *   <li>Filters the list to find a browser that matches the name specified in {@code browserInfo}.</li>
     *   <li>If a matching browser is found, it uses {@code browserDetector} to instantiate a WebDriver for the browser, passing in the
     *       provided capabilities and window type.</li>
     *   <li>If no matching browser is found, throws a {@code WebDriverInitializationException} indicating that the browser is
     * unsupported or unavailable.</li>
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
    @SneakyThrows
    private WebDriver createWebDriver(@NonNull BrowserInfo browserInfo,
                                      @Nullable MutableCapabilities capabilities,
                                      WindowType type) {
        // Instantiate the WebDriver using the provided options
        return browserDetector.getInstalledBrowserInfos().stream()
                .filter(browser -> browser.name().equalsIgnoreCase(browserInfo.name()))
                .findFirst()
                .map(browser -> browserDetector.instantiateDriver(browser, capabilities, type))
                .orElseThrow(() -> new WebDriverInitializationException(browserInfo.name()));
    }

    /**
     * Closes all open browser instances and releases associated resources.
     *
     * <p>This method iterates over the cached {@code WebDriver} instances and calls the {@code close} and {@code quit} methods
     * on each instance to ensure proper resource cleanup. This method is called during application shutdown.</p>
     */
    @RuntimeType
    private void closeAllBrowsers() {
        cache.synchronous().asMap().forEach((key, driver) -> {
            if (Objects.nonNull(driver)) {
                try {
                    driver.close();
                    driver.quit();
                } catch(Exception e) {
                    log.error(e.getMessage());
                }
            }
        });
    }
}