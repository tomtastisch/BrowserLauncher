package org.browser.automation.core

import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import org.browser.config.ConfigurationProvider
import org.openqa.selenium.WebDriver
import spock.lang.Specification
import spock.lang.Subject

/**
 * Unit tests for the {@link Browser} class.
 *
 * These tests validate that the browser configurations are correctly loaded and the appropriate
 * browser information is retrieved based on the system's operating system.
 */
class BrowserSpec extends Specification {

    // Dynamically create the browser instance using a real configuration provider
    def configProvider = new ConfigurationProvider()

    @Subject
    Browser browser = new Browser(configProvider)

    // Cache the retrieved browser info to avoid redundant retrieval
    Optional<Browser.BrowserInfo> cachedBrowserInfo = browser.getDefaultBrowserInfo()


    def "should load browser configuration"() {
        expect: "Configuration should not be empty"
        def config = configProvider.getConfig()
        config.hasPath("osBrowserPaths.mac") || config.hasPath("osBrowserPaths.win") || config.hasPath("osBrowserPaths.nix")
    }

    /**
     * Tests that the default browser information is correctly retrieved from the configuration.
     * This ensures that the correct browser path and WebDriver class are selected based on the OS.
     */
    def "should retrieve default browser information from configuration"() {
        expect: "Default browser information should be available and match expected properties"
        cachedBrowserInfo.isPresent()
        def browserInfo = cachedBrowserInfo.get()

        // Check for basic properties dynamically based on system configuration
        browserInfo.name() != null
        browserInfo.path() != null
        browserInfo.driverClass() != null
    }

    /**
     * Tests that the WebDriver class can be correctly instantiated from the class name.
     */
    def "should instantiate WebDriver from class name"() {
        given: "The dynamically retrieved browser information"
        def browserInfo = cachedBrowserInfo.get()

        and: "A mocked WebDriver instance"
        def mockWebDriver = Mock(WebDriver)

        and: "Mocking the instantiateDriver method to return the mocked WebDriver instance"
        browser.instantiateDriver(browserInfo.driverClass()) >> mockWebDriver

        when: "Instantiating the WebDriver class"
        WebDriver driver = browser.instantiateDriver(browserInfo.driverClass())

        then: "The WebDriver instance should be created successfully"
        driver != null
        driver == mockWebDriver
    }

    /**
     * Tests that the getDriverClass method correctly resolves the WebDriver class from its name.
     */
    def "should resolve WebDriver class from class name"() {
        given: "The dynamically retrieved browser information"
        def browserInfo = cachedBrowserInfo.get()

        when: "Retrieving the WebDriver class"
        Class<? extends WebDriver> driverClass = Browser.getDriverClass(browserInfo.driverClass().getName())

        then: "The correct WebDriver class should be resolved"
        driverClass != null
        driverClass.getName() == browserInfo.driverClass().getName()
    }

    /**
     * Tests the behavior when the browser configuration is missing or incorrect.
     */
    def "should handle missing or incorrect browser configuration gracefully"() {
        given: "A mock configuration provider with missing or incorrect configuration"
        def mockConfig = Mock(Config) {
            getConfigList(_ as String) >> { throw new ConfigException.Missing("osBrowserPaths") }
        }
        def mockConfigProvider = new ConfigurationProvider(mockConfig)
        def browser = new Browser(mockConfigProvider)

        when: "Retrieving the default browser information"
        Optional<Browser.BrowserInfo> browserInfo = browser.getDefaultBrowserInfo()

        then: "No browser information should be returned"
        !browserInfo.isPresent()
    }
}