package org.browser.automation.core.annotation.handler;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.browser.automation.core.annotation.CacheLock;
import org.browser.automation.core.annotation.ResourceKey;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The `LockInvocationHandler` is an implementation of the {@link InvocationHandler} interface
 * that provides thread-safe access to methods annotated with {@link CacheLock}. It supports
 * different lock levels (GLOBAL, RESOURCE, NONE), configurable lock timeouts, and efficient
 * resource management using weak references.
 *
 * <p>This class is particularly useful in multi-threaded environments where methods must be
 * synchronized based on different scopes: globally across the entire application or based on
 * specific resources like session IDs. By dynamically determining the resource key using the
 * {@link ResourceKey} annotation, this handler offers flexibility and precise control over
 * locking behavior.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li><strong>Dynamic Locking Strategy:</strong> The handler applies locking based on the
 *     {@link CacheLock} annotation, supporting global locks, resource-specific locks, and methods
 *     with no locking requirements.</li>
 *     <li><strong>Configurable Timeout:</strong> The timeout for acquiring locks is loaded from
 *     `application.conf`, allowing easy configuration without code changes.</li>
 *     <li><strong>Automatic Resource Management:</strong> Resource-specific locks are managed using
 *     weak references, ensuring that unused locks are garbage-collected to avoid memory leaks.</li>
 *     <li><strong>Comprehensive Logging:</strong> The handler automatically logs the creation of new locks,
 *     including details like the lock level, the method involved, and the current count of active locks.
 *     Additionally, it logs timeout events when a lock cannot be acquired within the specified time.</li>
 * </ul>
 *
 * Example usage:
 * <pre>{@code
 * @CacheLock(level = CacheLock.LockLevel.RESOURCE)
 * public void someMethod(@ResourceKey String sessionId, String otherParam) {
 *     // sessionId will be used as the resource key for locking
 * }
 * }</pre>
 *
 * @see CacheLock
 * @see ResourceKey
 * @see InvocationHandler
 */
@Slf4j
public class LockInvocationHandler implements InvocationHandler {

    // The timeout for acquiring locks, loaded from the configuration file
    private static final long LOCK_TIMEOUT_MS;

    // A counter to track the number of active locks
    private static final AtomicInteger lockCounter = new AtomicInteger(0);

    // The target object whose methods are being proxied and synchronized
    private final Object target;

    // The global lock used for methods with GLOBAL lock level
    private final Lock globalLock = new ReentrantLock();

    // A map to store resource-specific locks, managed with weak references
    private final Map<String, WeakReference<Lock>> resourceLocks = new ConcurrentHashMap<>();

    // Static block to load the lock timeout from application.conf
    static {
        Config config = ConfigFactory.load();
        LOCK_TIMEOUT_MS = config.getLong("webdriver.cache.lock.timeout");
    }

    /**
     * Constructs a new {@code LockInvocationHandler} for the given target object.
     * The lock timeout is configured via `application.conf`.
     *
     * @param target the object whose methods will be synchronized based on the {@link CacheLock} annotation.
     */
    public LockInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     * Intercepts method invocations on the proxy instance and applies synchronization based on the
     * {@link CacheLock} annotation. Depending on the lock level (GLOBAL, RESOURCE, NONE), different
     * locking strategies are applied.
     *
     * @param proxy  the proxy instance that the method was invoked on.
     * @param method the {@link Method} instance corresponding to the interface method invoked on the proxy instance.
     * @param args   an array of objects containing the values of the arguments passed in the method invocation on the proxy instance.
     * @return the result of the method execution.
     * @throws Throwable if the method invocation throws an exception.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        CacheLock cacheLock = method.getAnnotation(CacheLock.class);

