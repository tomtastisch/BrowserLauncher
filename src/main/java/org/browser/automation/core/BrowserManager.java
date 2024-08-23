package org.browser.automation.core;

import com.typesafe.config.Config;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.access.cache.WebDriverCache;
import org.browser.automation.core.exception.WebDriverInitializationException;
import org.browser.automation.utils.OSUtils;
import org.browser.config.ConfigurationProvider;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.util.List;
import java.util.Optional;

/**
 * The {@code BrowserManager} class is responsible for managing browser operations,
 * including opening new windows or tabs. It uses the {@code WebDriverCache} to centrally manage
 * and retrieve {@code WebDriver} instances. This class follows the Singleton design pattern
 * to ensure a single instance is used throughout the application.
 *
 * <p>Key responsibilities of this class include:
 * <ul>
 *     <li>Opening a new browser window.</li>
 *     <li>Opening a new browser tab.</li>
 *     <li>Retrieving a cached WebDriver instance by session ID.</li>
 *     <li>Managing multiple browser instances efficiently using session IDs as keys.</li>
 * </ul>
 *
 * <p>The class provides flexibility for testability by allowing a custom {@code WebDriverCache}
 * instance to be injected, which can be useful when mocking or spying during unit tests.
 */
@Slf4j
public class BrowserManager {

    private final WebDriverCache webDriverCache;
    private final BrowserDetector browserDetector; // BrowserDetector as a class-level instance

    /**
     * Singleton Helper class for lazy-loading the Singleton instance.
     */
    private static class SingletonHelper {
        private static final BrowserManager INSTANCE = new BrowserManager(WebDriverCache.getInstance());
    }

    /**
     * Retrieves the Singleton instance of {@code BrowserManager} using the default {@code WebDriverCache}.
     *
     * @return the Singleton instance of {@code BrowserManager}.
     */
    public static BrowserManager getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Creates a new instance of {@code BrowserManager} with a custom {@code WebDriverCache}.
     * This is useful for injecting a mock or spy during testing.
     *
     * @param webDriverCache the {@code WebDriverCache} instance to be used by this {@code BrowserManager}.
     * @return a new instance of {@code BrowserManager} with the specified cache.
     */
    public static BrowserManager getInstance(WebDriverCache webDriverCache) {
        return new BrowserManager(webDriverCache);
    }

    /**
     * Private constructor to prevent external instantiation. Initializes the {@code BrowserManager}
     * with the specified {@code WebDriverCache} instance and a shared instance of {@code BrowserDetector}.
     *
     * @param webDriverCache the {@code WebDriverCache} instance to be used.
     */
    private BrowserManager(WebDriverCache webDriverCache) {
        this.webDriverCache = webDriverCache;
        this.browserDetector = new BrowserDetector(); // Single instance of BrowserDetector
    }

    /**
     * Opens a new browser window and returns the associated {@code WebDriver} instance.
     * If the driver is not found in the cache, a {@code WebDriverInitializationException} is thrown.
     * The driver is added to the cache using its session ID as the key.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @return the {@code WebDriver} instance associated with the new window.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewWindow(String driverName) throws WebDriverInitializationException {
        return handleBrowserOperation(driverName, "window");
    }

    /**
     * Opens a new browser tab by default, without opening a new window.
     * The driver is added to the cache using its session ID as the key.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @return the {@code WebDriver} instance associated with the new tab.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewTab(String driverName) throws WebDriverInitializationException {
        return openNewTab(driverName, false);
    }

    /**
     * Opens a new browser tab or a new window based on the provided flag.
     * If {@code openNewWindow} is true, a new window is opened; otherwise, a new tab is opened.
     * The driver is added to the cache using its session ID as the key.
     *
     * @param driverName    the name of the {@code WebDriver} instance.
     * @param openNewWindow if true, opens a new window; otherwise, opens a new tab.
     * @return the {@code WebDriver} instance associated with the operation.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewTab(String driverName, boolean openNewWindow) throws WebDriverInitializationException {
        return openNewWindow ? openNewWindow(driverName) : handleBrowserOperation(driverName, "tab");
    }

    /**
     * Opens multiple links in new tabs within an existing browser window.
     * Each tab is opened within the same browser instance.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @param links      a list of URLs to be opened.
     * @return the {@code WebDriver} instance used to open the tabs.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openLinksInTabs(String driverName, List<String> links) throws WebDriverInitializationException {
        WebDriver driver = getOrCreateDriver(driverName);
        links.forEach(link -> openLinkInNewWindowOrTab(driver, link, false));
        return driver; // Return the WebDriver instance
    }

    /**
     * Opens multiple links, each in a new window.
     * Each window is opened within the same browser instance.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @param links      a list of URLs to be opened.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public void openLinksInNewWindows(String driverName, List<String> links) throws WebDriverInitializationException {
        WebDriver driver = getOrCreateDriver(driverName);
        links.forEach(link -> openLinkInNewWindowOrTab(driver, link, true));
    }

    /**
     * Opens the same link in multiple browsers.
     * Each browser instance is identified by a different session ID.
     *
     * @param link        the URL to be opened.
     * @param driverNames a list of driver names representing different browsers.
     * @throws WebDriverInitializationException if any of the {@code WebDriver} instances could not be created or retrieved.
     */
    public void openLinkInMultipleBrowsers(String link, List<String> driverNames) throws WebDriverInitializationException {
        driverNames.stream().map(this::getOrCreateDriver)
                .forEach(driver -> openLinkInNewWindowOrTab(driver, link, false));
    }

