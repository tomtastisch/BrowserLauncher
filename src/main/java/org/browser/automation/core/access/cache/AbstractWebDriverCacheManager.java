package org.browser.automation.core.access.cache;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.annotation.CacheLock;
import org.browser.automation.core.annotation.CacheLock.LockLevel;
import org.browser.automation.core.annotation.ResourceKey;
import org.browser.automation.core.access.cache.functional.WebDriverCacheManager;
import org.browser.automation.exception.browser.driver.WebDriverInitializationException;
import org.browser.automation.utils.DriverUtils;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract class that provides cache operations for managing WebDriver instances in a thread-safe manner
 * with configurable lock levels. This class can be extended by other classes that require WebDriver caching functionality.
 *
 * <p>This class acts as a utility layer that abstracts common cache-related operations and provides
 * convenience methods to manage {@link WebDriver} instances. It encapsulates the caching logic and ensures
 * consistent access to the {@link WebDriverCache}. Methods that modify or access the cache are annotated
 * with {@link CacheLock} to ensure thread-safe access.</p>
 *
 * <p>The key functionalities provided by this class include:
 * <ul>
 *     <li>Retrieving a WebDriver instance from the cache by session ID.</li>
 *     <li>Checking if a WebDriver is cached for a given session ID.</li>
 *     <li>Retrieving all cached WebDriver instances.</li>
 *     <li>Clearing all cached WebDriver instances and logging the result.</li>
 *     <li>Getting the count of currently cached WebDriver instances.</li>
 * </ul>
 *
 * <p>This class is intended to be extended by classes that require WebDriver caching logic, such as
 * browser managers or test controllers that manage multiple browser sessions. By annotating critical methods
 * with {@link CacheLock}, it ensures that cache operations remain thread-safe in multi-threaded environments.</p>
 */
@Slf4j
@Getter
public abstract class AbstractWebDriverCacheManager implements WebDriverCacheManager {

    @Getter(AccessLevel.PROTECTED)
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
     * <p>This method is typically used when a test or a browser manager needs to retrieve a specific
     * WebDriver instance based on its session ID. The {@link CacheLock} annotation ensures that this
     * operation is performed in a thread-safe manner using a resource-specific lock.</p>
     *
     * @param sessionId the session ID of the {@link WebDriver} instance.
     * @return the cached {@link WebDriver} instance associated with the session ID.
     * @throws WebDriverInitializationException if no WebDriver instance is found for the given session ID.
     */
    @Override
    @CacheLock(level = LockLevel.RESOURCE)
    public WebDriver getWebDriver(@ResourceKey String sessionId) throws WebDriverInitializationException {
        return Optional.ofNullable(webDriverCache.getDriverBySessionId(sessionId))
                .orElseThrow(() -> {
                    log.error("No WebDriver found for session ID: {}", sessionId);
                    return new WebDriverInitializationException("No WebDriver found for session ID: " + sessionId);
                });
    }

    /**
     * Checks if a {@link WebDriver} instance is cached by retrieving the session ID from the {@link WebDriverCache}
     * and verifying its presence.
     *
     * @param driver the {@link WebDriver} instance to check.
     * @return {@code true} if the WebDriver instance is present in the cache, {@code false} otherwise.
     */
    @Override
    @CacheLock(level = LockLevel.RESOURCE)
    public boolean isDriverCachedBySessionId(WebDriver driver) {
        return isDriverCachedBySessionId(DriverUtils.getSessionId(webDriverCache, driver));
    }

    /**
     * Checks if a {@link WebDriver} instance is present in the cache for the given session ID.
     *
     * <p>This method is useful for verifying if a WebDriver instance is already cached
     * before performing operations that depend on its presence. The {@link CacheLock} annotation ensures
     * that this check is thread-safe, preventing inconsistent cache state in multi-threaded scenarios.</p>
     *
     * @param sessionId the session ID of the {@link WebDriver} instance.
     * @return {@code true} if the WebDriver is present in the cache, {@code false} otherwise.
     */
    @Override
    @CacheLock(level = LockLevel.RESOURCE)
    public boolean isDriverCachedBySessionId(@ResourceKey String sessionId) {
        return Objects.nonNull(webDriverCache.getDriverBySessionId(sessionId));
    }

    @Override
    @CacheLock(level = LockLevel.RESOURCE)
    public boolean isDriverCachedByName(@ResourceKey String driverName) {
        return Objects.nonNull(webDriverCache.getDriverByName(driverName));
    }

    /**
     * Retrieves all {@link WebDriver} instances currently stored in the cache.
     *
     * <p>This method is useful when you need to inspect all active WebDriver sessions
     * or perform operations on all cached instances. The {@link CacheLock} annotation ensures
     * that the operation is performed in a thread-safe manner using a global lock.</p>
     *
     * @return a list of all cached {@link WebDriver} instances.
     */
    @Override
    @CacheLock(level = LockLevel.GLOBAL)
    public List<WebDriver> getAllCachedDrivers() {
        return new ArrayList<>(webDriverCache.getDriverCacheContent().values());
    }

    /**
     * Removes all {@link WebDriver} instances from the cache and quits each driver.
     *
     * <p>This method is typically used during cleanup operations, such as at the end of a test
     * run or when shutting down the application. It ensures that all WebDriver instances
     * are properly closed and removed from the cache. The {@link CacheLock} annotation ensures
     * that the operation is thread-safe, preventing race conditions during cache clearance.</p>
     *
     * <p>The method logs how many WebDriver instances were successfully closed out of the
     * total initially cached instances.</p>
     */
    @Override
    @Synchronized
    @CacheLock(level = LockLevel.GLOBAL)
    public void clearAllDrivers() {
        int initialSize = webDriverCache.getDriverCacheContent().size();

        long successfullyClosed = webDriverCache.getDriverCacheContent().values().stream()
                .flatMap(driver -> Optional.ofNullable(webDriverCache.removeDriver(driver)).stream())
                .count();

        log.info("Successfully closed {}/{} WebDriver instances.", successfullyClosed, initialSize);
    }

    /**
     * Gets the number of {@link WebDriver} instances currently stored in the cache.
     *
     * <p>This method provides a simple way to monitor how many WebDriver sessions
     * are currently active and managed by the cache. The {@link CacheLock} annotation ensures
     * that the operation is performed in a thread-safe manner using a global lock.</p>
     *
     * @return the number of cached {@link WebDriver} instances.
     */
    @Override
    @CacheLock(level = LockLevel.GLOBAL)
    public int getCachedDriverCount() {
        return webDriverCache.getDriverCacheContent().size();
    }
}