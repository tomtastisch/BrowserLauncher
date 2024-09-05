package org.browser.automation.core;

import com.typesafe.config.Config;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.browser.automation.core.BrowserDetector.BrowserInfo;
import org.browser.automation.core.access.cache.functional.ConfigInvocationHandler;
import org.browser.automation.core.access.cache.functional.WebDriverCacheManager;
import org.browser.automation.core.annotation.Essential;
import org.browser.automation.core.annotation.handler.LockInvocationHandler;
import org.browser.automation.exception.browser.BrowserManagerNotInitializedException;
import org.browser.automation.exception.browser.NoBrowserConfiguredException;
import org.browser.automation.exception.custom.EssentialFieldsNotSetException;
import org.browser.automation.utils.ByteBuddyUtils;
import org.browser.automation.utils.DriverUtils;
import org.browser.automation.utils.UrlUtil;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code BrowserLauncher} class provides a high-level API for managing browser operations such as opening new
 * windows or tabs, and handling multiple links across different browsers. It utilizes the {@link BrowserManager} to
 * manage and retrieve instances of {@link WebDriver}, ensuring that browser operations are executed in a controlled
 * and orderly manner.
 *
 * <p>Key functionalities provided by this class include:
 * <ul>
 *     <li>{@code open}: Opens a URL in a new browser tab or window depending on the specified configuration.</li>
 *     <li>{@code execute}: Executes the browser operations that have been set up, opening the specified URLs in the
 *          designated browsers.</li>
 * </ul>
 *
 * <p>Unlike previous versions, the {@code useNewWindow} parameter is no longer mandatory to be set explicitly.
 * It defaults to {@code true}, meaning that by default, URLs will open in new windows unless specified otherwise.</p>
 *
 * <p>Example usage:
 * <pre>
 * BrowserLauncher launcher = BrowserLauncher.builder()
 *     .getDefaultBrowser()
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
@Builder(builderClassName = "BrowserLauncherBuilder", toBuilder = true, setterPrefix = "with")
public class BrowserLauncher {

    @Builder.Default
    private String builderId = "";

    /**
     * The {@link BrowserManager} instance responsible for managing and retrieving {@link WebDriver} instances.
     */
    @Getter(AccessLevel.NONE)
    private BrowserManager manager;

    /**
     * Determines whether new windows should be opened for each URL.
     * This value defaults to {@code false}, meaning the first URL will be opened in a new window,
     * and subsequent URLs will open in new tabs within the same window.
     * If {@code false}, all URLs will open in new tabs within an existing browser window.
     * If {@code true}, each URL will open in its own new window.
     */
    @Builder.Default
    private boolean useNewWindow = false;

    /**
     * Indicates whether this is the first call to the {@code open} method.
     * Initialized to {@code true} and used to differentiate between the initial call and subsequent calls.
     */
    @Builder.Default
    private boolean firstCall = true;

    /**
     * A list of browser names (e.g., "Chrome", "Firefox") that will be used to open the URLs.
     * This field is annotated with {@link Essential}, meaning it is required for the execution of browser operations.
     */
    private final List<BrowserDetector.BrowserInfo> browsers;

    /**
     * A list of URLs that will be opened in the specified browsers.
     * This field is also annotated with {@link Essential}, indicating its necessity for the operation.
     */
    private final List<String> urls;

    /**
     * Stores browser-specific {@link MutableCapabilities} options.
     *
     * <p>This map configures custom browser options (capabilities) for each browser
     * that the {@link BrowserLauncher} manages. The map's key is the lowercase
     * name of the browser (e.g., "chrome", "firefox"), and the value is a {@link MutableCapabilities}
     * object containing the specific settings and configurations for that browser.
     *
     * <p>You set these options during the build process of the {@link BrowserLauncher} using
     * the {@link BrowserLauncherBuilder#withOptions(String, MutableCapabilities)} or
     * {@link BrowserLauncherBuilder#withDefaultOptions()} methods. The launcher applies
     * these options when starting the browser, allowing you to add arguments, set preferences,
     * or specify custom capabilities tailored to your needs.
     *
     * <p>If you do not provide custom options for a specific browser, the launcher will use
     * a default {@link MutableCapabilities} object when starting the browser.
     *
     * <p>The map is initialized as a {@link ConcurrentHashMap} to ensure thread safety
     * when multiple threads access or modify the map concurrently.
     */
    @Getter(AccessLevel.PROTECTED)
    @Builder.Default
    protected Map<String, MutableCapabilities> options = new ConcurrentHashMap<>();

