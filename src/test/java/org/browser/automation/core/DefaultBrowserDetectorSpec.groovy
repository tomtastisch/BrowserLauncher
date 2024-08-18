package org.browser.automation.core

import spock.lang.Specification

/**
 * Unit tests for the {@link DefaultBrowserDetector} class using Spock framework.
 *
 * <p>These tests validate the detection of the default browser based on the system configuration
 * and ensure that the initialization is correctly handled.
 */
class DefaultBrowserDetectorSpec extends Specification {

    DefaultBrowserDetector detector = DefaultBrowserDetector.getInstance()

    def "Detect default browser should not throw exceptions"() {
        when:
        def browserInfo = detector.getDefaultBrowserInfo()

        then:
        noExceptionThrown()
    }

    def "Browser information should be present if a matching configuration is found"() {
        when:
        def browserInfo = detector.getDefaultBrowserInfo()

        then:
        browserInfo.isPresent()
    }
}