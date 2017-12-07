package com.magnet.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yadhukrishnan.e@oneteam.us
 */

public class RequestBuilder {

    String baseUrl;
    String relativeUrl;
    String httpMethod;
    String body;
    boolean hasBody;
    boolean hasPart;
    boolean hasHeader;

    Map<String, String> headers;
    Map<String, String> formData;
    Map<String, File> multiParts;


    private static final String ISO_8859_1 = "ISO-8859-1";
    private static final String UTF_8 = "UTF-8";

    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";

    RequestBuilder(String baseUrl, String relativeUrl, String httpMethod, boolean hasBody, boolean hasPart, boolean hasHeader) {
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.httpMethod = httpMethod;
        this.hasBody = hasBody;
        this.hasPart = hasPart;
        this.hasHeader = hasHeader;
        this.headers = new HashMap<>();
        this.formData = new HashMap<>();
        this.multiParts = new HashMap<>();
    }

    void addQueryParam(String name, String value) {
        StringBuilder builder = new StringBuilder();
        if (!relativeUrl.contains("?")) {
            relativeUrl += "?";
        } else {
            builder.append(PARAMETER_SEPARATOR);
        }

        final String encodedName = encode(name, UTF_8);
        final String encodedValue = value != null? encode(value, UTF_8) : "";
        builder.append(encodedName);
        builder.append(NAME_VALUE_SEPARATOR);
        builder.append(encodedValue);

        relativeUrl += builder.toString();
    }

    void addPathParam(String name, String value) {
        final String encodedValue = value != null? encode(value, UTF_8) : "";
        if (relativeUrl == null) {
            throw new AssertionError("Relative URL not found");
        }

        if (relativeUrl.replace(" ", "").contains("{" + name + "}")) {
            throw new IllegalArgumentException("No such placeholder in relative url");
        }
        this.relativeUrl = relativeUrl.replace(" ", "").replace("{" + name + "}", encodedValue);
    }

    void addHeader(String name, String value) {
        if (value == null || value.equals("")) {
            throw new IllegalArgumentException("Null value passed inside header param");
        }
        headers.put(name, value);
    }

    <T> void addBodyParam(T t) {
        Gson gson = new GsonBuilder().create();
        try {
            body = gson.toJson(t);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Class Casting exception");
        }

    }

    void addFormData(String name, String value) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Null key passed inside form data");
        }

        if (value == null || value.equals("")) {
            throw new IllegalArgumentException("Null value passed inside form data");
        }

        this.body += parseFormParam(name, value);
    }

    void addPartParams(Map<String, File> files) {
        if (files.size() < 1) {
            throw new IllegalArgumentException("Multipart does not contain any file");
        }

        this.multiParts = files;
    }

    private static String parseFormParam(String name, String value) {
        StringBuilder builder = new StringBuilder();
        if (builder.length() > 0) {
            builder.append(PARAMETER_SEPARATOR);
        }
        final String encodedName = encode(name, UTF_8);
        final String encodedValue = value != null? encode(value, UTF_8) : "";
        builder.append(encodedName);
        builder.append(NAME_VALUE_SEPARATOR);
        builder.append(encodedValue);
        return builder.toString();
    }

    private static String encode(final String content, final String encoding) {
        try {
            return URLEncoder.encode(content, encoding != null? encoding : ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
