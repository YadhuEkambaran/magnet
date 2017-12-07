package com.magnet.processor;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by yadhukrishnan.e@oneteam.us
 */

abstract class ParameterHandler<T> {
    abstract void handle(RequestBuilder builder, T value);

    static class Query<T> extends ParameterHandler<T> {
        String name;
        Type type;

        Query(Type type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        void handle(RequestBuilder builder, T value) {
            builder.addQueryParam(name, Util.covertToString(value));
        }
    }


    static class Path<T> extends ParameterHandler<T> {
        String name;
        Type type;

        Path(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        @Override
        void handle(RequestBuilder builder, T value) {
            builder.addPathParam(name, Util.covertToString(value));
        }
    }

    static class HeaderMap<T> extends ParameterHandler<Map<String, T>> {

        @Override
        void handle(RequestBuilder builder, Map<String, T> value) {
            if (value == null) {
                throw new IllegalArgumentException("null passed as header");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.equals("")) {
                    throw new IllegalArgumentException("Header key is null");
                }

                T t = entry.getValue();
                builder.addHeader(key, Util.covertToString(t));
            }
        }
    }

    static class Body<T> extends ParameterHandler<T> {
        Type type;

        Body(Type type) {
            this.type = type;
        }

        @Override
        void handle(RequestBuilder builder, T value) {
            builder.addBodyParam(value);
        }
    }

    static class FormMap<T> extends ParameterHandler<Map<String, T>> {

        @Override
        void handle(RequestBuilder builder, Map<String, T> value) {
            if (value == null) {
                throw new IllegalArgumentException("null passed as form parameter");
            }

            for (Map.Entry<String, T> entry : value.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.equals("")) {
                    throw new IllegalArgumentException("Form parameter key is null");
                }

                T t = entry.getValue();
                builder.addFormData(key, Util.covertToString(t));
            }
        }
    }

    static class Part extends ParameterHandler<Map<String, File>> {

        @Override
        void handle(RequestBuilder builder, Map<String, File> value) {
            builder.addPartParams(value);
        }
    }
}
