package org.browser.automation.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.browser.automation.core.BrowserDetector.BrowserInfo;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.remote.AbstractDriverOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import com.google.common.base.Preconditions;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@code DriverUtils} class provides utility methods for managing and instantiating {@link WebDriver} instances.
 * It includes functionalities for creating browser-specific options, instantiating WebDriver instances using various
 * constructors, and configuring WebDriver binaries through WebDriverManager.
 *
 * <p>This class is designed as a utility class, meaning it contains only static methods and should not be instantiated.
 * It centralizes the logic related to WebDriver management, making it reusable across different parts of the application.</p>
 *
 * <p>Main functionalities provided by this class include:</p>
 * <ul>
 *     <li>Creating browser-specific options instances (e.g., {@code ChromeOptions}) based on the browser name.</li>
 *     <li>Instantiating WebDriver instances using constructors that accept {@link AbstractDriverOptions} objects.</li>
 *     <li>Providing fallback mechanisms to instantiate WebDriver instances using default constructors if necessary.</li>
 *     <li>Configuring and launching WebDriverManager to manage WebDriver binaries and create instances.</li>
 *     <li>Retrieving session IDs from WebDriver instances or generating unique identifiers if session IDs are not available.</li>
 *     <li>Extracting the browser name from WebDriver's capabilities.</li>
 * </ul>
 */
@Slf4j
@UtilityClass
public class DriverUtils {


    /**
     * Creates an instance of {@code WebDriver} using the provided driver class and capabilities.
     * This method uses {@link AbstractDriverOptions} for instantiation and provides a fallback mechanism.
     *
     * @param browserInfo  the {@code Class} of the WebDriver to instantiate.
     * @param capabilities the {@link MutableCapabilities} to be passed to the WebDriver's constructor.
     * @throws InvocationTargetException if the constructor or method invoked throws an exception.
     * @throws InstantiationException    if the WebDriver class cannot be instantiated.
     * @throws IllegalAccessException    if the constructor or method cannot be accessed.
     */
    public WebDriver createWebDriverInstance(BrowserInfo browserInfo,
                                             MutableCapabilities capabilities,
                                             WindowType type)
            throws InvocationTargetException, InstantiationException, IllegalAccessException {

        AbstractDriverOptions<?> options = createOptionsInstance(browserInfo.driverClass().getSimpleName(), capabilities);
        return createWebDriverInstance(browserInfo, options, type);
    }

