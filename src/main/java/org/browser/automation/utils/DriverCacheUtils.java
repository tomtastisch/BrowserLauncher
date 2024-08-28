package org.browser.automation.utils;

import lombok.experimental.UtilityClass;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A utility class for operations related to {@link WebDriver} instances and their session IDs.
 * This class contains static methods that aid in retrieving session IDs from WebDriver instances
 * and managing WebDriver caches.
 *
 * <p>As a utility class, it is not intended to be instantiated. It provides methods for:
 * <ul>
 *     <li>Retrieving the session ID of a WebDriver from a cache.</li>
 *     <li>Generating or retrieving a session ID from a WebDriver instance.</li>
 * </ul>
 */
@UtilityClass
public class DriverCacheUtils {

    /**
     * Retrieves the session ID of a {@link WebDriver} instance from the provided cache.
     * This method searches the cache for a {@link WebDriver} instance that matches the given
     * driver and returns its associated session ID if found.
     *
     * <p>The search is performed by iterating over the entries of the {@link WebDriverCache},
     * comparing each cached WebDriver instance with the provided driver. If a match is found,
     * the session ID of the matching WebDriver is returned.</p>
     *
     * <p>If no matching WebDriver is found in the cache, this method returns an empty string.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * WebDriverCache cache = WebDriverCache.getInstance();
     * WebDriver driver = ...; // a WebDriver instance
     * String sessionId = DriverCacheUtils.getSessionId(cache, driver);
     * </pre>
     *
     * @param cache  the {@link WebDriverCache} instance containing cached WebDriver instances.
     * @param driver the {@link WebDriver} instance whose session ID is to be retrieved.
     * @return the session ID of the WebDriver if it is found in the cache, otherwise an empty string.
     */
    public String getSessionId(WebDriverCache cache, WebDriver driver) {
        return cache.getDriverCache().entrySet().stream()
                .filter(entry -> entry.getValue().equals(driver))
                .map(Map.Entry::getKey)
                .findAny()
                .orElse("");
    }

    /**
     * Retrieves the session ID of a {@link WebDriver} instance. If the WebDriver instance is
     * an instance of {@link RemoteWebDriver}, the session ID is directly retrieved from it.
     * If the WebDriver is not a {@link RemoteWebDriver}, a new UUID is generated and returned
     * as a fallback since non-remote WebDriver instances do not have session IDs.
     *
     * <p>This method is useful for generating a unique identifier for WebDriver instances
     * that do not support session IDs or when dealing with WebDriver instances in a context
     * where the session ID is not available.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * WebDriver driver = ...; // a WebDriver instance
     * String sessionId = DriverCacheUtils.getSessionId(driver);
     * </pre>
     *
     * @param driver the {@link WebDriver} instance from which the session ID is to be retrieved.
     * @return the session ID as a {@link String} if the WebDriver is a {@link RemoteWebDriver};
     *         otherwise, a newly generated UUID.
     */
    public String getSessionId(WebDriver driver) {
        return Optional.ofNullable(driver)
                .filter(d -> d instanceof RemoteWebDriver)
                .map(d -> ((RemoteWebDriver) d).getSessionId())
                .map(Object::toString)
                .orElse(UUID.randomUUID().toString());
    }
}