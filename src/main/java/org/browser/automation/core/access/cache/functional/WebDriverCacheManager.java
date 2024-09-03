package org.browser.automation.core.access.cache.functional;

import org.browser.automation.exception.browser.driver.WebDriverInitializationException;
import org.openqa.selenium.WebDriver;

import java.util.List;

public interface WebDriverCacheManager {

    WebDriver getWebDriver(String sessionId) throws WebDriverInitializationException;

    boolean isDriverCachedBySessionId(WebDriver driver);

    boolean isDriverCachedBySessionId(String sessionId);

    boolean isDriverCachedByName(String driverName);

    List<WebDriver> getAllCachedDrivers();

    void clearAllDrivers();

    int getCachedDriverCount();
}