    /**
     * Creates an instance of `WebDriver` based on the provided `BrowserInfo` and optional configuration parameters.
     *
     * <p>This method uses the class specified in the `BrowserInfo` to instantiate a new `WebDriver` object.
     * Depending on the provided `WindowType` and optional configuration parameters, either a constructor with parameters
     * is invoked, or the default constructor is used. If the `WindowType` is set to `TAB`, a system property is set to
     * ensure that an already opened browser tab is used before creating a new driver instance.</p>
     *
     * @param browserInfo Information about the browser, including the WebDriver class and path for the WebDriver.
     * @param options     Options to be used when creating the WebDriver. This can be `ChromeOptions`, `FirefoxOptions`, etc.
     * @param type        The type of window indicating whether the WebDriver should be created for a new tab or a new window.
     * @return An instance of `WebDriver` created based on the provided information and options.
     * @throws InvocationTargetException If an error occurs while invoking the constructor.
     * @throws InstantiationException    If instantiation of the `WebDriver` object fails.
     * @throws IllegalAccessException    If access to the constructor is not allowed.
     *
     *                                   <p>The method performs the following steps:</p>
     *                                   <ol>
     *                                     <li>Checks if the `WindowType` is `TAB`. If so, sets a system property to use the already opened browser tab.</li>
     *                                     <li>Checks if `options` is not empty. If so, invokes the constructor of the `WebDriver` class with the options.</li>
     *                                     <li>If `options` is empty, invokes the default constructor of the `WebDriver` class.</li>
     *                                     <li>Logs a success message if the WebDriver is successfully instantiated.</li>
     *                                     <li>Catches `NoSuchMethodException` and logs an error if no suitable method is found.</li>
     *                                   </ol>
     */
    public WebDriver createWebDriverInstance(BrowserInfo browserInfo, @Nullable AbstractDriverOptions<?> options, WindowType type)
            throws InvocationTargetException, InstantiationException, IllegalAccessException {

        WebDriver driver = null;
        Class<? extends WebDriver> wbClass = browserInfo.driverClass();

        try {

            // If the window type is TAB, set a system property.
            if (type.equals(WindowType.TAB)) {
                System.setProperty(browserInfo.driverClass().getName(), browserInfo.path());
                // Future implementation might be added here.
            }

            // Instantiate the WebDriver using options or default constructor.
            if (ObjectUtils.isNotEmpty(options)) {
                driver = ConstructorUtils.invokeConstructor(wbClass, options);
            } else {
                driver = ConstructorUtils.invokeConstructor(wbClass);
            }
            log.debug("Successfully instantiated WebDriver using class: {}", wbClass.getSimpleName());

        } catch (NoSuchMethodException e) {
            log.error("Error while instantiating WebDriver using class: {}", wbClass.getSimpleName(), e);
        }

        return driver;
    }

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
     * @param browserName  the name of the browser for which to create the options instance (e.g., "chrome", "firefox").
     * @param capabilities the {@link MutableCapabilities} to be merged into the created options instance.
     * @return the created and configured options instance.
     */
    @SneakyThrows({InvocationTargetException.class, NoSuchMethodException.class, IllegalAccessException.class, InstantiationException.class, ClassNotFoundException.class})
    public <T extends AbstractDriverOptions<T>> T createOptionsInstance(String browserName, MutableCapabilities capabilities) {

        Preconditions.checkArgument(StringUtils.isNotBlank(browserName), "Browser name cannot be blank");

        String normalizedBrowserName = StringUtils.removeEnd(browserName.toLowerCase(), "driver");
        String className = String.format("org.openqa.selenium.%s.%sOptions",
                normalizedBrowserName,
                StringUtils.capitalize(normalizedBrowserName));

        Class<?> optionsClass = ClassUtils.getClass(className);

        @SuppressWarnings("unchecked")
        T options = (T) ConstructorUtils.invokeConstructor(optionsClass);

        options.merge(capabilities);

        log.debug("Created options instance for browser: {}", browserName);
        return options;
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
     * @param driver the {@link WebDriver} instance from which the session ID is to be retrieved.
     * @return the session ID as a {@link String} if the WebDriver is a {@link RemoteWebDriver};
     * otherwise, a newly generated UUID.
     */
    public String getSessionId(WebDriver driver) {
        String sessionId = Optional.ofNullable(driver)
                .filter(rDriver -> rDriver instanceof RemoteWebDriver)
                .map(rDriver -> ((RemoteWebDriver) rDriver).getSessionId())
                .map(Object::toString)
                .orElse(String.valueOf(System.identityHashCode(driver)));

        log.debug("Retrieved session ID: {}", sessionId);
        return sessionId;
    }

    /**
     * Retrieves the name of the WebDriver instance in lowercase.
     * This method extracts the browser name from the WebDriver's capabilities,
     * ensuring that the name is returned in lowercase letters.
     *
     * @param driver the {@link WebDriver} instance from which the browser name is to be retrieved.
     * @return the name of the browser in lowercase, or "unknown" if the name cannot be determined.
     */
    public String getBrowserName(WebDriver driver) {
        String browserName = Optional.ofNullable(driver)
                .filter(rDriver -> rDriver instanceof RemoteWebDriver)
                .map(rDriver -> ((RemoteWebDriver) rDriver).getCapabilities().getBrowserName().toLowerCase())
                .orElse("unknown");

        log.debug("Retrieved browser name: {}", browserName);
        return browserName;
    }
}