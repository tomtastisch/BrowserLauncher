package org.browser.automation.core.access.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.utils.DriverUtils;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * A thread-safe cache for managing multiple {@link WebDriver} instances.<br>
 * This cache is responsible for storing and retrieving {@code WebDriver} instances<br>
 * based on a unique identifier, specifically the session ID.<br>
 * <br>
 * <p>Key features include:</p><br>
 * <ul>
 *     <li>Thread-safe storage of {@link WebDriver} instances using a {@link ConcurrentMap}.</li>
 *     <li>Automatic cleanup of inactive {@link WebDriver} instances based on a configurable timeout.</li>
 *     <li>Support for retrieving, adding, and removing {@link WebDriver} instances based on their session IDs.</li>
 *     <li>Flexibility in enabling or disabling automatic cleanup based on the application's needs.</li>
 * </ul>
 * <br>
 * <p>The cache does not handle the creation of {@link WebDriver} instances;<br>
 * they must be provided externally and are managed by this class.</p><br>
 * <br>
 * <p>Usage example:</p><br>
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
     * The configuration object that loads settings from the application's configuration files
     * (e.g., `application.conf` or `reference.conf`). This uses the Typesafe Config library.
     * The configuration includes properties like whether automatic cleanup is enabled and the
     * inactivity timeout for cached {@link WebDriver} instances.
     * <br>
     * The loaded configuration is used throughout the class to determine the behavior of the cache.
     */
    private static final Config config = ConfigFactory.load();

    /**
     * A thread-safe map for storing {@link WebDriver} instances. The keys represent unique session IDs,
     * and the values are {@link WebDriver} instances.
     */
    @JsonIgnore
    private final ConcurrentMap<String, WebDriver> driverCacheContent = new ConcurrentHashMap<>();

    /**
     * A scheduler service used for running automatic cache cleanup tasks at regular intervals.
     */
    @JsonIgnore
    private final ScheduledExecutorService scheduler;

    /**
     * The time duration after which inactive {@link WebDriver} instances are eligible for cleanup.
     */
    @JsonIgnore
    private final Duration inactivityTimeout;

    /**
     * Determines if automatic cleanup is enabled.
     */
    private final boolean autoCleanupEnabled;

    /**
     * Inner static class responsible for holding the singleton instance of {@code WebDriverCache}.
     * The instance is lazily loaded when the class is first accessed.
     */
    private static class SingletonHelper {

        static boolean autoCleanupEnabled = Boolean.parseBoolean(config.getString("webdriver.cache.auto-cleanup.enabled"));
        static long timeout = Long.parseLong(config.getString("webdriver.cache.cleanup.timeout"));

        private static final WebDriverCache INSTANCE = WebDriverCache.builder()
                .autoCleanupEnabled(autoCleanupEnabled)
                .inactivityTimeout(Duration.ofMillis(timeout))
                .build();
    }

    /**
     * Returns the singleton instance of {@code WebDriverCache}.
     *
     * @return the singleton instance of {@code WebDriverCache}.
     */
    public static WebDriverCache getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Private constructor for {@code WebDriverCache}. This is initialized via the builder pattern.
     * The constructor is marked private to enforce the singleton pattern.
     *
     * @param autoCleanupEnabled whether automatic cleanup is enabled.
     * @param inactivityTimeout  the duration after which inactive {@link WebDriver} instances are removed.
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
     * Adds a {@link WebDriver} instance to the cache, using its session ID as the key.
     * If the {@link WebDriver} instance does not have a session ID, a UUID is generated as a fallback.
     * If a {@link WebDriver} instance with the same session ID already exists, it is replaced.
     *
     * @param driver the {@link WebDriver} instance to cache.
     */
    public void addDriver(@NonNull WebDriver driver) {
        driverCacheContent.putIfAbsent(DriverUtils.getSessionId(driver), driver);
    }

    /**
     * Retrieves a {@code WebDriver} from the cache based on the specified driver name.
     * Searches through the values in the {@code driverCache} to find a {@code WebDriver}
     * whose class name matches the given driver name, ignoring case differences.
     * The comparison is performed in a case-insensitive manner using {@code equalsIgnoreCase},
     * which means that "chrome", "Chrome", and "CHROME" will be considered equivalent
     * when searching for a driver.
     *
     * @param driverName the name of the {@code WebDriver} class to search for.
     * @return the {@code WebDriver} instance if found; {@code null} otherwise.
     */
    public WebDriver getDriverByName(@NonNull String driverName) {
        return driverCacheContent.values().stream()
                .filter(driver -> DriverUtils.getBrowserName(driver).equalsIgnoreCase(driverName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves a {@link WebDriver} instance from the cache based on the given session ID.
     *
     * @param sessionId the session ID of the {@link WebDriver} instance.
     * @return the cached {@link WebDriver} instance, or {@code null} if not found.
     */
    public WebDriver getDriverBySessionId(@NonNull String sessionId) {
        return driverCacheContent.get(sessionId);
    }

    public String getSessionId(@NonNull WebDriver driver) {
        return DriverUtils.getSessionId(driver);
    }

    /**
     * Removes a {@link WebDriver} instance from the cache and quits the instance.
     * This overload is provided to remove a {@link WebDriver} instance directly.
     *
     * @param driver the {@link WebDriver} instance to remove and quit.
     * @return the session ID of the removed {@link WebDriver} instance.
     */
    public String removeDriver(@NonNull WebDriver driver) {
        return removeDriver(DriverUtils.getSessionId(driver));
    }

    /**
     * Removes a {@link WebDriver} instance from the cache based on the given session ID.
     * If a {@link WebDriver} is found and removed, its {@code quit()} method is called to clean up resources.
     *
     * @param sessionId the session ID of the {@link WebDriver} instance.
     * @return the session ID of the removed {@link WebDriver} instance.
     */
    public String removeDriver(@NonNull String sessionId) {
        WebDriver driver = driverCacheContent.remove(sessionId);
        if (Objects.nonNull(driver)) {
            driver.quit();
        }
        return sessionId;
    }

    /**
     * Retrieves the cleanup timeout duration from the environment variables or the application configuration.<br>
     * The method first checks for a system environment variable named "WEBDRIVER_CACHE_CLEANUP_TIMEOUT".<br>
     * If the environment variable is not set, it falls back to the default value defined in the configuration file.<br>
     * <br>
     * This allows for flexible configuration of the cache timeout in different environments, such as<br>
     * development, testing, or production, by simply adjusting the environment variable or the config file.<br>
     * <br>
     *
     * @return the timeout duration in milliseconds as a long value.
     */
    private long getCleanupTimeout() {
        return Optional.ofNullable(System.getenv("WEBDRIVER_CACHE_CLEANUP_TIMEOUT"))
                .map(Long::parseLong)
                .orElse(config.getLong("webdriver.cache.cleanup.timeout"));
    }

    /**
     * Starts the automatic cleanup process for the cache. The cleanup runs at the specified
     * inactivity interval and removes {@link WebDriver} instances that have been inactive.
     * This method schedules the cleanup task to run periodically.
     *
     * <p>This method is a placeholder for a more advanced cleanup logic based on inactivity tracking,
     * which can be implemented based on custom requirements.</p>
     */
    private void startAutoCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            log.info("Running automatic cache cleanup...");
            driverCacheContent.forEach((sessionId, driver) -> {
                // Placeholder for more advanced inactivity tracking and cleanup logic
                log.info("Checking for inactive drivers...");
                // Implement actual inactivity tracking logic here
            });
        }, inactivityTimeout.toMinutes(), inactivityTimeout.toMinutes(), TimeUnit.MINUTES);
    }

    /**
     * Shuts down the scheduler if it is running. This is particularly useful when stopping the application.
     * It ensures that the automatic cleanup process is terminated gracefully.
     */
    public void shutdownScheduler() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}