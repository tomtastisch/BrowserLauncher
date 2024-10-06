package org.browser.automation.core;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.SystemUtils;
import org.browser.automation.exception.browser.driver.WebdriverNotFoundException;
import org.browser.automation.exception.custom.PackageNotFoundException;
import org.browser.automation.utils.DriverUtils;
import org.browser.automation.utils.OSUtils;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WindowType;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@code BrowserDetector} class is responsible for detecting and managing browser-related configurations
 * and dynamically resolving WebDriver instances for automation tasks. It leverages the configuration management
 * provided by the {@link ConfigurationProvider} class and uses reflection to scan and resolve classes based
 * on the detected browser information.
 *
 * <h2>Core Responsibilities:</h2>
 * <ul>
 *   <li>Determine the default browser based on the system configuration.</li>
 *   <li>Manage browser configurations and resolve corresponding WebDriver instances.</li>
 *   <li>Cache previously resolved WebDriver classes to enhance performance and avoid redundant reflection scans.</li>
 * </ul>
 *
 * <h2>Configuration Management:</h2>
 * The class loads configurations using the {@link ConfigurationProvider} class, which provides
 * a flexible and thread-safe way to manage multiple configurations. The primary configuration file
 * loaded is "application.conf".
 *
 * <p>Browser-related configurations, such as paths to browser executables and the associated WebDriver classes,
 * are managed within the configuration file. For more details, refer to the configuration file documentation
 * where these settings are defined.</p>
 *
 * <h3>Reference to Configuration:</h3>
 * For the complete configuration, including browser paths and WebDriver class mappings, please refer to the
 * respective configuration file (e.g., "application.conf") located in the resources folder.
 *
 * <p><b>Note:</b> The configuration is dynamically loaded based on the detected operating system (Windows, macOS, Linux),
 * and the path is constructed using {@link OSUtils#OS_KEY}.</p>
 */
@Slf4j
@Getter
@ThreadSafe
public class BrowserDetector {


    private List<BrowserInfo> installedBrowsers;
    /**
     * The {@code Config} object representing the loaded configuration settings for the {@code BrowserDetector}.
     * This configuration is used to manage various browser-related settings, such as paths to browser executables,
     * and WebDriver class mappings. The configuration is loaded from the "application.conf" file and is designed
     * to be thread-safe, ensuring that it can be accessed and modified concurrently without issues.
     */
    private final Config config;

    /**
     * The temporary directory path used by {@code WebDriverManager} to store downloaded WebDriver binaries.
     * This path is located in the system's default temporary directory, as determined by
     * {@code SystemUtils.getJavaIoTmpDir()}. The purpose of this directory is to cache WebDriver binaries so
     * that they can be reused across multiple sessions, improving performance by avoiding redundant downloads.
     */
    private final String cacheDirectory = new File(SystemUtils.getJavaIoTmpDir(), "webdriver-cache").getAbsolutePath();

    /**
     * A concurrent cache that stores previously resolved {@code WebDriver} classes, mapped by their fully qualified
     * class names (FQCN). This cache enhances performance by avoiding repeated reflection scans for the same WebDriver
     * class. The cache is thread-safe, allowing concurrent access in multithreaded environments without causing
     * inconsistencies or data corruption.
     */
    private static final Map<String, Class<? extends WebDriver>> cache = new ConcurrentHashMap<>();

    /**
     * Constructs the {@code BrowserDetector} instance with the configuration loaded from the {@code application.conf} file.
     * The configuration is retrieved using {@link ConfigurationProvider}, ensuring that it is loaded in a thread-safe
     * and efficient manner. This constructor specifically loads the configuration associated with the detected OS key.
     *
     * <p>It is designed to be the default constructor that provides a ready-to-use configuration based on the
     * current operating system environment, making it easy to integrate and use in typical scenarios.</p>
     *
     * <p>Internally, this constructor delegates the configuration loading to {@link ConfigurationProvider},
     * which manages the caching and thread-safe retrieval of configuration files.</p>
     */
    public BrowserDetector() {
        this(ConfigurationProvider.getInstance("application").getConfig());
    }

    /**
     * Constructs the {@code BrowserDetector} instance with a custom {@code Config} object. This constructor is useful
     * when you want to provide a specific configuration programmatically, bypassing the default {@code application.conf}.
     *
     * <p>This is particularly beneficial in testing scenarios, where you might want to inject mock configurations, or
     * when you need to dynamically load configurations based on specific conditions in your application.</p>
     *
     * @param config The {@code Config} object that contains the configuration settings for the {@code BrowserDetector}.
     *               This can be either a custom configuration or one loaded from a different source.
     */
    public BrowserDetector(Config config) {
        this.config = config;
    }

    /**
     * A record that holds browser-related information such as the browser name, executable path,
     * and the associated WebDriver class. This record is used to encapsulate the essential browser
     * details required for automation.
     */
    @Builder
    public record BrowserInfo(
            @JsonProperty("name") String name,
            @JsonProperty("path") String path,
            @JsonProperty("driverClass") Class<? extends WebDriver> driverClass
    ) {
        /**
         * Custom constructor for the {@code BrowserInfo} record that enables JSON deserialization using Jackson.
         *
         * @param name        the name of the browser (e.g., "Chrome", "Firefox")
         * @param path        the path to the browser executable
         * @param driverClass the WebDriver class associated with the browser
         */
        @JsonCreator
        public BrowserInfo {
            // The @JsonCreator and @JsonProperty annotations enable Jackson to correctly deserialize the record
        }
    }

    /**
     * Retrieves the default browser name without considering fallback options.
     * <br>
     * This method returns the name of the default browser without using a fallback browser.
     * It is a convenience method that calls `getDefaultBrowserName(false)` to obtain the default browser name.
     *
     * @return The name of the default browser, or an empty string if no default browser is found.
     */
    public String getDefaultBrowserName() {
        return getDefaultBrowserName(false);
    }

    /**
     * Retrieves the default browser name based on the given fallback option.
     * <br>
     * This method returns the name of the default browser. If the `useFallBackBrowser` parameter is set to true,
     * it will consider a fallback browser if the primary default browser is not available.
     * If no default browser is found, it will return an empty string.
     *
     * @param useFallBackBrowser A boolean indicating whether to use a fallback browser if the primary default browser is unavailable.
     * @return The name of the default browser, or an empty string if no default browser is found.
     */
    public String getDefaultBrowserName(boolean useFallBackBrowser) {
        BrowserInfo defBrowser = new BrowserInfo("", "", null);
        return getDefaultBrowserInfo(useFallBackBrowser).orElse(defBrowser).name();
    }

    /**
     * Retrieves the system's default browser information without a fallback option.
     * This method attempts to determine the default browser configured on the operating system
     * and returns its corresponding {@code BrowserInfo} wrapped in an {@code Optional}.
     * If the default browser cannot be determined, no fallback is provided, and the method
     * returns an empty {@code Optional}.
     *
     * <p>This method internally delegates to {@link #getDefaultBrowserInfo(boolean)} with
     * the {@code useFallbackBrowser} parameter set to {@code false}, meaning no fallback
     * browser is used if the default browser cannot be identified.</p>
     *
     * @return An {@code Optional<BrowserInfo>} containing the default browser information,
     * or an empty {@code Optional} if no match is found and no fallback is used.
     */
    public Optional<BrowserInfo> getDefaultBrowserInfo() {
        return getDefaultBrowserInfo(false);
    }

    /**
     * Retrieves the default browser information for the system, with an optional fallback mechanism.
     *
     * <p>This method attempts to determine the default web browser configured on the user's operating system.
     * It does so by first fetching the list of installed browsers and then trying to identify the default browser
     * using a system command. If the default browser cannot be determined and the fallback option is enabled,
     * the method returns the first available browser from the list of installed browsers.</p>
     *
     * <p>Usage Scenarios:</p>
     * <ul>
     *     <li>Primary: Identify the system's default web browser.</li>
     *     <li>Fallback: If the system's default browser cannot be identified, optionally use the first detected browser.</li>
     * </ul>
     *
     * @param useFallbackBrowser A flag indicating whether to fall back to the first available browser
     *                           if the default browser cannot be identified.
     * @return An {@code Optional<BrowserInfo>} containing the default browser information,
     * or the first available browser if the fallback is enabled. If neither is found, an empty {@code Optional} is returned.
     */
    public Optional<BrowserInfo> getDefaultBrowserInfo(boolean useFallbackBrowser) {
        List<BrowserInfo> installedBrowsers = getInstalledBrowserInfos();
        Optional<BrowserInfo> defaultBrowser = findDefaultBrowser(installedBrowsers);

        // Return the default browser if found, or use fallback if requested
        return defaultBrowser.or(() -> useFallbackBrowser ? installedBrowsers.stream().findFirst() : Optional.empty());
    }

    /**
     * Attempts to find the system's default web browser by executing a system-specific command.
     *
     * <p>This method runs a system command to determine the default web browser, parses the output,
     * and compares it against the list of installed browsers to find a match. If a match is found, it is returned.
     * If the command execution fails or no match is found, an empty {@code Optional} is returned.</p>
     *
     * <p>Key Steps:</p>
     * <ul>
     *     <li>Run a command specific to the operating system to determine the default browser.</li>
     *     <li>Parse the output to extract the browser information.</li>
     *     <li>Compare the parsed output with the list of installed browsers.</li>
     * </ul>
     *
     * @param installedBrowsers The list of browsers detected as installed on the system.
     * @return An {@code Optional<BrowserInfo>} containing the detected default browser,
     * or an empty {@code Optional} if no match is found.
     */
    Optional<BrowserInfo> findDefaultBrowser(List<BrowserInfo> installedBrowsers) {
        try {
            Process process = Runtime.getRuntime().exec(OSUtils.getDefaultBrowserCommand());
            if (process.waitFor() == 0) {
                String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
                return parseBrowserFromOutput(output, installedBrowsers);
            }
        } catch (Exception e) {
            log.error("Error while executing OS command: {}", OSUtils.getDefaultBrowserCommand(), e);
        }
        return Optional.empty();
    }

    /**
     * Retrieves a list of installed browsers from the configured browsers based on the current operating system.
     *
     * <p>This method reads browser configurations from the application configuration and filters out the browsers
     * that are installed by checking the existence of their executable paths. It returns a list of {@code BrowserInfo}
     * objects for each detected and installed browser that is listed in the configuration.</p>
     *
     * <p>For details about how the browser paths and WebDriver classes are configured, please refer to the
     * configuration file (e.g., "application.conf") in your project.</p>
     *
     * @return A list of {@code BrowserInfo} objects representing the installed browsers that match the configuration.
     * If no installed browsers are detected or if the configuration is missing, an empty list is returned.
     */
    protected List<BrowserInfo> getInstalledBrowserInfos() {
        if (ObjectUtils.isEmpty(installedBrowsers)) {
            installedBrowsers = getBrowserInfos(browser -> Files.exists(Paths.get(browser.getString("path"))));
        }
        return installedBrowsers;
    }

    /**
     * Retrieves a list of {@code BrowserInfo} objects based on a filtering predicate.
     * <br>
     * This method reads browser configurations from the application configuration, applies the specified filter,
     * and returns a list of {@code BrowserInfo} objects. If no filter is applied, all configured browsers are included.
     *
     * @param filter A {@code Predicate<Config>} used to filter the browsers. Only those configurations that match
     *               the predicate will be included in the result. If the filter is {@code null}, all configured browsers
     *               are included.
     * @return A {@code List<BrowserInfo>} containing information about browsers that match the filter criteria.
     */
    private List<BrowserInfo> getBrowserInfos(Predicate<? super Config> filter) {
        return config.getConfigList("osBrowserPaths." + OSUtils.OS_KEY).stream()
                .filter(ObjectUtils.isNotEmpty(filter) ? filter : e -> true)
                .map(this::getBrowserInfo)
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Parses the output of the system command to find the default browser from the list of installed browsers.
     *
     * <p>This method takes the raw output from the system command, processes each line, and attempts to match
     * it against the names of the installed browsers. The matching logic is case-insensitive. If multiple matches
     * are found, the last one is returned, ensuring the most recent match is used.</p>
     *
     * @param output            The output from the system command determining the default browser.
     * @param installedBrowsers The list of installed browsers to compare against.
     * @return An {@code Optional<BrowserInfo>} containing the detected browser from the parsed output,
     * or an empty {@code Optional} if no match is found.
     */
    private Optional<BrowserInfo> parseBrowserFromOutput(String output, List<BrowserInfo> installedBrowsers) {
        return output.lines()
                .map(String::trim)
                .flatMap(line -> installedBrowsers.stream().filter(browser ->
                        line.equalsIgnoreCase(browser.name))
                ).reduce((a, b) -> b); // Returns the last match if there is multiple
    }

    /**
     * Resolves the {@code BrowserInfo} based on the provided browser configuration.
     * This method dynamically loads the WebDriver class specified in the configuration and
     * creates a {@code BrowserInfo} object containing the browser's name, path, and WebDriver class.
     *
     * <p>The method uses {@code @SneakyThrows} to avoid handling {@code ClassNotFoundException}
     * explicitly, as this exception is not expected to occur during regular use.</p>
     *
     * @param browser The {@code Config} object representing the browser configuration.
     * @return An {@code Optional} containing the {@code BrowserInfo} if the WebDriver class can be resolved.
     */
    @SneakyThrows
    private Optional<BrowserInfo> getBrowserInfo(Config browser) {
        String path = browser.getString("path");
        String driverClassName = browser.getString("driverClass");
        Class<? extends WebDriver> driverClass = getDriverClass(driverClassName);

        return Optional.of(new BrowserInfo(
                browser.getString("name"),
                path,
                driverClass
        ));
    }

    /**
     * Resolves and retrieves a {@link WebDriver} class based on the provided class name.
     * This method uses a cache to store previously resolved WebDriver classes to optimize performance
     * and reduce redundant reflection scans. The resolution process is synchronized to ensure
     * thread safety in concurrent environments.
     *
     * <p><b>Workflow:</b></p>
     * <ol>
     *     <li>First, the cache is checked to see if the class has already been resolved.</li>
     *     <li>If the class is found in the cache, it is immediately returned, avoiding further computation.</li>
     *     <li>If the class is not in the cache, the package path is derived based on the class name.</li>
     *     <li>A reflection scan is performed within the derived package path to find the WebDriver class.</li>
     *     <li>The resolved class is then stored in the cache for future queries.</li>
     * </ol>
     *
     * <p><b>Key Features:</b></p>
     * <ul>
     *     <li>The method is synchronized to ensure that only one thread can resolve a WebDriver class at a time.</li>
     *     <li>The cache is managed using {@code computeIfAbsent}, which ensures that the computation is only performed once, even under concurrent access.</li>
     *     <li>Reflection is scoped to the derived package path to improve efficiency by limiting the scanning range.</li>
     * </ul>
     *
     * <p><b>Example Usage:</b></p>
     * <pre>
     * {@code
     * Class<? extends WebDriver> driverClass = BrowserDetector.getDriverClass("org.openqa.selenium.chrome.ChromeDriver");
     * }
     * </pre>
     *
     * @param driverClassName The fully qualified name of the WebDriver class (e.g., "org.openqa.selenium.chrome.ChromeDriver").
     * @return The resolved {@link WebDriver} class associated with the specified driver class name.
     */
    private synchronized Class<? extends WebDriver> getDriverClass(String driverClassName) {
        log.info("Resolving WebDriver class for: {}", driverClassName);

        // Retrieve the class from the cache or compute it if not present
        return cache.computeIfAbsent(driverClassName, key -> {
            // Derive the package path based on the class name
            String packagePath = derivePackagePath(driverClassName);

            // Perform a reflection scan within the derived package path to resolve the WebDriver class
            return resolveWebDriverClass(driverClassName, packagePath);
        });
    }

    /**
     * Derives the package path by analyzing segments of the given class name.
     * The method iteratively combines the segments and scans the resulting package paths
     * to determine where relevant WebDriver classes are located.
     *
     * @param driverClassName The fully qualified name of the WebDriver class.
     * @return The derived package path where the WebDriver class is likely located, or null if no valid path is found.
     */
    private static String derivePackagePath(String driverClassName) {
        return Arrays.stream(driverClassName.split("\\."))
                .reduce((acc, part) -> {
                    String currentPath = acc.isEmpty() ? part : acc + "." + part;
                    return !new Reflections(currentPath, Scanners.SubTypes).getSubTypesOf(WebDriver.class).isEmpty()
                            ? currentPath
                            : acc;
                }).orElse(null);
    }

    /**
     * Resolves the WebDriver class by performing a reflection scan within the specified package path.
     * The method attempts to find an exact match for the class name. If no exact match is found,
     * it falls back to returning the first available WebDriver class in the package.
     * <br>
     * If the class name contains a dot (.), it is assumed that a fully qualified class name is provided.
     * If no match is found, an exception specific to the situation is thrown.
     *
     * @param driverClassName The fully qualified name of the WebDriver class.
     * @param packagePath     The derived package path where the class is expected to be found.
     * @return The resolved WebDriver class.
     */
    @SneakyThrows
    private static Class<? extends WebDriver> resolveWebDriverClass(String driverClassName, String packagePath) {
        Set<Class<? extends WebDriver>> subTypes = new Reflections(packagePath, Scanners.SubTypes)
                .getSubTypesOf(WebDriver.class);

        // Attempt to find the exact class match or fall back to the first available class
        return subTypes.stream()
                .filter(clazz -> clazz.getName().equals(driverClassName))
                .findFirst()
                .or(() -> subTypes.stream().findFirst())
                .orElseThrow(() -> driverClassName.contains(".")
                        ? new WebdriverNotFoundException(driverClassName)
                        : new PackageNotFoundException(packagePath));
    }

    public WebDriver instantiateDriver(BrowserInfo browserInfo,
                                       @Nullable MutableCapabilities capabilities,
                                       WindowType type) {
        return configureAndLaunchWebDrivers(browserInfo, capabilities, type);
    }

    /**
     * Configures and launches a WebDriver instance based on the provided `BrowserInfo`, capabilities, and window type.
     *
     * <p>This method sets up the WebDriver by configuring it with the specified capabilities and ensuring that the required
     * driver binaries are downloaded and ready for use. It uses the `WebDriverManager` to handle driver binaries and their
     * setup, and then creates and returns a `WebDriver` instance using the `DriverUtils.createWebDriverInstance` method.</p>
     *
     * @param browserInfo  Information about the browser, including the WebDriver class and any other relevant details.
     * @param capabilities Optional capabilities to be set on the WebDriver, such as browser options or configurations.
     * @param type         The type of window (`WindowType`) indicating whether a new tab or window should be created.
     * @return An instance of `WebDriver` configured and launched based on the provided parameters.
     * @throws WebDriverException If an error occurs during the setup or instantiation of the WebDriver.
     *
     * <p>The method performs the following steps:</p>
     * <ol>
     *   <li>Initializes a `WebDriverManager` instance for the specified WebDriver class.</li>
     *   <li>Sets the cache path for the WebDriver binaries to the specified `cacheDirectory`.</li>
     *   <li>Forces the download of the WebDriver binaries, ensuring that the latest version is used.</li>
     *   <li>Sets the provided capabilities on the WebDriver manager.</li>
     *   <li>Executes the setup process to prepare the WebDriver binaries for use.</li>
     *   <li>Calls `DriverUtils.createWebDriverInstance` to create and return an instance of `WebDriver` with the specified configurations and window type.</li>
     * </ol>
     *
     * <p>The method is synchronized to ensure thread safety when configuring and launching WebDriver instances.</p>
     * @see WebDriverManager
     * @see DriverUtils#createWebDriverInstance(BrowserInfo, MutableCapabilities, WindowType)
     */
    @SneakyThrows
    @Synchronized
    private WebDriver configureAndLaunchWebDrivers(BrowserInfo browserInfo,
                                                   @Nullable MutableCapabilities capabilities,
                                                   WindowType type) throws WebDriverException {

        // Initialize WebDriverManager for the specified WebDriver class.
        WebDriverManager manager = WebDriverManager.getInstance(browserInfo.driverClass())
                .cachePath(cacheDirectory) // Set the path where WebDriver binaries are cached.
                .forceDownload(); // Force download of the WebDriver binaries, ensuring the latest version is used.

        // Set the specified capabilities on the WebDriver manager.
        manager.capabilities(capabilities);
        // Perform setup to prepare the WebDriver binaries for use.
        manager.setup();

        // Create and return an instance of WebDriver using the provided configuration and window type.
        return DriverUtils.createWebDriverInstance(browserInfo, capabilities, type);
    }
}