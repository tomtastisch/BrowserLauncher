package org.browser.automation.core;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.exception.WebDriverInitializationException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
@Setter
public class BrowserLauncher {

    private final BrowserManager manager;
    private String defaultLink = ""; // Default link to be used if set

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
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @return the {@code WebDriver} instance associated with the new window.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewWindow(String driverName) throws WebDriverInitializationException {
        return open(driverName, getDefaultLink(), true);
    }

    /**
     * Opens a new browser tab or a new window based on the provided flag.
     *
     * @param driverName    the name of the {@code WebDriver} instance to be used.
     * @param openNewWindow if true, opens a new window; otherwise, opens a new tab.
     * @return the {@code WebDriver} instance associated with the operation.
     * @throws WebDriverInitializationException if the {@code WebDriver} instance could not be created or retrieved.
     */
    public WebDriver openNewTab(String driverName, boolean openNewWindow) throws WebDriverInitializationException {
        return open(driverName, getDefaultLink(), openNewWindow);
    }

    /**
     * Opens multiple links in new tabs within an existing browser window.
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @param links      URLs to be opened in new tabs.
     * @return a list of {@code WebDriver} instances used to open the tabs.
     */
    public List<WebDriver> openLinksInTabs(String driverName, String... links) {
        WebDriver driver = handleBrowserOperation(driverName, "tab");
        return Arrays.stream(links)
                .map(link -> open(driver, link, false))
                .collect(Collectors.toList());
    }

    /**
     * Opens multiple links, each in a new window.
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @param links      URLs to be opened in new windows.
     * @return a list of {@code WebDriver} instances used to open the windows.
     */
    public List<WebDriver> openLinksInNewWindows(String driverName, String... links) {
        return Arrays.stream(links)
                .map(link -> open(driverName, link, true))
                .collect(Collectors.toList());
    }

    /**
     * Opens the same link in multiple browsers.
     *
     * @param link        the URL to be opened in multiple browsers.
     * @param driverNames names of the {@code WebDriver} instances to be used.
     * @return a list of {@code WebDriver} instances used to open the link.
     */
    public List<WebDriver> openLinkInMultipleBrowsers(String link, String... driverNames) {
        return Arrays.stream(driverNames)
                .map(driverName -> open(driverName, link, false))
                .collect(Collectors.toList());
    }

    /**
     * Opens all provided links in a new window, with each link in a separate tab.
     *
     * @param driverName the name of the {@code WebDriver} instance to be used.
     * @param links      URLs to be opened in new tabs within a new window.
     * @return a list of {@code WebDriver} instances used to open the tabs.
     */
    public List<WebDriver> openLinksInNewWindowWithTabs(String driverName, String... links) {
        WebDriver initialDriver = handleBrowserOperation(driverName, "window");
        List<WebDriver> drivers = new ArrayList<>(Collections.singletonList(initialDriver));
        List<WebDriver> tabs = Arrays.stream(links)
                .map(link -> open(initialDriver, link, false))
                .toList();
        drivers.addAll(tabs);
        return drivers;
    }

    /**
     * Opens a link in either a new tab or a new window using the specified {@code WebDriver} instance.
     *
     * @param driver        the {@code WebDriver} instance used to open the link.
     * @param link          the URL to be opened in the new tab or window.
     * @param openNewWindow if true, opens the link in a new window; otherwise, opens it in a new tab.
     * @return the {@code WebDriver} instance used for the operation.
     */
    private synchronized WebDriver open(WebDriver driver, String link, boolean openNewWindow) {
        return open(driver.getClass().getSimpleName(), link, openNewWindow);
    }

    /**
     * Opens a link in either a new tab or a new window based on the specified driver name.
     *
     * @param driverName    the name of the {@code WebDriver} instance to be used.
     * @param link          the URL to be opened in the new tab or window.
     * @param inNewWindow   if true, opens the link in a new window; otherwise, opens it in a new tab.
     * @return the {@code WebDriver} instance used for the operation.
     */
    private synchronized WebDriver open(String driverName, String link, boolean inNewWindow) {
        final String type = inNewWindow ? WindowType.WINDOW.toString() : WindowType.TAB.toString();
        WebDriver driver = handleBrowserOperation(driverName, type);
        driver.switchTo().newWindow(WindowType.fromString(type));
        driver.get(link);
        return driver;
    }

    /**
     * Handles the common logic for browser operations, such as opening new windows or tabs.
     * Logs the operation being performed and retrieves the corresponding {@code WebDriver} instance
     * from the {@code BrowserManager}.
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