package org.browser.automation.utils;

import lombok.experimental.UtilityClass;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatcher;

import java.lang.reflect.InvocationTargetException;

/**
 * The {@code ByteBuddyUtils} class provides utility methods for creating dynamic instances of classes
 * using ByteBuddy with method interception.
 */
@UtilityClass
public class ByteBuddyUtils {

    /**
     * Dynamically creates an instance of a class using ByteBuddy with method interception.
     * This method allows the creation of a subclass of the specified target class, where
     * specified methods are intercepted and handled by the provided handler instance.
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