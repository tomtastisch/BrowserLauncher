package org.browser.automation.core;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.browser.automation.core.BrowserDetector.BrowserInfo;
import org.browser.automation.exception.browser.BrowserManagerNotInitializedException;
import org.browser.automation.exception.browser.NoBrowserConfiguredException;
import org.browser.automation.utils.DriverUtils;
import org.browser.automation.utils.UrlUtil;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

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
    @Getter(AccessLevel.NONE)
    @Builder.Default
    private boolean useNewWindow = false;

    /**
     * Indicates whether this is the first call to the {@code open} method.
     * Initialized to {@code true} and used to differentiate between the initial call and subsequent calls.
     */
    @Builder.Default
    private boolean firstCall = true;

    private final List<BrowserDetector.BrowserInfo> browsers;

    private final List<String> urls;

    /**
     * Stores browser-specific {@link MutableCapabilities} options.
     *
     * <p>This map holds custom browser options (capabilities) for each browser managed by the {@link BrowserLauncher}.
     * The key in the map is the case-insensitive name of the browser (e.g., "chrome", "firefox"), and the value is a
     * {@link MutableCapabilities} object containing specific configurations for that browser.
     *
     * <p>You can configure these options using the {@link BrowserLauncherBuilder#withSameOptions(MutableCapabilities)}
     * method to apply the same settings to all browsers, or the {@link BrowserLauncherBuilder#withOptions(Map)} method
     * to apply specific settings per browser by name.
     *
     * <p>When a browser is started, the launcher applies the configured options. If no custom options are provided for a
     * particular browser, the launcher uses a default set of capabilities for that browser.
     *
     * <p>The map is initialized as a synchronized {@link TreeMap} with case-insensitive keys to ensure both
     * thread safety and case-insensitive browser name matching when accessed by multiple threads concurrently.
     */
    private Map<String, MutableCapabilities> options;

    @Synchronized
    public List<WebDriver> execute() {
        return browsers.stream()
                .flatMap(browser -> urls.stream().map(url -> open(browser, url, useNewWindow)))
                .peek(driver ->
                        log.info("Browser: {}, Session ID: {}, Tab: {}, URL: {}",
                                DriverUtils.getBrowserName(driver), DriverUtils.getSessionId(driver),
                                driver.getWindowHandle(), driver.getCurrentUrl())
                ).toList();
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
        boolean isBrowserNotExists = !manager.containsBrowser(browserInfo.name());
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
    private WebDriver open(BrowserInfo browserInfo, String link, WindowType type) throws ExecutionException, InterruptedException {
        WebDriver driver = handleBrowserOperation(browserInfo, type).get();

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
     *   <li>Calls the {@link BrowserManager#computeIfAbsent(BrowserInfo, MutableCapabilities, WindowType)} method on
     *       the `manager` to retrieve or create a WebDriver instance based on the `browserInfo`, capabilities, and `windowType`.</li>
     * </ol>
     *
     * <p>The method is synchronized to ensure thread safety when interacting with WebDriver management operations.</p>
     * @see BrowserInfo
     * @see WindowType
     * @see MutableCapabilities
     * @see WebDriver
     * @see BrowserManager#computeIfAbsent(BrowserInfo, MutableCapabilities, WindowType)
     */
    @Synchronized
    private CompletableFuture<WebDriver> handleBrowserOperation(BrowserInfo browserInfo, WindowType type) {
        log.info("Performing '{}' operation for driver: {}", type, browserInfo);

        MutableCapabilities capabilities = new MutableCapabilities();
        if (ObjectUtils.isNotEmpty(options)) {
            capabilities = options.getOrDefault(browserInfo.name().toLowerCase(), new MutableCapabilities());
        }

        // Retrieves or creates a ^new WebDriver instance for the specified browser
        return manager.computeIfAbsent(browserInfo, capabilities, type);
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
         * Configures the builder to use the default browser detected on the system.
         * <p>
         * This method utilizes the {@link BrowserDetector#getDefaultBrowserInfo(boolean)} method from the {@code BrowserDetector}
         * class to automatically detect and set the default browser for the system. The detection process examines the system's
         * configuration to identify the default web browser installed on the user's operating system. If the default browser cannot
         * be determined, the method employs a fallback mechanism to select the first available browser from the list of installed
         * browsers, as indicated by the {@code useFallBackBrowser=true} parameter.
         * <p>
         * The {@code BrowserDetector} class provides robust logic for identifying the system's default browser by inspecting
         * system-specific configurations, executing OS commands, and analyzing installed browsers. This detection process adapts
         * dynamically to various operating systems and environments.
         * <p>
         * If no browsers are detected or installed, and the fallback mechanism is triggered, the builder will select the first
         * available browser from the list of installed browsers as the default. Be aware that this could result in unexpected
         * behavior if the chosen fallback browser is not the intended default for the user's context.
         * <p>
         * Note: This method should be invoked before any other method that requires a browser to be set, such as
         * {@link #applyBrowserManager()}, to prevent {@link NoBrowserConfiguredException}.
         *
         * @return the current {@link BrowserLauncherBuilder} instance for method chaining.
         */
        @SneakyThrows
        public BrowserLauncherBuilder withDefaultBrowser() {
            if (Objects.isNull(manager)) {
                throw new BrowserManagerNotInitializedException();
            }

            return withBrowsers(Collections.singletonList(
                    manager.getBrowserDetector()
                            .getDefaultBrowserInfo(true)
                            .orElseThrow()
            ));
        }

        public BrowserLauncherBuilder autoCleanUp() {
            this.manager.updateCacheExpiration(10, TimeUnit.MINUTES);
            return this;
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
         * Configures the builder with a list of {@link BrowserInfo} objects.
         * <p>
         * This method takes a list of {@link BrowserInfo} objects, extracts their names, and uses the {@link #existingBrowsers(String...)}
         * method to filter the installed browsers based on these names. The filtered list of installed browsers is then set
         * for further configuration.
         * <p>
         * This method is useful for initializing the builder with a specific set of installed browsers, allowing you to
         * configure options or settings only for the browsers that are actually present.
         *
         * @param browsers a {@link List} of {@link BrowserInfo} objects representing the browsers to be used for configuration.
         * @return the current {@link BrowserLauncherBuilder} instance for method chaining.
         */
        private BrowserLauncherBuilder withBrowsers(List<BrowserInfo> browsers) {
            // Set the list of browsers based on the names extracted from the provided BrowserInfo objects
            this.browsers = existingBrowsers(browsers.stream().map(BrowserInfo::name).toArray(String[]::new));
            return this;
        }

        /**
         * Checks if the specified browsers are installed and returns information about the existing ones.
         * <p>
         * This method accepts a variable number of browser names as arguments, converts them to lowercase for case-insensitive comparison,
         * and filters the list of installed browsers to find matches. It then returns a list of {@link BrowserInfo} objects for the browsers
         * that are installed and match the provided names.
         * <p>
         * If none of the specified browsers are installed, an empty list is returned.
         *
         * @param browserNames the names of the browsers to check for existence. The comparison is case-insensitive.
         * @return a {@link List} of {@link BrowserInfo} objects representing the installed browsers that match the provided names.
         */
        public List<BrowserInfo> existingBrowsers(String... browserNames) {
            // Convert the provided browser names to lowercase for case-insensitive comparison
            List<String> names = Arrays.stream(browserNames)
                    .map(String::toLowerCase)
                    .toList();

            // Filter the list of installed browser information based on the provided names
            return manager.getBrowserDetector().getInstalledBrowserInfos()
                    .stream()
                    .filter(browser -> names.contains(browser.name().toLowerCase()))
                    .collect(Collectors.toList());
        }

        @SneakyThrows
        public BrowserLauncherBuilder applyBrowserManager() {
            return withManager(BrowserManager.getInstance());
        }

        /**
         * Applies the same {@link MutableCapabilities} to all managed browsers.
         *
         * <p>This method configures and applies the same {@link MutableCapabilities} to all browsers listed in the {@code browsers} field of
         * the {@link BrowserLauncher}. The capabilities are stored in the builder's internal options map, where each key
         * is the case-insensitive name of a browser (e.g., "chrome", "firefox"), and the value is the provided {@link MutableCapabilities}.
         *
         * <p>The method uses the list of browsers obtained from the {@code browsers} field and maps each browser name
         * to the provided capabilities, ensuring consistent configuration across all specified browsers.
         * If unique capabilities need to be applied to individual browsers, use the {@link #withOptions(Map)} method.
         *
         * <p>This is useful when you want to apply a uniform configuration to all browsers without specifying capabilities
         * for each one individually.
         *
         * @param capabilities the {@link MutableCapabilities} to be applied to all specified browsers.
         * @return the current {@link BrowserLauncherBuilder} instance, allowing for method chaining.
         */
        public BrowserLauncherBuilder withSameOptions(MutableCapabilities capabilities) {
            withOptions(this.browsers.stream().collect(Collectors.toMap(BrowserInfo::name, browser -> capabilities)));
            return this;
        }

        /**
         * Adds browser-specific {@link MutableCapabilities} options to the builder.
         *
         * <p>This method allows specifying a map of browser options (capabilities), where the key is the case-insensitive
         * name of the browser (e.g., "chrome", "firefox"), and the value is the corresponding {@link MutableCapabilities}.
         * The map is stored internally and is case-insensitive, meaning that browser names will be matched regardless
         * of their case (e.g., "Chrome" and "chrome" are treated the same).
         *
         * <p>To ensure thread safety, the option map is initialized as a synchronized {@link TreeMap} with
         * {@link String#CASE_INSENSITIVE_ORDER} if it is currently uninitialized (i.e., null). This ensures that the map can be
         * safely accessed and modified by multiple threads concurrently while ignoring case when matching browser names.
         *
         * <p>Once the map is initialized, the provided capabilities will be added to the map. If a key already exists (i.e.,
         * for a browser with the same name in any case format), its value will be overwritten.
         *
         * @param capabilities a map where the key is the case-insensitive browser name and the value is its corresponding {@link MutableCapabilities}.
         * @return the current {@link BrowserLauncherBuilder} instance, allowing for method chaining.
         */
        public BrowserLauncherBuilder withOptions(Map<String, MutableCapabilities> capabilities) {
            if (ObjectUtils.isEmpty(options)) {
                options = Collections.synchronizedMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
            }

            // Add all provided capabilities to the internal options map
            // If a key already exists, its value will be overwritten
            options.putAll(capabilities);

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
    }
}