package org.browser.automation.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The `@CacheLock` annotation is used to indicate that a method should be synchronized using a lock.
 *
 * <p>This annotation provides a mechanism to control the granularity of synchronization by offering different
 * lock levels, which determine how the locking is applied. This is particularly useful in multi-threaded environments
 * where different levels of thread-safety are required based on the resource being accessed.</p>
 *
 * <p>The supported lock levels are:
 * <ul>
 *     <li>**GLOBAL**: A single lock is applied globally, meaning that all methods annotated with this level
 *     are synchronized using the same lock. This ensures that only one thread can access any of the annotated methods at a time.</li>
 *     <li>**RESOURCE**: A more granular lock that is based on a resource identifier, such as a session ID.
 *     This allows for thread-safe access to specific resources without blocking unrelated operations. For example,
 *     operations on different session IDs can run in parallel.</li>
 *     <li>**NONE**: No synchronization is applied. This level can be used for methods where thread-safety is either not required
 *     or handled differently.</li>
 * </ul>
 *
 * Example usage:
 * <pre>
 * {@code
 * @CacheLock(level = LockLevel.RESOURCE)
 * public void updateCache(String sessionId) {
 *     // Logic that needs to be thread-safe for a specific resource
 * }
 * }
 * </pre>
 *
 * <p>In a multi-threaded environment, this annotation ensures that only one thread can access the annotated method
 * at a time (based on the selected lock level), preventing concurrent modifications and ensuring data consistency.</p>
 *
 * @see org.browser.automation.core.annotation.handler.LockInvocationHandler
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CacheLock {

    /**
     * Defines the level of locking applied when this annotation is used.
     * The default is {@link LockLevel#GLOBAL}, which applies a single lock for all annotated methods.
     */
    LockLevel level() default LockLevel.GLOBAL;

    /**
     * Enum representing the different lock levels available.
     */
    enum LockLevel {
        /**
         * A global lock that synchronizes all annotated methods across the application.
         */
        GLOBAL,

        /**
         * A resource-specific lock that synchronizes based on a resource identifier (e.g., session ID).
         */
        RESOURCE,

        /**
         * No synchronization is applied.
         */
        NONE
    }
}