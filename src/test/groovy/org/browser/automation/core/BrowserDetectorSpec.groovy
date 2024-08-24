package org.browser.automation.core

import com.typesafe.config.Config
import org.browser.automation.core.BrowserDetector.BrowserInfo
import org.browser.config.ConfigurationProvider
import spock.lang.Specification

class BrowserDetectorSpec extends Specification {

    def "should return default browser information"() {
        given: "A BrowserDetector instance with a mocked configuration provider"
        def configProvider = Mock(ConfigurationProvider) {
            getConfig() >> Mock(Config) {
                getConfigList(_ as String) >> []
            }
        }
        def detector = new BrowserDetector(configProvider)

        when: "getDefaultBrowserInfo is called"
        def result = detector.getDefaultBrowserInfo()

        then: "The result should contain a valid BrowserInfo if the browser is installed"
        result.isPresent()
        result.get() instanceof BrowserInfo
    }

    def "should return empty Optional if no browsers are installed"() {
        given: "A BrowserDetector instance with an empty configuration"
        def configProvider = Mock(ConfigurationProvider) {
            getConfig() >> Mock(Config) {
                getConfigList(_ as String) >> []
            }
        }
        def detector = new BrowserDetector(configProvider)

        when: "getInstalledBrowsers is called"
        def installedBrowsers = detector.getInstalledBrowsers()

        then: "No browsers should be detected"
        installedBrowsers.isEmpty()
    }
}