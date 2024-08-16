package org.browser.automation.core


import org.browser.automation.core.access.cache.WebDriverCache
import org.openqa.selenium.WebDriver
import spock.lang.Specification

import java.lang.reflect.Field

class BrowserManagerSpec extends Specification {

    BrowserManager browserManager
    WebDriverCache mockWebDriverCache = Mock(WebDriverCache)
    WebDriver mockWebDriver = Mock(WebDriver)

    def setup() {
        // Get the singleton instance of BrowserManager
        browserManager = BrowserManager.getInstance()

        // Use reflection to inject the mock WebDriverCache into the BrowserManager instance
        Field cacheField = BrowserManager.class.getDeclaredField("webDriverCache")
        cacheField.setAccessible(true)
        cacheField.set(browserManager, mockWebDriverCache)

        // Set the mock behavior for the WebDriverCache
        mockWebDriverCache.getDriver(_ as String) >> mockWebDriver
    }

    def "Test openNewWindow with reflection-based field injection"() {
        given:
        String driverName = "testDriver"

        when:
        WebDriver result = browserManager.openNewWindow(driverName)

        then:
        1 * mockWebDriverCache.getDriver(driverName)
        result == mockWebDriver
    }

    def "Test openNewTab without opening a new window"() {
        given:
        String driverName = "testDriver"

        when:
        WebDriver result = browserManager.openNewTab(driverName, false)

        then:
        1 * mockWebDriverCache.getDriver(driverName)
        result == mockWebDriver
    }

    def "Test openNewTab with opening a new window"() {
        given:
        String driverName = "testDriver"

        when:
        WebDriver result = browserManager.openNewTab(driverName, true)

        then:
        1 * mockWebDriverCache.getDriver(driverName)
        result == mockWebDriver
    }

    def "Test getWebDriver"() {
        given:
        String driverName = "testDriver"

        when:
        WebDriver result = browserManager.getWebDriver(driverName)

        then:
        1 * mockWebDriverCache.getDriver(driverName)
        result == mockWebDriver
    }
}