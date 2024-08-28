package org.browser.automation.core;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.BrowserDetector.BrowserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class BrowserDetectorTest {

    @Spy
    @InjectMocks
    private BrowserDetector browserDetector;

    @Mock
    private WebDriver mockWebDriver;

    @BeforeEach
    void setUp() {
        // Lese die tatsächliche Konfiguration ein und modifiziere sie für den Test
        Config realConfig = ConfigFactory.parseResources("application.conf");

        // Füge Mock-Pfade hinzu, um die realen Pfade zu überschreiben
        Config mockConfig = realConfig.withValue("osBrowserPaths.win", ConfigValueFactory.fromIterable(List.of(
                ConfigValueFactory.fromMap(Map.of(
                        "name", "Chrome",
                        "path", "C:/Mock/Path/To/Chrome",
                        "driverClass", "org.openqa.selenium.chrome.ChromeDriver"
                )),
                ConfigValueFactory.fromMap(Map.of(
                        "name", "Firefox",
                        "path", "C:/Mock/Path/To/Firefox",
                        "driverClass", "org.openqa.selenium.firefox.FirefoxDriver"
                ))
        )));

        browserDetector = spy(new BrowserDetector(mockConfig));
    }

    @Test
    void testGetDefaultBrowserInfo() {
        log.info("Starting test for default browser detection.");

        doReturn(Optional.of(
                new BrowserInfo("Chrome", "C:/Mock/Path/To/Chrome", mockWebDriver.getClass()))
        ).when(browserDetector).findDefaultBrowser(anyList());

        Optional<BrowserInfo> defaultBrowser = browserDetector.getDefaultBrowserInfo();

        assertTrue(defaultBrowser.isPresent(), "Default browser should be detected.");
        assertEquals("Chrome", defaultBrowser.get().name(), "Expected Chrome as the default browser.");
    }

    @Test
    void testGetDefaultBrowserInfoWithFallback() {
        log.info("Starting test for default browser detection with fallback.");

        // Simuliere, dass kein Standardbrowser gefunden wird
        doReturn(Optional.empty()).when(browserDetector).findDefaultBrowser(anyList());

        // Rufe die Methode mit useFallbackBrowser = true auf
        Optional<BrowserInfo> fallbackBrowser = browserDetector.getDefaultBrowserInfo(true);

        // Überprüfe, ob einer der beiden Browser zurückgegeben wird
        assertTrue(fallbackBrowser.isPresent(), "A fallback browser should be detected.");
        assertTrue(List.of("Chrome", "Firefox").contains(fallbackBrowser.get().name()),
                "Expected either Chrome or Firefox as the fallback browser.");
    }
}