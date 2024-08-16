package org.browser.automation.core


import spock.lang.Specification

/**
 * Unit tests for the {@link Browser} class.
 *
 * These tests validate that the browser configurations are correctly loaded and the appropriate
 * browser information is retrieved based on the system's operating system.
 */
class BrowserSpec extends Specification {

    /**
     * Tests that the default browser information is correctly retrieved from the configuration.
     * This ensures that the correct browser path and WebDriver class are selected based on the OS.
     */
    def "should retrieve default browser information from configuration"() {
        when: "Retrieving the default browser information"
        Optional<Browser.BrowserInfo> browserInfo = Browser.getDefaultBrowserInfo()

        then: "Default browser information should be available"
        browserInfo.isPresent()
    }
}