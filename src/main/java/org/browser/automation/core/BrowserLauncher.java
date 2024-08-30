package org.browser.automation.core;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.browser.automation.core.access.cache.WebDriverCacheManager;
import org.browser.automation.core.annotation.Essential;
import org.browser.automation.core.annotation.handler.LockInvocationHandler;
import org.browser.automation.exception.EssentialFieldsNotSetException;
import org.browser.automation.exception.TabNotOpenedException;
import org.browser.automation.exception.WebDriverInitializationException;
import org.browser.automation.exception.base.LocalizedExceptionBase;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The {@code BrowserLauncher} class provides a high-level API for managing browser operations such as opening new windows or tabs,
 * and handling multiple links across different browsers. It utilizes the {@link BrowserManager} to manage and retrieve instances of
 * {@link WebDriver}, ensuring that browser operations are executed in a controlled and orderly manner.
 *
 * <p>Key functionalities provided by this class include:
 * <ul>
 *     <li>{@code open}: Opens a URL in a new browser tab or window depending on the specified configuration.</li>
 *     <li>{@code execute}: Executes the browser operations that have been set up, opening the specified URLs in the designated browsers.</li>
 * </ul>
 *
 * <p>Unlike previous versions, the {@code useNewWindow} parameter is no longer mandatory to be set explicitly.
 * It defaults to {@code true}, meaning that by default, URLs will open in new windows unless specified otherwise.</p>
 *
 * <p>Example usage:
 * <pre>
 * BrowserLauncher launcher = BrowserLauncher.builder()
 *     .browsers(List.of("Chrome", "Firefox"))
 *     .urls(List.of("<a href="https://example.com">...</a>", "<a href="https://another-example.com">...</a>"))
 *     .build(); // useNewWindow defaults to true
 *
 * launcher.execute();
 * </pre>
 *
 * <p>Method Call Order:
 * <ol>
 *     <li>First, use {@code open} to set up the browser context.</li>
 *     <li>Then, use {@code execute} to perform the browser operations.</li>
 * </ol>
 */
@Slf4j
@Getter
@Builder(builderClassName = "BrowserLauncherBuilder", toBuilder = true)
public class BrowserLauncher {

    private static final BrowserDetector DETECTOR = new BrowserDetector();

    /**
     * The {@link BrowserManager} instance responsible for managing and retrieving {@link WebDriver} instances.
     */
    private BrowserManager manager;

    /**
     * Determines whether new windows should be opened for each URL.
     * This field defaults to {@code true}, meaning that by default, a new window will be opened for the first URL,
     * and subsequent URLs will open in new tabs in the same window.
     * If {@code false}, the URLs will open in new tabs within an existing browser window.
     */
    @Builder.Default
    private boolean useNewWindow = true;

    /**
     * A list of browser names (e.g., "Chrome", "Firefox") that will be used to open the URLs.
     * This field is annotated with {@link Essential}, meaning it is required for the execution of browser operations.
     */
    @Essential
    private final List<String> browsers;

    /**
     * A list of URLs that will be opened in the specified browsers.
     * This field is also annotated with {@link Essential}, indicating its necessity for the operation.
     */
    @Essential
    private final List<String> urls;

    /**
     * The main method demonstrating how to instantiate and use the {@code BrowserLauncher} class.
     * It configures the browser operations and then executes them.
     *
     * @param args command-line arguments (not used).
     */
    public static void main(String[] args) {

        /* BrowserLauncher launcher = BrowserLauncher.builder()
                .withDefaultBrowser()  // Set the default browser to be used
                .urls(List.of("https://example.com", "https://www.google.com"))  // Define the URLs to be opened
                .withNewManager()  // Use a new BrowserManager instance
                .build();  // useNewWindow defaults to true

        // Execute the configured browser operations
        //List<WebDriver> drivers = launcher.execute();
        */
        System.out.println("Hello World");
    }

    /**
     * The {@code BrowserLauncherBuilder} class provides a builder for creating instances of {@link BrowserLauncher}.
     * It allows for the configuration of various options, including the browser to use, the URLs to open, and
     * the {@link BrowserManager} instance to manage the browsers.
     */
    public static class BrowserLauncherBuilder {

        /**
         * Configures the builder to use the default browser detected on the system.
         *
         * @return the current {@link BrowserLauncherBuilder} instance for chaining.
         */
        BrowserLauncherBuilder withDefaultBrowser() {
            this.browsers = Collections.singletonList(DETECTOR.getDefaultBrowserName(true));
            return this;
        }

        /**
         * Configures the builder to use a new {@link BrowserManager} instance,
         * dynamically created using ByteBuddy with method interception via {@link LockInvocationHandler}.
         *
         * @return the current {@link BrowserLauncherBuilder} instance for chaining.
         */
        @SneakyThrows
        BrowserLauncherBuilder withNewManager() {
            this.manager = new ByteBuddy()
                    .subclass(BrowserManager.class)
                    .method(ElementMatchers.isDeclaredBy(WebDriverCacheManager.class))
                    .intercept(MethodDelegation.to(new LockInvocationHandler(BrowserManager.getInstance())))
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded().getDeclaredConstructor().newInstance();

            return this;
        }
    }

    /**
     * Executes the browser operations that have been set up by opening the specified URLs in the designated browsers.
     * This method must be called after setting up the browser context to ensure that the necessary browser operations are performed.
     *
     * @return a list of {@link WebDriver} instances used to open the URLs.
     * @throws EssentialFieldsNotSetException if any required fields are missing.
     */
    @Synchronized
    public List<WebDriver> execute() throws EssentialFieldsNotSetException {
        validate();
        return browsers.stream()
                .flatMap(browser -> urls.stream().map(url -> open(browser, url, useNewWindow)))
                .toList();
    }

    /**
     * Determines whether to open a new window or a new tab and then performs the operation.
     * If {@code useNewWindow} is true, a new window will be opened unless a window is already open for the specified browser.
     * If a window is already open, a new tab will be used instead.
     *
     * @param browserName  the name of the browser (e.g., "Chrome", "Firefox").
     * @param url          the URL to be opened.
     * @param useNewWindow if {@code true}, attempts to open a new window; otherwise, opens a new tab.
     * @return the {@link WebDriver} instance used for the operation.
     */
    @Synchronized
    private WebDriver open(String browserName, String url, boolean useNewWindow) {
        WebDriver driver = null;
        boolean isBrowserNotExists = !getManager().isDriverCachedByName(browserName);

        try {
            if (!useNewWindow && isBrowserNotExists) {
                throw new TabNotOpenedException(browserName);
            }

            driver = open(browserName, url, (isBrowserNotExists && useNewWindow) ? WindowType.WINDOW : WindowType.TAB);
        } catch (LocalizedExceptionBase le) {
            log.error("Failed to initialize WebDriver for browser: {}", browserName, le);
            manager.clearAllDrivers();
        }
        return driver;
    }

    /**
     * Opens a link in either a new tab or a new window based on the specified {@code WindowType}.
     * This method uses the {@code WebDriver}'s {@code switchTo().newWindow} function to create the new browser context.
     *
     * @param driverName the name of the {@link WebDriver} instance to be used.
     * @param link       the URL to be opened in the new tab or window.
     * @param type       the type of the window, represented by {@link WindowType}.
     * @return the {@link WebDriver} instance used for the operation.
     * @throws WebDriverInitializationException if the WebDriver instance cannot be initialized.
     */
    @Synchronized
    private WebDriver open(String driverName, String link, WindowType type) throws WebDriverInitializationException {
        WebDriver driver = handleBrowserOperation(driverName, type.toString());
        driver.switchTo().newWindow(type);  // Switches to the new window or tab based on the specified WindowType
        driver.get(link);  // Navigates to the specified URL in the newly opened window or tab
        return driver;
    }

    /**
     * Handles the common logic for browser operations, such as opening new windows or tabs.
     * Logs the operation being performed and retrieves the corresponding {@link WebDriver} instance from the {@link BrowserManager}.
     *
     * @param driverName    the name of the {@link WebDriver} instance to be used.
     * @param operationType the type of operation being performed (e.g., "window" or "tab").
     * @return the {@link WebDriver} instance associated with the operation.
     * @throws WebDriverInitializationException if the WebDriver instance cannot be retrieved or created.
     */
    @Synchronized
    private WebDriver handleBrowserOperation(String driverName, String operationType) throws WebDriverInitializationException {
        log.info("Performing '{}' operation for driver: {}", operationType, driverName);  // Logs the type of operation being performed
        return manager.getOrCreateDriver(driverName);  // Retrieves or creates a new WebDriver instance for the specified browser
    }

    /**
     * Validates that the required fields {@code browsers} and {@code urls} have been set.
     * If any of these essential fields are not set, an {@link EssentialFieldsNotSetException} is thrown.
     */
    private void validate() throws EssentialFieldsNotSetException {
        String missingFields = StringUtils.join(Arrays.stream(FieldUtils.getFieldsWithAnnotation(this.getClass(), Essential.class))
                .filter(field -> ObjectUtils.isEmpty(readField(field)))
                .map(Field::getName)
                .toArray(String[]::new), ",");

        if (ObjectUtils.isNotEmpty(missingFields)) {
            throw new EssentialFieldsNotSetException(missingFields);  // Throws an exception if essential fields are missing
        }
    }

    /**
     * Reads the value of the specified field using reflection.
     * This method is used to retrieve the values of fields annotated with {@link Essential}.
     *
     * @param field the field to be read.
     * @return the value of the field.
     */
    @SneakyThrows
    private Object readField(Field field) {
        return FieldUtils.readField(field, this, true);  // Uses reflection to read the value of the specified field
    }
}