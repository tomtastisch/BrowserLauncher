package org.browser.automation.core.access.cache

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.openqa.selenium.WebDriver
import spock.lang.Specification
import spock.lang.Subject

import java.time.Duration

class WebDriverCacheSpec extends Specification {

    @Subject
    WebDriverCache webDriverCache

    def setup() {
        webDriverCache = WebDriverCache.builder()
                .autoCleanupEnabled(false) // Disable auto-cleanup for testing purposes
                .inactivityTimeout(Duration.ofMinutes(1))
                .build()
    }

    def "should add and retrieve a WebDriver instance"() {
        given: "A mock WebDriver instance"
        WebDriver mockDriver = Mock(WebDriver)

        when: "Adding the WebDriver instance to the cache"
        webDriverCache.addDriver("chrome", mockDriver)

        then: "The WebDriver instance should be retrievable from the cache"
        webDriverCache.getDriver("chrome") == mockDriver
    }

    def "should replace an existing WebDriver instance"() {
        given: "Two different mock WebDriver instances"
        WebDriver firstDriver = Mock(WebDriver)
        WebDriver secondDriver = Mock(WebDriver)

        when: "Adding the first WebDriver instance and then replacing it with a second one"
        webDriverCache.addDriver("chrome", firstDriver)
        webDriverCache.addDriver("chrome", secondDriver)

        then: "The cache should contain the second WebDriver instance"
        webDriverCache.getDriver("chrome") == secondDriver
    }

    def "should remove a WebDriver instance from the cache"() {
        given: "A mock WebDriver instance"
        WebDriver mockDriver = Mock(WebDriver)

        and: "The WebDriver instance is added to the cache"
        webDriverCache.addDriver("chrome", mockDriver)

        when: "Removing the WebDriver instance from the cache"
        webDriverCache.removeDriver("chrome")

        then: "The WebDriver instance should no longer be in the cache"
        webDriverCache.getDriver("chrome") == null

        and: "The quit method on the WebDriver instance should be called"
        1 * mockDriver.quit()
    }

    def "should convert the driver cache to JSON"() {
        given: "Two mock WebDriver instances"
        WebDriver chromeDriver = Mock(WebDriver)
        WebDriver firefoxDriver = Mock(WebDriver)

        and: "The WebDriver instances are added to the cache"
        webDriverCache.addDriver("chrome", chromeDriver)
        webDriverCache.addDriver("firefox", firefoxDriver)

        when: "Converting the driver cache to JSON"
        String jsonOutput = webDriverCache.toJson()

        then: "The JSON output should contain the simplified class names"
        jsonOutput.contains('"chrome":"MockFor(WebDriver)"')
        jsonOutput.contains('"firefox":"MockFor(WebDriver)"')
    }

    def "should handle JSON conversion failure gracefully"() {
        given: "A broken ObjectMapper that throws an exception during serialization"
        ObjectMapper brokenMapper = Mock(ObjectMapper)
        brokenMapper.writeValueAsString(_) >> { throw new JsonProcessingException("Serialization error") {} }

        and: "The ObjectMapper is injected into the WebDriverCache instance using reflection"
        injectMockObjectMapper(webDriverCache, brokenMapper)

        when: "Attempting to convert the cache to JSON"
        String jsonOutput = webDriverCache.toJson()

        then: "The output should be an empty JSON object"
        jsonOutput == "{}"
    }

    private static void injectMockObjectMapper(WebDriverCache cache, ObjectMapper mockMapper) {
        def field = WebDriverCache.getDeclaredField("objectMapper")
        field.accessible = true
        field.set(cache, mockMapper)
    }

    def cleanup() {
        webDriverCache.shutdownScheduler()
    }
}