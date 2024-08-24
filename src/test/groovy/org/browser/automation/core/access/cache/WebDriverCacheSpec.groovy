package org.browser.automation.core.access.cache

import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.SessionId
import spock.lang.Specification

import java.time.Duration

class WebDriverCacheSpec extends Specification {

    WebDriverCache cache

    def setup() {
        cache = WebDriverCache.builder()
                .autoCleanupEnabled(false)
                .inactivityTimeout(Duration.ofMinutes(1))
                .build()
    }

    def "should add and retrieve WebDriver instance by session ID"() {
        given: "A mock WebDriver with a session ID"
        WebDriver driver = createMockDriver("test-session-id")

        when: "The WebDriver is added to the cache"
        cache.addDriver(driver)

        then: "It can be retrieved by its session ID"
        cache.getDriverBySessionId("test-session-id") == driver
    }

    def "should remove WebDriver instance by session ID and call quit"() {
        given: "A cached WebDriver"
        WebDriver driver = createMockDriver("test-session-id")
        cache.addDriver(driver)

        when: "The WebDriver is removed by its session ID"
        cache.removeDriver("test-session-id")

        then: "The driver is removed and quit is called"
        1 * driver.quit()
        !cache.getDriverBySessionId("test-session-id")
    }

    def "should use UUID as session ID for non-RemoteWebDriver instances"() {
        given: "A non-RemoteWebDriver"
        WebDriver driver = Mock(WebDriver)

        when: "The WebDriver is added to the cache"
        cache.addDriver(driver)

        then: "It is stored with a generated UUID as session ID"
        cache.getDriverCache().size() == 1
    }

    def "should handle auto-cleanup when enabled"() {
        given: "A WebDriverCache instance with auto-cleanup enabled"
        cache = WebDriverCache.builder()
                .autoCleanupEnabled(true)
                .inactivityTimeout(Duration.ofMinutes(1))
                .build()

        expect: "The scheduler is running"
        !cache.getScheduler().isShutdown()
    }

    def "should shutdown the scheduler gracefully"() {
        given: "A WebDriverCache instance with auto-cleanup enabled"
        cache = WebDriverCache.builder()
                .autoCleanupEnabled(true)
                .inactivityTimeout(Duration.ofMinutes(1))
                .build()

        when: "The scheduler is shut down"
        cache.shutdownScheduler()

        then: "The scheduler is no longer running"
        cache.getScheduler().isShutdown()
    }

    def "should return the singleton instance"() {
        expect: "The singleton instance is the same on multiple calls"
        WebDriverCache.getInstance() == WebDriverCache.getInstance()
    }

    private WebDriver createMockDriver(String sessionId) {
        WebDriver driver = Mock(RemoteWebDriver)

        SessionId mockSessionId = new SessionId(sessionId)
        driver.getSessionId() >> mockSessionId

        return driver
    }
}
