package org.browser.automation.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The `@ResourceKey` annotation is used to specify which method parameter should be
 * treated as the resource key when determining the lock scope for a RESOURCE-level lock.
 *
 * <p>This annotation is particularly useful when the resource key is not the first argument
 * or when multiple potential keys exist.</p>
 *
 * Example usage:
 * <pre>{@code
 * @CacheLock(level = LockLevel.RESOURCE)
 * public void someMethod(@ResourceKey String sessionId, String otherParam) {
 *     // sessionId will be used as the resource key for locking
 * }
 * }</pre>
 *
 * <p>The annotation should be placed on the method parameter that represents the resource key.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ResourceKey {
}