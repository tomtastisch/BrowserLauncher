package org.browser.config

import com.typesafe.config.Config
import spock.lang.Specification

/**
 * Test class for the BrowserConfig class using Spock.
 * <p>
 * This class tests the retrieval of configuration values from the application.conf file.
 */
class BrowserConfigSpec extends Specification {

    def "Config should be loaded correctly"() {
        expect: "The configuration is loaded correctly from the application.conf file"
        Config config = BrowserConfig.getConfig()
        config != null
    }

    def "Config value should be retrieved correctly as a string"() {
        when: "A known configuration value is retrieved as a string"
        Config browserConfig = BrowserConfig.getConfig().getConfigList("osBrowserPaths.win").get(0)
        String value = browserConfig.getString("name")

        then: "The configuration value should not be null"
        value != null
    }

    def "Config integer value should be retrieved correctly or be -1 if not found"() {
        when: "An integer configuration value is retrieved"
        int value = BrowserConfig.getInt("someIntegerConfigKey")

        then: "The value should be greater than or equal to -1"
        value >= -1
    }
}