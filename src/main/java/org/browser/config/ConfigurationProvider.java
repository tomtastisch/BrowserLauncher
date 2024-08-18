package org.browser.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.Getter;

@Getter
public class ConfigurationProvider {

    private final Config config;

    public ConfigurationProvider() {
        this.config = ConfigFactory.load();
    }

    // For testing, you can inject a mock configuration
    public ConfigurationProvider(Config config) {
        this.config = config;
    }
}