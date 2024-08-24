package org.browser.automation.core.annotation.handler;

import org.browser.automation.core.annotation.CacheLock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockInvocationHandler implements InvocationHandler {

    private final Object target;
    private final Lock lock = new ReentrantLock();

    public LockInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isAnnotationPresent(CacheLock.class)) {
            lock.lock();
            try {
                return method.invoke(target, args);
            } finally {
                lock.unlock();
            }
        }
        return method.invoke(target, args);
    }
}
