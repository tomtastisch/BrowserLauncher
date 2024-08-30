package org.browser.automation.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code Essential} annotation is used to mark fields in a class that are considered
 * essential for the proper use and functioning of the class. This annotation is purely
 * for documentation purposes and does not enforce any validation or processing at runtime.
 *
 * <p>By marking a field with {@code Essential}, developers can signal to others that this
 * field is critical for the intended use of the class and should be carefully managed.
 *
 * <p>Example usage:
 * <pre>
 * public class MyClass {
 *
 *     {@literal @}Essential
 *     private String importantField;
 *
 *     private String optionalField;
 *
 *     // Constructor, getters, setters, etc.
 * }
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface Essential {
    // No methods or fields required; purely for marking purposes
}
