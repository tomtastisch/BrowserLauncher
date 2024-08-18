package org.browser.automation.core;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class TestLocalization {

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH); // Stelle sicher, dass Englisch verwendet wird
        ResourceBundle messages = ResourceBundle.getBundle("context.messages", Locale.getDefault());

        // Testen der Fehlermeldungen
        String message = messages.getString("exception.webdriver.not.found");
        System.out.println("Unformatted message: " + message);

        String formattedMessage = MessageFormat.format(message, "Test");
        System.out.println("Formatted message: " + formattedMessage);
    }
}