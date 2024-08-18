package org.browser.automation.core.exception.base

import spock.lang.Specification
import spock.lang.Unroll

class LocalizedExceptionBaseSpec extends Specification {

    static class TestLocalizedException extends LocalizedExceptionBase {
        TestLocalizedException(String messageKey, Object... args) {
            super(messageKey, args)
        }

        TestLocalizedException(String customMessage, String messageKey, Object... args) {
            super(customMessage, messageKey, args)
        }
    }

    def setupSpec() {
        // Set default locale to test translations, in this case to German
        Locale.setDefault(Locale.GERMANY)
    }

    @Unroll
    def "should return the correct localized message for #locale"() {
        given: "The default locale is set to #locale"
        Locale.setDefault(locale)

        when: "The exception is thrown with a specific key"
        def exception = new TestLocalizedException("exception.webdriver.not.found", "Test")

        then: "The localized message should be correctly formatted"
        exception.message == expectedMessage

        where:
        locale           | expectedMessage
        Locale.GERMANY   | "Die Klasse Test entspricht keiner verfügbaren WebDriver-Klasse. Bitte überprüfen Sie den Klassenpfad und stellen Sie sicher, dass die Klasse existiert."
        Locale.FRANCE    | "La classe Test ne correspond à aucune classe WebDriver disponible. Veuillez vérifier le chemin de classe et vous assurer que la classe existe."
        Locale.of("es")  | "La clase Test no coincide con ninguna clase WebDriver disponible. Verifique la ruta de la clase y asegúrese de que la clase exista."
    }

    def "should return the correct custom message when provided"() {
        when: "The exception is thrown with a custom message"
        def exception = new TestLocalizedException("Custom message", "exception.webdriver.not.found.custom", "Test", "This class is missing.")

        then: "The custom message should be returned"
        exception.message == "Custom message"
    }
}