    /**
     * Validates the required fields and then executes the browser operations by opening the specified URLs
     * in the configured browsers. This method should be used when you want to ensure that all required fields
     * are set before performing any browser operations.
     *
     * <p>Example usage:
     * <pre>
     * {@code
     * BrowserLauncher launcher = BrowserLauncher.builder()
     *      .withDefaultBrowser()
     *      .urls(List.of("https://example.com", "https://www.google.com"))
     *      .build();
     * List<WebDriver> drivers = launcher.validateAndExecute();
     * }
     * </pre>
     *
     * @return a list of {@link WebDriver} instances used to open the URLs.
     * @throws EssentialFieldsNotSetException if any required fields are missing.
     */
    public List<WebDriver> validateAndExecute() throws EssentialFieldsNotSetException {
        validate();
        return execute();
    }

    /**
     * Executes the browser operations that have been set up by opening the specified URLs in the designated browsers.
     * After opening the URLs, any comparisons that have been configured will be executed.
     * This method must be called after setting up the browser context to ensure that the necessary browser operations
     * are performed.
     * <br>
     * <b>Note:</b> This method is considered <b>unsafe</b> for external use, meaning that it should be used with caution
     * as it assumes that all necessary validations have been completed prior to its invocation. It is recommended to use
     * {@link #validateAndExecute()} instead, which includes validation before execution.
     *
     * @return a list of {@link WebDriver} instances used to open the URLs.
     */
    @Synchronized
    List<WebDriver> execute() {
        return browsers.stream()
                .flatMap(browser -> urls.stream().map(url -> open(browser, url, useNewWindow)))
                .peek(driver ->
                        // log the new created Browser
                        log.info("Browser: {}, Session ID: {}, Tab: {}, URL: {}",
                                DriverUtils.getBrowserName(driver),
                                DriverUtils.getSessionId(driver),
                                driver.getWindowHandle(),
                                driver.getCurrentUrl())
                )
                .toList();
    }

    /**
     * Determines whether to open a new window or a new tab and then performs the operation.
     * If {@code useNewWindow} is true, a new window will be opened unless a window is already open for the specified browser.
     * If a window is already open, a new tab will be used instead.
     *
     * @param browserInfo  the name of the browser (e.g., "Chrome", "Firefox").
     * @param url          the URL to be opened.
     * @param useNewWindow if {@code true}, attempts to open a new window; otherwise, opens a new tab.
     * @return the {@link WebDriver} instance used for the operation.
     */
    @Synchronized
    @SneakyThrows
    private WebDriver open(BrowserInfo browserInfo, String url, boolean useNewWindow) {
        boolean isBrowserNotExists = !manager.isDriverCachedByName(browserInfo.name());
        return open(browserInfo, url, (isBrowserNotExists && useNewWindow) ? WindowType.WINDOW : WindowType.TAB);
    }

    /**
     * Opens a link in a new tab or window based on the specified {@code WindowType}.
     * This method utilizes the {@code WebDriver}'s {@code switchTo().newWindow} function to create a new browser context.
     *
     * @param browserInfo the {@link WebDriver} instance to be used for this operation.
     * @param link        the URL to be opened in the new tab or window.
     * @param type        the type of the window to open, represented by {@link WindowType}.
     * @return the {@link WebDriver} instance used for the operation.
     */
    @Synchronized
    private WebDriver open(BrowserInfo browserInfo, String link, WindowType type) {
        WebDriver driver = handleBrowserOperation(browserInfo, type);

        if (firstCall) { // On the first call, simply set the firstCall flag to false.
            firstCall = false;
        } else { // For subsequent calls, open a new window or tab based on the WindowType.
            driver.switchTo().newWindow(type);
        }

        // Navigate to the specified URL in the newly opened window or tab.
        driver.get(link);
        return driver;
    }


