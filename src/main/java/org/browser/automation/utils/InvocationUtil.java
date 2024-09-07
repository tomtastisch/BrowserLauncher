package org.browser.automation.utils;

import lombok.experimental.UtilityClass;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.InvocationTargetException;

/**
 * The {@code InvocationUtil} class provides utility methods for dynamically creating and configuring instances
 * of classes using ByteBuddy and reflection. It supports the creation of dynamic instances with method interception,
 * as well as the configuration of driver options with specific settings for browser automation.
 * <p>
 * This utility class includes methods for:
 * <ul>
 *     <li>Creating dynamic subclasses of classes using ByteBuddy, allowing for method interception and delegation.</li>
 *     <li>Applying configurations to instances of driver options, such as setting capabilities and preferences.</li>
 * </ul>
 * The methods in this class facilitate dynamic class creation and configuration management, enabling flexible
 * and powerful automation scenarios.
 * </p>
 */
@UtilityClass
public class InvocationUtil {

    /**
     * Enumeration representing the different types of options that can be applied to a driver options instance.
     * This enum categorizes the configuration options into distinct types, allowing for more precise handling
     * of configuration settings.
     */
    public enum OptionsType {
        /**
         * Represents command-line arguments that can be passed to the browser or driver.
         * These arguments are typically used to customize the behavior of the browser at startup.
         */
        ARGUMENTS,

        /**
         * Represents user preferences or settings for the browser.
         * These preferences may include custom configurations for the browser's behavior or appearance.
         */
        PREFERENCES,

        /**
         * Represents capabilities of the browser or driver.
         * These capabilities are often used to define specific features or behaviors required for the browser
         * or driver instance.
         */
        CAPABILITIES
    }

    /**
     * Dynamically creates an instance of a class using ByteBuddy with method interception.
     * This method allows for the creation of a subclass of the specified target class, where
     * methods matching the given matcher are intercepted and handled by the provided handler instance.
     *
     * @param <T>             the type of the class to be created.
     * @param targetClass     the class to be subclassed and dynamically instantiated.
     * @param handlerInstance the instance of the handler that will process the intercepted methods.
     * @param matcher         an {@link ElementMatcher} that defines which methods to intercept in the target class.
     * @return the dynamically created instance of the specified class.
     * @throws NoSuchMethodException     if the specified constructor cannot be found.
     * @throws InvocationTargetException if the underlying constructor throws an exception.
     * @throws InstantiationException    if the class that declares the underlying constructor is abstract.
     * @throws IllegalAccessException    if the underlying constructor is inaccessible.
     */
    public <T> T createInstance(Class<T> targetClass, Object handlerInstance, ElementMatcher<? super MethodDescription> matcher)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        // Create a dynamic subclass and intercept methods as specified by the matcher
        return new ByteBuddy()
                .subclass(targetClass)
                .method(matcher)
                .intercept(MethodDelegation.to(handlerInstance))
                .make()
                .load(targetClass.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }
}