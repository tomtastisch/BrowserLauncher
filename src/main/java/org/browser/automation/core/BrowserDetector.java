package org.browser.automation.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.browser.automation.core.exception.PackageNotFoundException;
import org.browser.automation.core.exception.WebdriverNotFoundException;
import org.browser.automation.utils.OSUtils;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.WebDriver;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The {@code WebDriverResolver} class provides a method to dynamically resolve the appropriate {@code WebDriver} class
 * based on a given class name. The method utilizes reflection to search and identify the correct class within the package
 * structure, with the results being cached for more efficient subsequent lookups.
 *
 * <p>This implementation is designed to be flexible, allowing for the resolution of {@code WebDriver} classes even if
 * they are part of a complex package structure. It also includes caching capabilities to improve performance by avoiding
 * redundant reflection scans.</p>
 *
 * <p><b>How the Method Works:</b></p>
 * <ol>
 *   <li><b>Caching:</b> The resolved {@code WebDriver} classes are stored in a static, thread-safe cache. If the class
 *   has already been resolved before, it is retrieved from the cache without performing a new scan.</li>
 *   <li><b>Package Resolution:</b> The method dynamically determines the package path by analyzing the segments of the
 *   class name. This ensures that even if the full package path isn't provided, the correct path can be derived and scanned.</li>
 *   <li><b>Reflection Scanning:</b> The Reflections library is used to scan the identified package for classes implementing
 *   {@code WebDriver}. This scan is limited to the relevant package to optimize performance.</li>
 *   <li><b>Fallback Mechanism:</b> If no exact match is found for the class name, the method attempts to retrieve the first
 *   available {@code WebDriver} class within the identified package.</li>
 * </ol>
 *
 * <p><b>Key Benefits:</b></p>
 * <ul>
 *   <li>Improved Performance: The caching mechanism ensures that reflection scans are only performed once for each unique class name.</li>
 *   <li>Flexibility: The package path is dynamically determined, allowing for a more adaptive resolution process.</li>
 *   <li>Thread-Safe: The use of a {@code ConcurrentHashMap} ensures that the cache is safe for use in multi-threaded environments.</li>
 * </ul>
 *
 * <p><b>Note:</b> This method uses the {@code @SneakyThrows} annotation from Lombok to handle any checked exceptions,
 * such as {@code ClassNotFoundException} or {@code ReflectiveOperationException}, that may arise during the reflection
 * process. These exceptions are not expected to occur under normal usage.</p>
 */
@Slf4j
public class BrowserDetector {

    private final Config config;

    /**
     * Cache to store previously resolved {@code WebDriver} classes.
     * The key is the fully qualified class name (FQCN), and the value is the corresponding {@code WebDriver} class.
     */
    private static final Map<String, Class<? extends WebDriver>> cache = new ConcurrentHashMap<>();

    public BrowserDetector() {
        this(new ConfigurationProvider());
    }

    /**
     * Constructs the {@code BrowserDetector} instance with the given configuration provider.
     *
     * @param configProvider the configuration provider that supplies the application configuration
     */
    public BrowserDetector(ConfigurationProvider configProvider) {
        this.config = configProvider.getConfig();
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
        List<BrowserInfo> installedBrowsers = getInstalledBrowsers();
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
    private Optional<BrowserInfo> findDefaultBrowser(List<BrowserInfo> installedBrowsers) {
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
                .flatMap(line -> installedBrowsers.stream().filter(browser -> line.equalsIgnoreCase(browser.name)))
                .reduce((a, b) -> b); // Returns the last match if there are multiple
    }

    /**
     * Retrieves a list of all installed browsers based on the current operating system.
     *
     * <p>This method reads browser configurations from the application configuration and
     * determines which browsers are installed by checking the existence of the executable paths.
     * It returns a list of {@code BrowserInfo} objects for each detected browser.</p>
     *
     * @return A list of {@code BrowserInfo} objects representing installed browsers.
     * If no browsers are detected or if the configuration is missing, an empty list is returned.
     */
    public List<BrowserInfo> getInstalledBrowsers() {
        try {
            return config.getConfigList("osBrowserPaths." + OSUtils.OS_KEY).stream()
                    .filter(browser -> Files.exists(Paths.get(browser.getString("path"))))
                    .map(BrowserDetector::getBrowserInfo)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());
        } catch (ConfigException.Missing e) {
            log.error("Configuration for OS {} is missing.", OSUtils.OS_KEY, e);
            return List.of();
        }
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
    static Optional<BrowserInfo> getBrowserInfo(Config browser) {
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
    protected synchronized static Class<? extends WebDriver> getDriverClass(String driverClassName) {
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
        Set<Class<? extends WebDriver>> subTypes = new Reflections(packagePath, Scanners.SubTypes).getSubTypesOf(WebDriver.class);

        // Attempt to find the exact class match or fall back to the first available class
        return subTypes.stream()
                .filter(clazz -> clazz.getName().equals(driverClassName))
                .findFirst()
                .or(() -> subTypes.stream().findFirst())
                .orElseThrow(() -> driverClassName.contains(".")
                        ? new WebdriverNotFoundException(driverClassName)
                        : new PackageNotFoundException(packagePath));
    }

    /**
     * Instantiates the WebDriver based on the provided driver class.
     * The method dynamically creates an instance of the WebDriver using reflection.
     *
     * @param driverClass the {@code Class} of the WebDriver to instantiate
     * @return an instance of the specified WebDriver
     */
    @SneakyThrows
    protected WebDriver instantiateDriver(Class<? extends WebDriver> driverClass) {
        return driverClass.getDeclaredConstructor().newInstance();
    }
}