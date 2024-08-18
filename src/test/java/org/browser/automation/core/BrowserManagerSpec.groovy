import com.fasterxml.jackson.databind.ObjectMapper
import org.browser.automation.core.Browser
import org.browser.automation.core.BrowserManager
import org.browser.automation.core.DefaultBrowserDetector
import org.browser.automation.core.access.cache.WebDriverCache
import org.openqa.selenium.WebDriver
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Field
import java.time.Duration

class BrowserManagerSpec extends Specification {

    @Shared
    Browser.BrowserInfo defaultBrowserInfo

    @Shared
    WebDriverCache realWebDriverCache

    @Shared
    WebDriver mockWebDriver = Mock(WebDriver)

    @Shared
    BrowserManager browserManager

    @Shared
    String cacheJson

    def setupSpec() {
        // Lade die JSON-Repräsentation des Caches
        cacheJson = loadCacheJson()

        // Setup des WebDriverCache mit Deserialisierung und Rekonstruktion der WebDriver-Instanzen
        setupWebDriverCache()

        // Setup des BrowserManagers und Injektion des konfigurierten WebDriverCache
        setupBrowserManager()

        // Setup des Default-Browsers
        setupDefaultBrowserInfo()
    }

    private void setupDefaultBrowserInfo() {
        Optional<Browser.BrowserInfo> detectedBrowserInfo = DefaultBrowserDetector.getInstance().getDefaultBrowserInfo()
        assert detectedBrowserInfo.isPresent(): "No default browser found"
        defaultBrowserInfo = detectedBrowserInfo.get()
    }

    private void setupWebDriverCache() {
        // Initialisiere den realen WebDriverCache
        realWebDriverCache = WebDriverCache.builder()
                .autoCleanupEnabled(true)
                .inactivityTimeout(Duration.ofMinutes(10))
                .build()

        // Verwende das geladene JSON, um die WebDriver-Instanzen zu rekonstruieren
        Map<String, String> driverMap = new ObjectMapper().readValue(cacheJson, Map.class)
        driverMap.each { key, className ->
            if (className == "MockWebDriver") {
                // Stellen Sie sicher, dass der Schlüssel als String übergeben wird
                realWebDriverCache.addDriver((String) key, mockWebDriver)
            }
            // Weitere Bedingungen für andere WebDriver-Typen, falls notwendig
        }
    }

    private void setupBrowserManager() {
        browserManager = BrowserManager.getInstance()

        // Injektion des realen WebDriverCache in das BrowserManager-Objekt
        Field cacheField = BrowserManager.class.getDeclaredField("webDriverCache")
        cacheField.setAccessible(true)
        cacheField.set(browserManager, realWebDriverCache)
    }

    @SuppressWarnings("StaticMethod")
    private String loadCacheJson() {
        // Simuliertes Laden des JSON-Strings (in der Realität könnte dies aus einer Datei oder einem anderen Speicherort kommen)
        return '''
        {
            "chrome": "MockWebDriver"
        }
        '''
    }

    def "Test openNewWindow with reflection-based field injection"() {
        given:
        String driverName = defaultBrowserInfo.name()

        when:
        WebDriver result = browserManager.openNewWindow(driverName)

        then:
        1 * realWebDriverCache.getDriver(driverName) >> mockWebDriver
        result == mockWebDriver
    }

    def "Test openNewTab without opening a new window"() {
        given:
        String driverName = defaultBrowserInfo.name()

        when:
        WebDriver result = browserManager.openNewTab(driverName, false)

        then:
        1 * realWebDriverCache.getDriver(driverName) >> mockWebDriver
        result == mockWebDriver
    }

    def "Test openNewTab with opening a new window"() {
        given:
        String driverName = defaultBrowserInfo.name()

        when:
        WebDriver result = browserManager.openNewTab(driverName, true)

        then:
        1 * realWebDriverCache.getDriver(driverName) >> mockWebDriver
        result == mockWebDriver
    }

    def "Test getWebDriver"() {
        given:
        String driverName = defaultBrowserInfo.name()

        when:
        WebDriver result = browserManager.getWebDriver(driverName)

        then:
        1 * realWebDriverCache.getDriver(driverName) >> mockWebDriver
        result == mockWebDriver
    }
}