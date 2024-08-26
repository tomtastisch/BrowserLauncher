#!/bin/bash
set -e  # Script stops on first error

echo "Running tests..."

# Verwende Maven statt Gradle
mvn clean package # Führe die Tests mit Maven aus

# Falls du Headless-Browser-Tests hast:
export BROWSER_OPTS="--headless"
java -jar target/browser-detection-1.0-SNAPSHOT.jar https://example.com --browser chrome $BROWSER_OPTS

echo "Tests completed successfully."