    /**
     * Handles the opening of a link in either a new tab or a new window.
     * The link is loaded into the specified browser instance.
     *
     * @param driver      the {@code WebDriver} instance to be used.
     * @param link        the URL to be opened.
     * @param inNewWindow if true, opens the link in a new window; otherwise, in a new tab.
     */
    private void openLinkInNewWindowOrTab(WebDriver driver, String link, boolean inNewWindow) {
        driver.switchTo().newWindow(inNewWindow ? WindowType.WINDOW : WindowType.TAB);
        driver.get(link);
    }

    /**
     * Retrieves the cached {@code WebDriver} instance associated with the given session ID.
     * If the session ID is not found, a new driver is created.
     *
     * @param sessionId the session ID of the {@code WebDriver} instance.
     * @return the cached or newly created {@code WebDriver} instance.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver getWebDriver(String sessionId) throws WebDriverInitializationException {
        return Optional.ofNullable(webDriverCache.getDriverBySessionId(sessionId))
                .orElseThrow(() -> {
                    log.error("No WebDriver found for session ID: {}", sessionId);
                    return new WebDriverInitializationException("No WebDriver found for session ID: " + sessionId);
                });
    }

    /**
     * Retrieves or creates a {@code WebDriver} instance based on the provided driver name.
     * If a cached instance exists, it is returned; otherwise, a new instance is created,
     * added to the cache, and returned.
     *
     * @param driverName the name of the {@code WebDriver} instance.
     * @return the cached or newly created {@code WebDriver} instance.
     */
    @SneakyThrows
    private WebDriver getOrCreateDriver(String driverName) {
        WebDriver driver = createWebDriver(driverName);
        String sessionId = ((RemoteWebDriver) driver).getSessionId().toString();
        webDriverCache.addDriver(driver); // Add the driver to the cache with the session ID as the key
        return driver;
    }


    /**
     * Creates a new {@code WebDriver} instance based on the provided browser name.
     *
     * <p>This method dynamically identifies the appropriate {@code WebDriver} class
     * based on the browser configuration retrieved from the {@code BrowserDetector}.
     * It leverages a stream to filter through the list of installed browsers and finds
     * the one that matches the specified {@code driverName}. If a match is found, the
     * corresponding {@code WebDriver} class is instantiated using reflection, with exception
     * handling performed using Lombok's {@code @SneakyThrows} annotation.</p>
     *
     * <p>If no browser matches the provided {@code driverName}, the method throws a
     * {@code WebDriverInitializationException} indicating that the specified browser is either
     * unsupported or unavailable on the system.</p>
     *
     * <p>Key steps in this method include:
     * <ul>
     *     <li>Using the {@code BrowserDetector} to retrieve a list of installed browsers.</li>
     *     <li>Filtering the list to find a browser that matches the given {@code driverName}, ignoring case.</li>
     *     <li>Instantiating the appropriate {@code WebDriver} class using reflection.</li>
     *     <li>Handling errors gracefully using the {@code @SneakyThrows} annotation to avoid explicit try-catch blocks.</li>
     * </ul>
     *
     * @param driverName the name of the browser (e.g., "chrome", "firefox", "edge").
     * @return a new instance of the corresponding {@code WebDriver} for the specified browser.
     * @throws WebDriverInitializationException if the specified browser is unsupported or unavailable,
     *                                          or if the {@code WebDriver} instance cannot be created.
     */
    private WebDriver createWebDriver(String driverName) throws WebDriverInitializationException {
        return browserDetector.getInstalledBrowsers().stream()
                .filter(browser -> browser.name().equalsIgnoreCase(driverName))
                .findFirst()
                .map(this::instantiateDriverSneaky)
                .orElseThrow(() -> new WebDriverInitializationException("Unsupported or unavailable browser: " + driverName));
    }

    /**
     * Instantiates the {@code WebDriver} for the provided {@code BrowserInfo} using reflection.
     *
     * <p>This method is used to dynamically create an instance of the appropriate
     * {@code WebDriver} class based on the configuration found in the {@code BrowserInfo}.
     * The method is annotated with Lombok's {@code @SneakyThrows}, which automatically
     * handles any {@code ReflectiveOperationException} that may occur during the instantiation
     * process. This allows the method to remain concise without requiring explicit try-catch blocks.</p>
     *
     * <p>The method is called within the stream processing pipeline of {@code createWebDriver}
     * and directly maps the {@code BrowserInfo} to a new {@code WebDriver} instance.</p>
     *
     * @param browserInfo the {@code BrowserInfo} containing the relevant configuration for the browser.
     * @return a new instance of the appropriate {@code WebDriver} for the specified browser.
     */
    @SneakyThrows
    private WebDriver instantiateDriverSneaky(BrowserDetector.BrowserInfo browserInfo) {
        return browserDetector.instantiateDriver(browserInfo.driverClass());
    }

    /**
     * Handles the common logic for browser operations (like opening new windows or tabs).
     * Logs the operation being performed and retrieves the corresponding {@code WebDriver} instance
     * from the cache. If the driver is not found, an exception is thrown.
     *
     * @param driverName    the name of the {@code WebDriver} instance.
     * @param operationType the type of operation being performed (e.g., "window" or "tab").
     * @return the {@code WebDriver} instance associated with the operation.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    private WebDriver handleBrowserOperation(String driverName, String operationType) throws WebDriverInitializationException {
        log.info("Performing '{}' operation for driver: {}", operationType, driverName);
        return getOrCreateDriver(driverName);
    }
}