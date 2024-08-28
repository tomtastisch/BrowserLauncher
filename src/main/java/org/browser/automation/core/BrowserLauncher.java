package org.browser.automation.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.exception.WebDriverInitializationException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.util.List;

/**
 * The {@code BrowserLauncher} class provides functionality for managing browser operations,
 * such as opening new windows, tabs, and handling multiple links across different browsers.
 * It interacts with the {@code BrowserManager} to retrieve or create {@code WebDriver} instances.
 * This class does not enforce a singleton pattern, allowing multiple instances if needed.
 *
 * <p>Key responsibilities of this class include:
 * <ul>
 *     <li>Opening new browser windows or tabs.</li>
 *     <li>Opening multiple links in tabs or windows.</li>
 *     <li>Handling links across multiple browsers.</li>
 * </ul>
 *
 * <p>Instances of this class can be created using the default constructor or by providing a
 * {@code BrowserManager} instance for greater control over browser management.
 */
@Slf4j
@Getter
public class BrowserLauncher {

    private final BrowserManager manager;

    /**
     * Constructs a new {@code BrowserLauncher} instance using the default {@code BrowserManager} instance.
     * This constructor allows for default behavior with a shared browser manager.
     */
    public BrowserLauncher() {
        this(BrowserManager.getInstance());
    }

    /**
     * Constructs a new {@code BrowserLauncher} instance using the provided {@code BrowserManager}.
     * This constructor allows for a custom {@code BrowserManager} to be used, which can be useful
     * for testing or specific configurations.
     *
     * @param manager the {@code BrowserManager} instance to be used by this {@code BrowserLauncher}.
     */
    public BrowserLauncher(BrowserManager manager) {
        this.manager = manager;
    }

    /**
     * Opens a new browser window and returns the associated {@code WebDriver} instance.
     * If the driver is not found in the cache, a {@code WebDriverInitializationException} is thrown.
     * The driver is added to the cache using its session ID as the key.
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
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
     * @param driverName the name of the {@code WebDriver} instance to be used.
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
     * @param driverName    the name of the {@code WebDriver} instance to be used.
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
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @param links      a list of URLs to be opened in new tabs.
     * @return the {@code WebDriver} instance used to open the tabs.nstance could not be created or retrieved.
     */
    public WebDriver openLinksInTabs(String driverName, List<String> links) {
        WebDriver driver = manager.getOrCreateDriver(driverName);
        links.forEach(link -> openLinkInNewWindowOrTab(driver, link, false));
        return driver; // Return the WebDriver instance
    }

    /**
     * Opens multiple links, each in a new window.
     * Each window is opened within the same browser instance.
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @param links      a list of URLs to be opened in new windows.
     */
    public void openLinksInNewWindows(String driverName, List<String> links) {
        WebDriver driver = manager.getOrCreateDriver(driverName);
        links.forEach(link -> openLinkInNewWindowOrTab(driver, link, true));
    }

    /**
     * Opens the same link in multiple browsers.
     * Each browser instance is identified by a different session ID.
     *
     * @param link        the URL to be opened in multiple browsers.
     * @param driverNames a list of driver names representing different browsers.
     * @throws WebDriverInitializationException if any of the {@code WebDriver} instances could not be created or retrieved.
     */
    public void openLinkInMultipleBrowsers(String link, List<String> driverNames) {
        driverNames.stream().map(manager::getOrCreateDriver)
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
     * Handles the common logic for browser operations, such as opening new windows or tabs.
     * Logs the operation being performed and retrieves the corresponding {@code WebDriver} instance
     * from the {@code BrowserManager}. If the driver is not found, an exception is thrown.
     *
     * @param driverName    the name of the {@code WebDriver} instance to be used.
     * @param operationType the type of operation being performed (e.g., "window" or "tab").
     * @return the {@code WebDriver} instance associated with the operation.
     */
    private WebDriver handleBrowserOperation(String driverName, String operationType) {
        log.info("Performing '{}' operation for driver: {}", operationType, driverName);
        return manager.getOrCreateDriver(driverName);
    }
}