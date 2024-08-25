package org.browser.automation.core.access.cache

import org.browser.automation.core.exception.WebDriverInitializationException
import org.openqa.selenium.WebDriver
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class AbstractWebDriverCacheManagerSpec extends Specification {

    WebDriverCache mockCache
    AbstractWebDriverCacheManager manager

    def setup() {
        mockCache = Mock(WebDriverCache)
        manager = new AbstractWebDriverCacheManager(mockCache) {}
    }

    def "should retrieve a cached WebDriver instance by session ID"() {
        given: "A mock WebDriver instance and session ID"
        WebDriver mockDriver = Mock(WebDriver)
        String sessionId = "test-session-id"
        mockCache.getDriverBySessionId(sessionId) >> mockDriver

        expect: "The manager should return the cached WebDriver instance"
        manager.getWebDriver(sessionId) == mockDriver
    }

    def "should throw an exception if WebDriver is not found in the cache"() {
        given: "A session ID that is not cached"
        String sessionId = "invalid-session-id"
        mockCache.getDriverBySessionId(sessionId) >> null

        when: "Trying to retrieve the WebDriver instance"
        manager.getWebDriver(sessionId)

        then: "A WebDriverInitializationException should be thrown"
        thrown(WebDriverInitializationException)
    }

    def "should verify if a WebDriver is cached by session ID"() {
        given: "A session ID and cached WebDriver instance"
        String sessionId = "test-session-id"
        mockCache.getDriverBySessionId(sessionId) >> Mock(WebDriver)

        expect: "The manager should return true if the WebDriver is cached"
        manager.isDriverCached(sessionId)
    }

    def "should return a list of all cached WebDriver instances"() {
        given: "Multiple WebDriver instances in the cache"
        WebDriver driver1 = Mock(WebDriver)
        WebDriver driver2 = Mock(WebDriver)
        List<WebDriver> cachedDrivers = [driver1, driver2]
        mockCache.getDriverCache() >> new ConcurrentHashMap<String, WebDriver>(
                [(UUID.randomUUID().toString()): driver2, (UUID.randomUUID().toString()): driver1]
        )

        expect: "The manager should return all cached WebDriver instances"
        manager.getAllCachedDrivers() == cachedDrivers
    }

    def "should remove each WebDriver instance and call quit()"() {
        given: "Multiple WebDriver instances in the cache"
        WebDriver driver1 = Mock(WebDriver)
        WebDriver driver2 = Mock(WebDriver)
        def driverMap = [(UUID.randomUUID().toString()): driver1, (UUID.randomUUID().toString()): driver2]
        mockCache.getDriverCache() >> new ConcurrentHashMap<>(driverMap)

        when: "The manager removes all drivers"
        manager.clearAllDrivers()

        then: "Each driver should be removed using removeDriver(), which internally calls quit()"
        1 * mockCache.removeDriver(driver1)
        1 * mockCache.removeDriver(driver2)
    }

    def "should return the number of cached WebDriver instances"() {
        given: "A mock ConcurrentMap with WebDriver instances"
        def driverCache = Mock(ConcurrentMap)
        driverCache.size() >> 3
        mockCache.getDriverCache() >> driverCache

        expect: "The manager should return the correct count of cached WebDriver instances"
        manager.getCachedDriverCount() == 3
    }
}