    /**
     * Handles operations related to the WebDriver for a specified browser and window type.
     *
     * <p>This method logs the type of operation being performed and retrieves or creates a WebDriver instance
     * for the given `browserInfo` and `windowType`. It uses capabilities configured for the browser or defaults to
     * an empty set if no specific capabilities are provided.</p>
     *
     * @param browserInfo Information about the browser, including its name and other details necessary for creating the WebDriver.
     * @param type        The type of window operation (`WindowType`) that specifies whether to create a new tab or window.
     * @return An instance of `WebDriver` that is either retrieved from the cache or newly created.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Logs the operation being performed, including the window type and browser information.</li>
     *   <li>Retrieves the capabilities associated with the browser from the `options` map. If no specific capabilities
     *       are configured, it defaults to a new `MutableCapabilities` instance.</li>
     *   <li>Calls the {@link BrowserManager#getOrCreateDriver(BrowserInfo, MutableCapabilities, WindowType)} method on
     *       the `manager` to retrieve or create a WebDriver instance based on the `browserInfo`, capabilities, and `windowType`.</li>
     * </ol>
     *
     * <p>The method is synchronized to ensure thread safety when interacting with WebDriver management operations.</p>
     * @see BrowserInfo
     * @see WindowType
     * @see MutableCapabilities
     * @see WebDriver
     * @see BrowserManager#getOrCreateDriver(BrowserInfo, MutableCapabilities, WindowType)
     */
    @Synchronized
    private WebDriver handleBrowserOperation(BrowserInfo browserInfo, WindowType type) {
        log.info("Performing '{}' operation for driver: {}", type, browserInfo);
        MutableCapabilities capabilities = options.getOrDefault(browserInfo.name().toLowerCase(), new MutableCapabilities());

        // Retrieves or creates a new WebDriver instance for the specified browser
        return manager.getOrCreateDriver(browserInfo, capabilities, type);
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
            // Throws an exception if essential fields are missing
            throw new EssentialFieldsNotSetException(missingFields);
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
        // Uses reflection to read the value of the specified field
        return FieldUtils.readField(field, this, true);
    }

    /**
     * The {@code BrowserLauncherBuilder} class is responsible for building and configuring instances of {@link BrowserLauncher}.
     * <p>
     * This class provides methods for setting up browser-specific configurations, such as WebDriver capabilities
     * and browser managers. It supports dynamic method interception and configuration loading based on the system's
     * environment and custom configurations.
     * <p>
     * The builder pattern implemented by this class allows for flexible and fluent configuration of the {@link BrowserLauncher},
     * ensuring that all required configurations are properly set before launching browsers.
     * <p>
     * Example usage:
     * <pre>
     * BrowserLauncher launcher = BrowserLauncher.builder()
     *     .withDefaultBrowser()
     *     .withDefaultOptions()
     *     .applyBrowserManager()
     *     .autoCleanUp()
     *     .build();
     * </pre>
     */
    @Slf4j
    public static class BrowserLauncherBuilder {

        /**
         * A temporary map used to store browser-specific {@link MutableCapabilities} options
         * before they are added to the main {@code options} map of the {@link BrowserLauncherBuilder}.
         *
         * <p>This map holds the capabilities that are set via the {@link #withOptions(String, MutableCapabilities)} method.
         * Once options are added to this map, they are transferred to the main {@code options} map using
         * the {@link #withOptions(Map)} method, which integrates them into the builder's configuration.
         *
         * <p>The use of this intermediate map allows for efficient option management, ensuring that capabilities
         * for each browser are stored and handled correctly before being finalized in the builder.
         *
         * <p>This map is initialized as a {@link ConcurrentHashMap} to ensure thread safety when multiple
         * threads access or modify the map concurrently.
         */
        Map<String, MutableCapabilities> innerOptions = new ConcurrentHashMap<>();

        /**
         * Configures the builder to use the default browser detected on the system.
         * <p>
         * This method uses the {@link BrowserDetector#getDefaultBrowserName(boolean)} method to automatically detect
         * and set the default browser for the system. The method checks the system's configuration to identify the
         * default web browser installed on the user's operating system. If the default browser cannot be determined,
         * it uses a fallback mechanism to select the first available browser from the list of installed browsers,
         * as indicated by the {@code useFallBackBrowser=true} parameter.
         * <p>
         * The {@code BrowserDetector} class provides the logic to identify the system's default browser by examining
         * system-specific configurations, executing OS commands, and analyzing installed browsers. This detection process
         * is robust and accommodates different operating systems by dynamically adapting to the environment.
         * <p>
         * If no browsers are installed or detected, and the fallback mechanism is triggered, the builder will set the first
         * available browser from the list of installed browsers as the default. This could result in unexpected behavior if
         * the chosen fallback browser is not the intended default browser for the user's context.
         * <p>
         * Note: This method should be called before any other method that requires a browser to be set, such as
         * {@link #withDefaultOptions()} or {@link #applyBrowserManager()}, to avoid {@link NoBrowserConfiguredException}.
         *
         * @return the current {@link BrowserLauncherBuilder} instance for chaining.
         */
        public BrowserLauncherBuilder withDefaultBrowser() throws BrowserManagerNotInitializedException {
            if (Objects.isNull(manager)) {
                throw new BrowserManagerNotInitializedException();
            }

            return withBrowsers(Collections.singletonList(
                    manager.getBrowserDetector()
                            .getDefaultBrowserInfo(true)
                            .orElseThrow()
            ));
        }

