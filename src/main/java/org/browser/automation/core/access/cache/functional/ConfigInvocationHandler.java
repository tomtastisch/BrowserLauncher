package org.browser.automation.core.access.cache.functional;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import com.typesafe.config.Config;
import org.openqa.selenium.MutableCapabilities;

import java.lang.reflect.Method;

/**
 * The {@code ConfigInvocationHandler} class is a dynamic method interceptor that handles
 * method calls on a ByteBuddy-generated proxy. It is designed to dynamically configure
 * {@link MutableCapabilities} for WebDriver instances based on a provided configuration.
 *
 * <p>This handler intercepts method calls such as {@code addArguments}, {@code addPreference},
 * and {@code setCapability} and maps them to corresponding configuration values from a {@link Config}
 * object. The class is designed to eliminate redundancy and handle method calls efficiently.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Dynamically intercepts and processes method calls for configuring WebDriver capabilities.</li>
 *   <li>Maps method calls to configuration entries, enabling flexible and centralized management of browser options.</li>
 *   <li>Supports a wide range of method signatures without the need for hardcoding specific logic.</li>
 * </ul>
 *
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * ConfigInvocationHandler handler = new ConfigInvocationHandler(config);
 * MutableCapabilities capabilities = new ByteBuddy()
 *      .subclass(MutableCapabilities.class)
 *      .method(ElementMatchers.named("addArguments")
 *              .or(ElementMatchers.named("addPreference"))
 *              .or(ElementMatchers.named("setCapability")))
 *      .intercept(MethodDelegation.to(handler))
 *      .make()
 *      .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
 *      .getLoaded()
 *      .getDeclaredConstructor()
 *      .newInstance();
 * }</pre>
 */
public class ConfigInvocationHandler {

    private final Config config;

    /**
     * Constructs a new {@code ConfigInvocationHandler} with the given configuration.
     *
     * @param config the {@link Config} object containing the browser options and preferences.
     */
    public ConfigInvocationHandler(Config config) {
        this.config = config;
    }

    /**
     * Intercepts method calls on the proxy and applies the corresponding configuration values.
     *
     * <p>This method dynamically intercepts method calls and maps them to the appropriate
     * configuration entries. It supports methods like {@code addArguments}, {@code addPreference},
     * and {@code setCapability}, but is flexible enough to handle other method names that match
     * the configuration keys.</p>
     *
     * @param method the original method being called.
     * @param args   the arguments passed to the method.
     * @return the result of the method invocation, or {@code null} if no matching configuration is found.
     * @throws Exception if an error occurs during method invocation.
     */
    @RuntimeType
    public Object intercept(@Origin Method method, @AllArguments Object[] args) throws Exception {
        String methodName = method.getName();

        // Map method names to configuration paths
        String configPath = getConfigPathForMethod(methodName);
        if (configPath == null) {
            return null;
        }

        // Fetch the corresponding configuration value based on method name
        Object configValue = getConfigValue(configPath, args);

        if (configValue != null) {
            return method.invoke(args[0], configValue);
        }

        return null;
    }

    /**
     * Maps the method name to the corresponding configuration path.
     *
     * @param methodName the name of the method being called.
     * @return the configuration path corresponding to the method, or {@code null} if no match is found.
     */
    private String getConfigPathForMethod(String methodName) {
        return switch (methodName) {
            case "addArguments" -> "arguments";
            case "addPreference" -> "preferences";
            case "setCapability" -> "capabilities";
            default -> null;
        };
    }

    /**
     * Retrieves the configuration value based on the method name and arguments.
     *
     * @param configPath the configuration path to look up.
     * @param args       the arguments passed to the intercepted method.
     * @return the configuration value, or {@code null} if not found.
     */
    private Object getConfigValue(String configPath, Object[] args) {
        return switch (configPath) {
            case "arguments" -> config.getStringList(configPath).toArray(new String[0]);
            case "preferences", "capabilities" -> {
                String key = (String) args[0];
                yield config.getConfig(configPath).hasPath(key) ? config.getConfig(configPath).getAnyRef(key) : null;
            }
            default -> null;
        };
    }
}