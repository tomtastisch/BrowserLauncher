package org.browser.automation.utils;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The {@code DriverUtils} class provides utility methods related to {@link WebDriver} instances,
 * such as retrieving session IDs, managing WebDriver caches, and creating browser-specific options instances.
 *
 * <p>This class is designed as a utility class, meaning that it contains only static methods and should not be instantiated.
 * It provides essential operations for working with WebDriver instances, especially in automated browser testing contexts.</p>
 *
 * <p>Main functionalities provided by this class include:</p>
 * <ul>
 *     <li>Retrieving the session ID of a WebDriver from a cache or directly from the WebDriver instance.</li>
 *     <li>Generating a unique session ID for WebDriver instances that do not support session IDs.</li>
 *     <li>Creating browser-specific options instances (e.g., ChromeOptions) based on the browser name.</li>
 *     <li>Extracting the browser name from the WebDriver's capabilities.</li>
 * </ul>
 */
@UtilityClass
public class DriverUtils {

    /**
     * Dynamically creates an instance of a browser-specific options class based on the provided browser name
     * and merges the provided {@link MutableCapabilities} into the created options instance.
     *
     * <p>This method uses reflection to dynamically load the appropriate options class (e.g., {@code ChromeOptions})
     * based on the provided browser name, instantiate it, and then merge the provided capabilities into the options instance.</p>
     *
     * <p>The class name is dynamically generated by capitalizing the first letter of the browser name and appending "Options".
     * For example, if the browser name is "chrome", the method will attempt to load "org.openqa.selenium.ChromeOptions".</p>
     *
     * <p>This method is useful when dynamically configuring WebDriver options during runtime, especially in environments where
     * the browser type is determined dynamically or passed as a parameter.</p>
     *
     * @param browserName  the name of the browser for which to create the options instance (e.g., "chrome", "firefox").
     * @param capabilities the {@link MutableCapabilities} to be merged into the created options instance.
     * @return the created and configured options instance.
     */
    @SuppressWarnings("unchecked")
    @SneakyThrows({InvocationTargetException.class, NoSuchMethodException.class, IllegalAccessException.class, InstantiationException.class, ClassNotFoundException.class})
    public <T extends AbstractDriverOptions<T>> T createOptionsInstance(String browserName, MutableCapabilities capabilities) {

        String className = "org.openqa.selenium." + browserName.toLowerCase() + "." + StringUtils.capitalize(browserName) + "Options";
        Class<?> optionsClass = Class.forName(className);

        T options = (T) ConstructorUtils.invokeConstructor(optionsClass);
        options.merge(capabilities);

        return options;
    }


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
     * String sessionId = DriverUtils.getSessionId(cache, driver);
     * </pre>
     *
     * @param cache  the {@link WebDriverCache} instance containing cached WebDriver instances.
     * @param driver the {@link WebDriver} instance whose session ID is to be retrieved.
     * @return the session ID of the WebDriver if it is found in the cache, otherwise an empty string.
     */
    public String getSessionId(WebDriverCache cache, WebDriver driver) {
        return cache.getDriverCacheContent().entrySet().stream()
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
     * String sessionId = DriverUtils.getSessionId(driver);
     * </pre>
     *
     * @param driver the {@link WebDriver} instance from which the session ID is to be retrieved.
     * @return the session ID as a {@link String} if the WebDriver is a {@link RemoteWebDriver};
     * otherwise, a newly generated UUID.
     */
    public String getSessionId(WebDriver driver) {
        return Optional.ofNullable(driver)
                .filter(d -> d instanceof RemoteWebDriver)
                .map(d -> ((RemoteWebDriver) d).getSessionId())
                .map(Object::toString)
                .orElse(UUID.randomUUID().toString());
    }

    /**
     * Retrieves the name of the WebDriver instance in lowercase.
     * This method extracts the browser name from the WebDriver's capabilities,
     * ensuring that the name is returned in lowercase letters.
     *
     * <p>Example usage:</p>
     * <pre>
     * WebDriver driver = ...; // a WebDriver instance
     * String browserName = DriverUtils.getBrowserName(driver);
     * </pre>
     *
     * @param driver the {@link WebDriver} instance from which the browser name is to be retrieved.
     * @return the name of the browser in lowercase, or "unknown" if the name cannot be determined.
     */
    public String getBrowserName(WebDriver driver) {
        return Optional.ofNullable(driver)
                .filter(rDriver -> rDriver instanceof RemoteWebDriver)
                .map(rDriver -> ((RemoteWebDriver) rDriver).getCapabilities().getBrowserName().toLowerCase())
                .orElse("unknown");
    }
}