        /**
         * Configures the {@link BrowserLauncherBuilder} to use all browsers detected on the system.
         * This method retrieves the list of installed browsers using the {@link BrowserDetector},
         * and sets it as the list of browsers to be used by the {@link BrowserLauncher}.
         *
         * <p>This method is useful when you want to run tests or perform operations across all available
         * browsers on the system, ensuring broad coverage and compatibility.</p>
         *
         * <p><b>Key Implementation Details:</b></p>
         * <ul>
         *   <li>Retrieves the list of installed browsers using the {@link BrowserDetector#getInstalledBrowserInfos()} method.</li>
         *   <li>Maps the retrieved list of {@link BrowserDetector.BrowserInfo} objects to their names using the {@code name()} method.</li>
         *   <li>Stores the list of browser names in the {@code browsers} field of the {@link BrowserLauncherBuilder}.</li>
         * </ul>
         *
         * <p><b>Advantages of this Method:</b></p>
         * <ul>
         *   <li>Allows for easy configuration of the {@link BrowserLauncher} to run on all detected browsers
         *          without manual specification.</li>
         *   <li>Ensures that the {@link BrowserLauncher} will attempt to operate on every available browser,
         *          maximizing test coverage.</li>
         *   <li>Simple and efficient, using Java Streams to process the list of browsers.</li>
         * </ul>
         *
         * @return the current {@link BrowserLauncherBuilder} instance for chaining.
         */
        public BrowserLauncherBuilder withAllInstalledBrowsers() throws BrowserManagerNotInitializedException {

            if (Objects.isNull(manager)) {
                throw new BrowserManagerNotInitializedException();
            }

            return withBrowsers(manager.getBrowserDetector().getInstalledBrowserInfos());
        }

        /**
         * Filters the list of configured browsers to include only those that are installed on the system.
         * <p>
         * This method first retrieves a list of installed browsers using the {@link BrowserDetector}. It then filters
         * the provided list of browsers, keeping only those that are installed. For each browser that is not installed,
         * an informational log message is generated indicating that the browser cannot be used in the further process.
         * </p>
         *
         * @param browsers The list of {@code BrowserInfo} objects representing browsers to be filtered.
         * @return The updated {@code BrowserLauncherBuilder} instance, with the {@code browsers} list containing
         *         only the installed browsers. Browsers that are not installed will be excluded from the list.
         */
        public BrowserLauncherBuilder withBrowsers(List<BrowserInfo> browsers) {
            // Retrieve a list of installed browsers
            List<BrowserInfo> installedBrowsers = manager.getBrowserDetector().getInstalledBrowsers();

            // Filter out browsers that are not installed and log a message for each excluded browser
            this.browsers = browsers.stream()
                    .filter(browser -> {
                        boolean isInstalled = installedBrowsers.contains(browser);
                        if (!isInstalled) {
                            log.info("Browser: {} is not installed and will not be used in further processes.", browser);
                        }
                        return isInstalled;
                    })
                    .toList();

            return this;
        }

        /**
         * Configures the builder to use a dynamically created {@link BrowserManager} instance with method interception.
         * <p>
         * This method uses ByteBuddy to dynamically generate a subclass of {@link BrowserManager}, which acts as a proxy
         * for method calls to intercept and manage them via {@link LockInvocationHandler}. The generated proxy intercepts
         * method calls defined in {@link WebDriverCacheManager}, allowing the {@link LockInvocationHandler} to apply
         * additional behavior such as synchronization, logging, or other cross-cutting concerns before delegating
         * to the original {@link BrowserManager} implementation.
         * </p>
         * <p>
         * By leveraging ByteBuddy for dynamic subclass generation and method interception, this approach offers
         * flexibility in controlling method execution without altering the original {@link BrowserManager} code.
         * </p>
         * <p>
         * After configuring the {@link BrowserManager} with the {@link LockInvocationHandler}, it is instantiated and
         * assigned to the {@code manager} field of this builder. Note that if a {@code manager} instance was previously
         * set using another method, it will be replaced by this dynamically created instance.
         * </p>
         * <p>
         * Note: Ensure that a browser configuration has been set using {@link #withDefaultBrowser()} or a similar method
         * before invoking this method. If no browser has been configured, calling this method may result in unexpected
         * behavior.
         * </p>
         *
         * @return the current {@link BrowserLauncherBuilder} instance for method chaining.
         */
        @SneakyThrows
        public BrowserLauncherBuilder applyBrowserManager() {
            return withManager(ByteBuddyUtils.createInstance(
                    BrowserManager.class,
                    LockInvocationHandler.class,
                    ElementMatchers.isDeclaredBy(WebDriverCacheManager.class)
            ));
        }

