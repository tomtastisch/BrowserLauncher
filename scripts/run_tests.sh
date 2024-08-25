#!/bin/bash
set -e  # Script stops on first error

echo "Running tests..."

# Beispiel: Führe Tests mit Gradle oder Maven aus
./gradlew clean test  # oder: mvn clean test

# Falls du Headless-Browser-Tests hast:
export BROWSER_OPTS="--headless"
java -jar BrowserLauncher.jar https://example.com --browser chrome $BROWSER_OPTS

echo "Tests completed successfully."