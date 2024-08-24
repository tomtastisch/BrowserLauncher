package org.browser.automation.core.access.cache;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.annotation.CacheLock;
import org.browser.automation.core.exception.WebDriverInitializationException;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Abstract class that provides cache operations for managing WebDriver instances.
 * This class can be extended by other classes that require WebDriver caching functionality.
 *
 * <p>This class acts as a utility layer that abstracts common cache-related operations
 * and provides convenience methods to manage {@link WebDriver} instances. It encapsulates
 * the caching logic and ensures consistent access to the {@link WebDriverCache}.
 *
 * <p>Key functionalities include:
 * <ul>
 *     <li>Retrieving a WebDriver instance from the cache by session ID.</li>
 *     <li>Checking if a WebDriver is cached for a given session ID.</li>
 *     <li>Retrieving all cached WebDriver instances.</li>
 *     <li>Clearing all cached WebDriver instances and logging the result.</li>
 *     <li>Getting the count of currently cached WebDriver instances.</li>
 * </ul>
 *
 * <p>This class is intended to be extended by classes that require WebDriver caching logic, such as
 * browser managers or test controllers that manage multiple browser sessions.</p>
 */
@Slf4j
@Getter(AccessLevel.PROTECTED)
public abstract class AbstractWebDriverCacheManager {

    private final WebDriverCache webDriverCache;

    /**
     * Constructor for creating an instance of {@code AbstractWebDriverCacheManager}.
     * The provided {@link WebDriverCache} instance is used for all cache operations.
     *
     * @param webDriverCache the {@link WebDriverCache} instance to be used for managing cached WebDriver instances.
     */
    protected AbstractWebDriverCacheManager(WebDriverCache webDriverCache) {
        this.webDriverCache = webDriverCache;
    }

    /**
     * Retrieves the cached {@link WebDriver} instance associated with the given session ID.
     * If the session ID is not found, a {@link WebDriverInitializationException} is thrown.
     *
     * <p>This method is typically used when a test or a browser manager needs to
     * retrieve a specific WebDriver instance based on its session ID.</p>
     *
     * @param sessionId the session ID of the {@link WebDriver} instance.
     * @return the cached {@link WebDriver} instance associated with the session ID.
     * @throws WebDriverInitializationException if no WebDriver instance is found for the given session ID.
     */
    @CacheLock
    public WebDriver getWebDriver(String sessionId) throws WebDriverInitializationException {
        return Optional.ofNullable(webDriverCache.getDriverBySessionId(sessionId))
                .orElseThrow(() -> {
                    log.error("No WebDriver found for session ID: {}", sessionId);
                    return new WebDriverInitializationException("No WebDriver found for session ID: " + sessionId);
                });
    }

    /**
     * Checks if a {@link WebDriver} instance is present in the cache for the given session ID.
     *
     * <p>This method is useful for verifying if a WebDriver instance is already cached
     * before performing operations that depend on its presence.</p>
     *
     * @param sessionId the session ID of the {@link WebDriver} instance.
     * @return {@code true} if the WebDriver is present in the cache, {@code false} otherwise.
     */
    @CacheLock
    public boolean isDriverCached(String sessionId) {
        return webDriverCache.getDriverBySessionId(sessionId) != null;
    }

    /**
     * Retrieves all {@link WebDriver} instances currently stored in the cache.
     *
     * <p>This method is useful when you need to inspect all active WebDriver sessions
     * or perform operations on all cached instances.</p>
     *
     * @return a list of all cached {@link WebDriver} instances.
     */
    @CacheLock
    public List<WebDriver> getAllCachedDrivers() {
        return new ArrayList<>(webDriverCache.getDriverCache().values());
    }

    /**
     * Removes all {@link WebDriver} instances from the cache and quits each driver.
     *
     * <p>This method is typically used during cleanup operations, such as at the end of a test
     * run or when shutting down the application. It ensures that all WebDriver instances
     * are properly closed and removed from the cache.</p>
     *
     * <p>The method logs how many WebDriver instances were successfully closed out of the
     * total initially cached instances.</p>
     */
    @CacheLock
    public void clearAllDrivers() {
        int initialSize = webDriverCache.getDriverCache().size();

        long successfullyClosed = webDriverCache.getDriverCache().values().stream()
                .flatMap(driver -> Optional.ofNullable(webDriverCache.removeDriver(driver)).stream())
                .count();

        log.info("Successfully closed {}/{} WebDriver instances.", successfullyClosed, initialSize);
    }

    /**
     * Gets the number of {@link WebDriver} instances currently stored in the cache.
     *
     * <p>This method provides a simple way to monitor how many WebDriver sessions
     * are currently active and managed by the cache.</p>
     *
     * @return the number of cached {@link WebDriver} instances.
     */
    @CacheLock
    public int getCachedDriverCount() {
        return webDriverCache.getDriverCache().size();
    }
}