        /**
         * Configures the builder with the default {@link MutableCapabilities} for each browser configured in the {@link BrowserLauncher}.
         * <p>
         * This method loads the default options from a configuration file and sets them for each browser in the builder.
         * It uses ByteBuddy to dynamically create a proxy for the {@link MutableCapabilities} class, allowing the
         * options to be configured based on the specific browser. The options are stored in the builder's internal map.
         * <p>
         * If {@link #withOptions(String, MutableCapabilities)} was called before this method, any already
         * configured capabilities for a browser will not be overwritten. This ensures that manually set options take
         * precedence over default ones.
         * <p>
         * Note: Ensure that at least one browser is configured using {@link #withDefaultBrowser()} or a similar
         * method before calling this method. Otherwise, a {@link NoBrowserConfiguredException} will be thrown.
         * <p>
         * This method should be used after at least one browser has been selected and configured. If called before
         * browser selection, it will have no effect. The default options are only applied to browsers that have been
         * configured; otherwise, no default options will be set.
         *
         * @return the current {@link BrowserLauncherBuilder} instance for chaining.
         * @throws NoBrowserConfiguredException if no browsers have been configured
         *                                      in the current {@link BrowserLauncher} instance.
         */
        public BrowserLauncherBuilder withDefaultOptions() throws NoBrowserConfiguredException {

            // Ensure that at least one browser is configured
            if (ObjectUtils.isEmpty(this.browsers)) {
                throw new NoBrowserConfiguredException("No browsers configured in the current BrowserLauncher instance.");
            }

            // Load configurations with ConfigurationProvider
            Config optionsConfig = ConfigurationProvider.getInstance("application_options").getConfig();
            List<BrowserInfo> configuredBrowsers = manager.getBrowserDetector().getInstalledBrowsers();
            List<String> namedBrowsers = this.browsers.stream().map(browser -> browser.name().toLowerCase()).toList();

            // Set default options for each configured browser, without overriding any existing manual configurations
            configuredBrowsers.stream()
                    .map(browser -> browser.name().toLowerCase())
                    .filter(namedBrowsers::contains)
                    .forEach(browserName -> withOptions(browserName, createCapabilities(browserName, optionsConfig)));

            return this;
        }

        /**
         * Adds custom {@link MutableCapabilities} for a specified browser.
         * <p>
         * This method allows you to manually configure and add {@link MutableCapabilities} for a specific browser.
         * It stores the provided capabilities in the builder's internal map of options, where the key is the browser
         * name.
         * <p>
         * This method can be used when you need to set specific capabilities that are not covered by the default options.
         * If this method is called before {@link #withDefaultOptions()}, the manually added capabilities will not be
         * overwritten.
         *
         * @param browserName  the name of the browser for which to set the capabilities.
         * @param capabilities the {@link MutableCapabilities} to set for the specified browser.
         * @return the current {@link BrowserLauncherBuilder} instance for chaining.
         */
        public BrowserLauncherBuilder withOptions(String browserName, MutableCapabilities capabilities) {
            innerOptions.putIfAbsent(browserName, capabilities);
            withOptions(innerOptions);
            return this;
        }

        /**
         * Validates URLs by checking if they are blacklisted using the full URL match mode.
         * <p>
         * This method calls {@link #applyBlacklistFilter(boolean)} with the default parameter {@code matchFullUrl} set to {@code true}.
         * It will remove any URLs from the {@code urls} list that are found to be blacklisted.
         * </p>
         *
         * @return a reference to the current {@code BrowserLauncherBuilder} object,
         * allowing for method chaining.
         * @see #applyBlacklistFilter(boolean)
         */
        public BrowserLauncherBuilder applyBlacklistFilter() {
            return applyBlacklistFilter(true);
        }