        if (Objects.nonNull(cacheLock)) {
            return switch (cacheLock.level()) {
                case GLOBAL -> handleWithGlobalLock(method, args);
                case RESOURCE -> handleWithResourceLock(method, args);
                default -> {
                    log.trace("No lock applied - Method: {}, Level: NONE", method.getName());
                    yield method.invoke(target, args);
                }
            };
        } else {
            log.trace("Method invoked without CacheLock annotation - Method: {}", method.getName());
        }
        return method.invoke(target, args);
    }

    /**
     * Handles method invocation with a global lock. This ensures that only one thread can execute
     * any method annotated with a GLOBAL lock at a time. The lock timeout is configurable.
     *
     * @param method the method to be invoked.
     * @param args   the method arguments.
     * @return the result of the method execution.
     * @throws Throwable if the method invocation throws an exception or if the lock cannot be acquired within the timeout.
     */
    private Object handleWithGlobalLock(Method method, Object[] args) throws Throwable {
        if (globalLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            log.info("Global lock acquired - Method: {}", method.getName());
            try {
                logNewLock("GLOBAL", method);
                return method.invoke(target, args);
            } finally {
                globalLock.unlock();
                log.info("Global lock released - Method: {}", method.getName());
            }
        } else {
            logTimeout(method);
            throw new IllegalStateException("Global lock timeout exceeded.");
        }
    }

    /**
     * Handles method invocation with a resource-specific lock. This lock is based on a resource key
     * (e.g., session ID), allowing parallel access to different resources. The lock is managed using
     * a weak reference, so it can be garbage-collected if it is no longer needed.
     *
     * <p>The resource key is determined either by the {@link ResourceKey} annotation on a method parameter,
     * or by defaulting to the first argument.</p>
     *
     * @param method the method to be invoked.
     * @param args   the method arguments.
     * @return the result of the method execution.
     * @throws Throwable if the method invocation throws an exception or if the lock cannot be acquired within the timeout.
     */
    private Object handleWithResourceLock(Method method, Object[] args) throws Throwable {
        String resourceKey = extractResourceKey(method, args);
        Lock resourceLock = resourceLocks.computeIfAbsent(resourceKey, key -> new WeakReference<>(new ReentrantLock())).get();

        if (Objects.nonNull(resourceLock) && resourceLock.tryLock(LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
            log.info("Resource lock acquired - Method: {}, Resource: {}", method.getName(), resourceKey);
            try {
                logNewLock("RESOURCE", method);
                return method.invoke(target, args);
            } finally {
                resourceLock.unlock();
                log.info("Resource lock released - Method: {}, Resource: {}", method.getName(), resourceKey);
                cleanUpUnusedLocks(resourceKey, resourceLock);
            }
        } else {
            logTimeout(method, resourceKey);
            throw new IllegalStateException("Resource lock timeout for key: " + resourceKey + " exceeded.");
        }
    }

    /**
     * Cleans up unused resource locks by removing them from the map if they are no longer referenced.
     * This method is called after releasing a lock to ensure that resources are efficiently managed.
     *
     * @param resourceKey  the key associated with the lock (e.g., session ID).
     * @param resourceLock the lock that was previously acquired.
     */
    private void cleanUpUnusedLocks(String resourceKey, Lock resourceLock) {
        if (resourceLock.tryLock()) {
            try {
                resourceLocks.computeIfPresent(resourceKey, (key, weakLock) -> Objects.isNull(weakLock.get()) ? null : weakLock);
            } finally {
                resourceLock.unlock();
            }
        }
    }

    /**
     * Extracts the resource key from the method arguments. The method first checks if any parameter
     * is annotated with {@link ResourceKey}. If found, that parameter's value is used as the resource key.
     * If no such annotation is found, it defaults to using the first argument.
     *
     * @param method the method being invoked.
     * @param args   the method arguments.
     * @return the extracted resource key.
     * @throws IllegalArgumentException if the resource key is not found or is of the wrong type.
     */
    private String extractResourceKey(Method method, Object[] args) {
        return java.util.stream.IntStream.range(0, method.getParameterCount())
                .filter(i -> java.util.Arrays.stream(method.getParameterAnnotations()[i])
                        .anyMatch(annotation -> annotation instanceof ResourceKey))
                .mapToObj(i -> args[i])
                .filter(arg -> arg instanceof String)
                .map(String.class::cast)
                .findFirst()
                .orElseGet(() -> Optional.ofNullable(args)
                        .filter(a -> a.length > 0 && a[0] instanceof String)
                        .map(a -> (String) a[0])
                        .orElseThrow(() -> new IllegalArgumentException("Expected a String resource key as the first argument or marked with @ResourceKey.")));
    }

    /**
     * Logs the creation of a new lock, including the lock level, method information,
     * and the current count of active locks.
     *
     * @param lockLevel the level of the lock being created (GLOBAL or RESOURCE).
     * @param method    the method that triggered the lock.
     */
    private void logNewLock(String lockLevel, Method method) {
        int currentLockCount = lockCounter.incrementAndGet();
        log.info("New Lock Created - Level: {}, Method: {}, Active Lock Count: {}", lockLevel, method.getName(), currentLockCount);
    }

    /**
     * Logs a timeout event when a lock cannot be acquired within the specified timeout period.
     *
     * @param method the method that attempted to acquire the lock.
     */
    private void logTimeout(Method method) {
        log.warn("Lock acquisition timed out - Level: {}, Method: {}, Timeout: {} ms", "GLOBAL", method.getName(), LOCK_TIMEOUT_MS);
    }

    /**
     * Logs a timeout event when a resource-specific lock cannot be acquired within the specified timeout period.
     *
     * @param method      the method that attempted to acquire the lock.
     * @param resourceKey the resource key associated with the lock.
     */
    private void logTimeout(Method method, String resourceKey) {
        log.warn("Lock acquisition timed out - Level: {}, Method: {}, Resource: {}, Timeout: {} ms", "RESOURCE", method.getName(), resourceKey, LOCK_TIMEOUT_MS);
    }
}