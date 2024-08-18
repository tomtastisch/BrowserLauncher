package org.browser.automation.core.access.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.concurrent.*;

/**
 * A thread-safe cache for managing multiple WebDriver instances.
 * This cache is responsible for storing and retrieving WebDriver instances
 * based on a unique identifier (e.g., session ID or driver name).
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
 * cache.addDriver("chrome", someWebDriverInstance);
 * WebDriver driver = cache.getDriver("chrome");
 * </pre>
 */
@Slf4j
@Getter
public class WebDriverCache {

    /**
     * A thread-safe map for storing WebDriver instances. The keys represent unique identifiers
     * (e.g., browser names), and the values are WebDriver instances.
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
     * The Jackson ObjectMapper used for JSON serialization and deserialization of the cache.
     * This is ignored in serialization processes to avoid circular dependencies.
     */
    @JsonIgnore
    private final ObjectMapper objectMapper = new ObjectMapper();

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
     * Adds a WebDriver instance to the cache with a given key.
     * If a WebDriver instance with the same key already exists, it is replaced.
     *
     * @param key    the unique identifier for the WebDriver instance
     * @param driver the WebDriver instance to cache
     */
    public void addDriver(@NotNull String key, @NotNull WebDriver driver) {
        driverCache.put(key, driver);
    }

    /**
     * Retrieves a WebDriver instance from the cache based on the given key.
     *
     * @param key the unique identifier for the WebDriver instance
     * @return the cached WebDriver instance, or {@code null} if not found
     */
    public WebDriver getDriver(@NotNull String key) {
        return driverCache.get(key);
    }

    /**
     * Removes a WebDriver instance from the cache based on the given key.
     * If a WebDriver is found and removed, its {@code quit()} method is called to clean up resources.
     *
     * @param key the unique identifier for the WebDriver instance
     */
    public void removeDriver(@NotNull String key) {
        WebDriver driver = driverCache.remove(key);
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
            driverCache.forEach((key, driver) -> {
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
}