        /**
         * Validates each URL in the list to determine if it is blacklisted.
         * <p>
         * This method filters the list of URLs by checking if each URL is blacklisted using the
         * {@link UrlUtil#isUrlBlacklisted(String, boolean)} method. The check is based on the value of the
         * {@code matchFullUrl} parameter, which determines whether to perform a full URL match or just
         * a base domain match against the blacklist.
         * </p>
         * <p>
         * If a URL is found to be blacklisted, it is removed from the {@code urls} list.
         * </p>
         *
         * @param matchFullUrl if {@code true}, performs a full URL match against the blacklist;
         *                     if {@code false}, only the base domain of the URL is checked.
         * @return a reference to the current {@code BrowserLauncherBuilder} object, allowing for method chaining.
         * @see UrlUtil#isUrlBlacklisted(String, boolean)
         */
        public BrowserLauncherBuilder applyBlacklistFilter(boolean matchFullUrl) {
            urls = urls.stream()
                    .filter(url -> {
                        boolean isBlacklisted = UrlUtil.isUrlBlacklisted(url, matchFullUrl);
                        if (isBlacklisted) {
                            log.info("URL '{}' is blacklisted.", url);
                        }
                        return !isBlacklisted;
                    })
                    .toList();
            return this;
        }

        /**
         * Provides custom functionality for automatic cleanup of the {@link BrowserLauncher} instance.
         * <p>
         * This method registers a shutdown hook with the {@link Runtime} to ensure that all associated {@link WebDriver}
         * instances are properly closed when the {@link BrowserLauncher} is no longer in use. The cleanup process
         * is managed using an {@link ExecutorService} to handle the closing of drivers asynchronously. This method
         * is not a standard Lombok builder method and is specifically provided to facilitate automatic resource management
         * beyond the typical builder capabilities.
         * </p>
         *
         * <p>
         * When this method is invoked, it sets up a shutdown hook that will execute the cleanup actions when the JVM
         * terminates. If the cleanup takes longer than expected, the executor service will forcefully shut down after
         * a timeout period.
         * </p>
         *
         * @return the current {@link BrowserLauncherBuilder} instance for method chaining.
         */
        public BrowserLauncherBuilder autoCleanUp() {

            Optional.of(this.manager).ifPresent(mngr -> {
                ExecutorService executor = Executors.newSingleThreadExecutor();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    executor.submit(manager::clearAllDrivers);
                    executor.shutdown();
                    try {
                        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                            executor.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                }));
            });
            return this;
        }

        /**
         * Dynamically creates and configures the {@link MutableCapabilities} for the specified browser.
         * <p>
         * This method uses ByteBuddy to create a subclass of {@link MutableCapabilities}, which acts as a proxy
         * for the browser-specific options. The method configures the capabilities by intercepting method calls
         * such as {@code addArguments}, {@code addPreference}, and {@code setCapability}, and applies the configuration
         * values defined in the specified configuration file.
         * <p>
         * This method is typically used internally by {@link #withDefaultOptions()} to generate the default capabilities
         * for each browser. It can also be extended or modified to suit different configuration needs.
         *
         * @param browserName   the name of the browser for which to create and configure the capabilities.
         * @param optionsConfig the configuration file containing the browser options.
         * @return the dynamically created and configured {@link MutableCapabilities} instance.
         */
        @SneakyThrows
        private MutableCapabilities createCapabilities(String browserName, Config optionsConfig) {
            return ByteBuddyUtils.createInstance(
                    MutableCapabilities.class,
                    new ConfigInvocationHandler(optionsConfig.getConfig("browserOptions." + browserName)),
                    ElementMatchers.named("addArguments")
                            .or(ElementMatchers.named("addPreference"))
                            .or(ElementMatchers.named("setCapability"))
            );
        }

        /**
         * This method is provided to override the default Lombok-generated setter for the {@code firstCall} variable.
         * It ensures that the {@code firstCall} variable remains immutable from external access by effectively preventing
         * any changes to its value through this method.
         *
         * <p>The method returns {@code this} to allow for method chaining, as typically expected in builder patterns.
         * However, since the method does not actually modify the {@code firstCall} variable, it is solely intended
         * to suppress the Lombok-generated setter and ensure that the value of {@code firstCall} is not altered externally.</p>
         *
         * @param firstCall the value intended to be set for the {@code firstCall} variable, which is ignored.
         * @return the current instance of the {@link BrowserLauncherBuilder} class, allowing for method chaining.
         */
        @SuppressWarnings("unused")
        BrowserLauncherBuilder firstCall(boolean firstCall) {
            return this;
        }
    }
}