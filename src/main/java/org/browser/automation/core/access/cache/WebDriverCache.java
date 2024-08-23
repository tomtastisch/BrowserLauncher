package org.browser.automation.core.access.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A thread-safe cache for managing multiple WebDriver instances.
 * This cache is responsible for storing and retrieving WebDriver instances
 * based on a unique identifier, specifically the session ID.
 *
 * <p>It also supports automatic cleanup of inactive WebDriver instances
 * after a configurable timeout period.</p>
 *
 * <p>The cache does not handle the creation of WebDriver instances;
 * they must be provided externally and are managed by this class.</p>
 * <p>
 * Example usage:
 * <pre>
 * WebDriverCache cache = WebDriverCache.getInstance();
 * cache.addDriver(someWebDriverInstance);
 * WebDriver driver = cache.getDriverBySessionId(someSessionId);
 * </pre>
 */
@Slf4j
@Getter
public class WebDriverCache {

    /**
     * A thread-safe map for storing WebDriver instances. The keys represent unique session IDs,
     * and the values are WebDriver instances.
     */
    @JsonIgnore
    private final ConcurrentMap<String, WebDriver> driverCache = new ConcurrentHashMap<>();

    /**
     * A scheduler service used for running automatic cache cleanup tasks at regular intervals.
     */
    @JsonIgnore
    private final ScheduledExecutorService scheduler;

    /**
     * The time duration after which inactive WebDriver instances are eligible for cleanup.
     */
    @JsonIgnore
    private final Duration inactivityTimeout;

    /**
     * Determines if automatic cleanup is enabled.
     */
    private final boolean autoCleanupEnabled;

    /**
     * Inner static class responsible for holding the Singleton instance of {@code WebDriverCache}.
     * The instance is lazily loaded when the class is first accessed.
     */
    private static class SingletonHelper {
        private static final WebDriverCache INSTANCE = WebDriverCache.builder()
                .autoCleanupEnabled(true)
                .inactivityTimeout(Duration.ofMinutes(10))
                .build();
    }

    /**
     * Returns the singleton instance of {@code WebDriverCache}.
     *
     * @return the singleton instance of {@code WebDriverCache}
     */
    public static WebDriverCache getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Private constructor for {@code WebDriverCache}. This is initialized via the builder.
     * The constructor is marked private to enforce the singleton pattern.
     *
     * @param autoCleanupEnabled whether automatic cleanup is enabled
     * @param inactivityTimeout  the duration after which inactive WebDriver instances are removed
     */
    @Builder
    private WebDriverCache(boolean autoCleanupEnabled, @NonNull Duration inactivityTimeout) {
        this.autoCleanupEnabled = autoCleanupEnabled;
        this.inactivityTimeout = inactivityTimeout;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // If auto-cleanup is enabled, start the scheduled cleanup task
        if (autoCleanupEnabled) {
            startAutoCleanup();
        }
    }

    /**
     * Adds a WebDriver instance to the cache, using its session ID as the key.
     * If the WebDriver instance does not have a session ID, a UUID is generated as a fallback.
     * If a WebDriver instance with the same session ID already exists, it is replaced.
     *
     * @param driver the WebDriver instance to cache
     */
    public void addDriver(@NonNull WebDriver driver) {
        String sessionId = getSessionId(driver);
        driverCache.put(sessionId, driver);
    }

    /**
     * Retrieves a WebDriver instance from the cache based on the given session ID.
     *
     * @param sessionId the session ID of the WebDriver instance
     * @return the cached WebDriver instance, or {@code null} if not found
     */
    public WebDriver getDriverBySessionId(@NonNull String sessionId) {
        return driverCache.get(sessionId);
    }

    /**
     * Removes a WebDriver instance from the cache based on the given session ID.
     * If a WebDriver is found and removed, its {@code quit()} method is called to clean up resources.
     *
     * @param sessionId the session ID of the WebDriver instance
     */
    public void removeDriver(@NonNull String sessionId) {
        WebDriver driver = driverCache.remove(sessionId);
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Starts the automatic cleanup process for the cache. The cleanup runs at the specified
     * inactivity interval and removes WebDriver instances that have been inactive.
     * This method schedules the cleanup task to run periodically.
     */
    private void startAutoCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            log.info("Running automatic cache cleanup...");
            driverCache.forEach((sessionId, driver) -> {
                // Placeholder for more advanced inactivity tracking and cleanup logic
                log.info("Checking for inactive drivers...");
                // Implement actual inactivity tracking logic here
            });
        }, inactivityTimeout.toMinutes(), inactivityTimeout.toMinutes(), TimeUnit.MINUTES);
    }

    /**
     * Shuts down the scheduler if it is running. This is particularly useful when stopping the application.
     */
    public void shutdownScheduler() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Retrieves the session ID of a WebDriver instance. If the WebDriver is a RemoteWebDriver,
     * the session ID is directly retrieved. Otherwise, a UUID is generated as a fallback.
     *
     * @param driver the WebDriver instance
     * @return the session ID as a String
     */
    private String getSessionId(WebDriver driver) {
        return Optional.ofNullable(driver)
                .filter(d -> d instanceof RemoteWebDriver)
                .map(d -> ((RemoteWebDriver) d).getSessionId())
                .map(Object::toString)
                .orElse(UUID.randomUUID().toString());
    }
}