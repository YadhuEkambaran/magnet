package com.magnet.processor;

import com.magnet.annotations.Body;
import com.magnet.annotations.DELETE;
import com.magnet.annotations.FormMap;
import com.magnet.annotations.GET;
import com.magnet.annotations.HeaderMap;
import com.magnet.annotations.PUT;
import com.magnet.annotations.Part;
import com.magnet.annotations.POST;
import com.magnet.annotations.Path;
import com.magnet.annotations.Query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by Yadhukrishnan.e@oneteam.us
 */

class ServiceMethod {

    private static final String PARAM = "[a-zA-Z][a-zA-Z0-9_-]*";
    private static final Pattern PARAM_URL_REGEX = Pattern.compile("\\{(" + PARAM + ")\\}");

    private final String baseUrl;
    private final String relativeUrl;
    private final String httpMethod;
    private final boolean hasBody;
    private final boolean hasPart;
    private final boolean hasHeader;

    private ParameterHandler<?>[] parameterHandlers;

    ServiceMethod(Builder builder) {
        baseUrl = builder.magnet.baseUrl;
        relativeUrl = builder.relativeUrl;
        httpMethod = builder.httpMethod;
        hasBody = builder.hasBody;
        hasPart = builder.hasPart;
        hasHeader = builder.hasHeader;
        parameterHandlers = builder.mParameterHandlers;
    }

    RequestBuilder toRequestBuilder(Object[] objs) {
        RequestBuilder builder = new RequestBuilder(baseUrl, relativeUrl, httpMethod, hasBody, hasPart, hasHeader);

        int argsCount = objs != null ? objs.length : 0;
        if (argsCount != parameterHandlers.length) {
            throw new IllegalArgumentException("Argument count does not match");
        }

        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

        for (int p = 0; p < parameterHandlers.length; p++) {
            handlers[p].handle(builder, objs[p]);
        }

        return builder;
    }

    final static class Builder {
        final Magnet magnet;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        ParameterHandler<?>[] mParameterHandlers;

        boolean hasBody;
        boolean hasPart;
        boolean hasHeader;
        String httpMethod;
        String relativeUrl;
        Set<String> relativeUrlParamNames;


        Builder(Magnet magnet, Method method) {
            this.magnet = magnet;
            this.method = method;
            this.methodAnnotations = method.getDeclaredAnnotations();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
        }

        ServiceMethod build() {
            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }

            int parameterCount = parameterAnnotationsArray.length;
            mParameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0; p < parameterCount; p++) {
                Type parameterType = parameterTypes[p];
                Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
                mParameterHandlers[p] = parseParameter(parameterType, parameterAnnotations);
            }

            return new ServiceMethod(this);
        }

        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof GET) {
                httpMethod = "GET";
                relativeUrl = ((GET) annotation).value();
                relativeUrlParamNames = parsePathParameters(relativeUrl);
            } else if (annotation instanceof POST) {
                httpMethod = "POST";
                relativeUrl = ((POST) annotation).value();
                relativeUrlParamNames = parsePathParameters(relativeUrl);
            } else if (annotation instanceof PUT) {
                httpMethod = "PUT";
                relativeUrl = ((PUT) annotation).value();
                relativeUrlParamNames = parsePathParameters(relativeUrl);
            } else if (annotation instanceof DELETE) {
                httpMethod = "DELETE";
                relativeUrl = ((DELETE) annotation).value();
                relativeUrlParamNames = parsePathParameters(relativeUrl);
            }
        }

        private ParameterHandler<?> parseParameter(Type parameterType, Annotation[] annotations) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(parameterType, annotation);
                if (annotationAction == null) {
                    continue;
                }

                result = annotationAction;
            }

            if (result == null) {
                throw new IllegalArgumentException("Empty parameter");
            }

            return result;
        }

        private ParameterHandler<?> parseParameterAnnotation(Type parameterType, Annotation annotation) {
            if (annotation instanceof Query) {
                Query query = (Query) annotation;
                String value = query.value();
                return new ParameterHandler.Query<>(parameterType, value) ;
            } else if (annotation instanceof Body) {
                hasBody = true;
                return new ParameterHandler.Body<>(parameterType);
            } else if (annotation instanceof Path) {
                Path path = (Path) annotation;
                String value = path.value();
                return new ParameterHandler.Path<>(value, parameterType);
            } else if (annotation instanceof HeaderMap){
                hasHeader = true;
                return new ParameterHandler.HeaderMap<>();
            } else if (annotation instanceof FormMap) {
                hasBody = true;
                return new ParameterHandler.FormMap<>();
            } else if (annotation instanceof Part) {
                hasBody = true;
                hasPart = true;
                return new ParameterHandler.Part();
            } else {
                throw new IllegalArgumentException("You have used unknown parameter annotation");
            }
        }

        static Set<String> parsePathParameters(String path) {
            Matcher m = PARAM_URL_REGEX.matcher(path);
            Set<String> patterns = new LinkedHashSet<>();
            while (m.find()) {
                patterns.add(m.group(1));
            }
            return patterns;
        }

    }
}
