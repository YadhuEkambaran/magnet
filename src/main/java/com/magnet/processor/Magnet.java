package com.magnet.processor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yadhukrishnan.e@oneteam.us
 */

public class Magnet {

    private final Map<Method, ServiceMethod> serviceMethodCache = new ConcurrentHashMap<>();

    final String baseUrl;

    Magnet(Builder builder) {
        baseUrl = builder.baseUrl;
    }

    public <T> T create(Class<T> ourInterface) {
        validateMethods(ourInterface);

        return (T) Proxy.newProxyInstance(ourInterface.getClassLoader(), new Class[]{ourInterface},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        ServiceMethod serviceMethod =
                                (ServiceMethod) loadServiceMethod(method);
                        HttpWorker call = new HttpWorker(serviceMethod, args);
                        return call;
                    }
                });
    }




    private <T> void validateMethods(Class<T> ourInterface) {
        for (Method method : ourInterface.getDeclaredMethods()) {
            loadServiceMethod(method);
        }
    }

    private ServiceMethod loadServiceMethod(Method method) {
        ServiceMethod result = serviceMethodCache.get(method);
        if (result != null) {
            return result;
        }

        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                result = new ServiceMethod.Builder(this, method).build();
                serviceMethodCache.put(method, result);
            }
        }

        return result;
    }

    public final static class Builder {
        private String baseUrl;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public  Magnet build() {
            return new Magnet(this);
        }
    }


}
