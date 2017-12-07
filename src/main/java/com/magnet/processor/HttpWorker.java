package com.magnet.processor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Map;


/**
 * Created by yadhukrishnan.e@oneteam.us
 */

public class HttpWorker<T> extends Thread {

    private static final int CONNECTION_TIMEOUT = 5000;
    private static final int READ_TIMEOUT = 5000;

    private ServiceMethod serviceMethod;
    private Object[] args;

    private RequestBuilder builder;
    private CallBack<T> callback;
    private Class<T> responseType;

    private Map<String, String> headers;
    private Map<String, File> parts;

    HttpWorker(ServiceMethod serviceMethod, Object[] args) {
        this.serviceMethod = serviceMethod;
        this.args = args;
    }

    public void execute(Class<T> responseType, CallBack<T> callBack) {
        this.callback = callBack;
        this.builder = serviceMethod.toRequestBuilder(args);
        this.headers = builder.headers;
        this.responseType = responseType;
        this.parts = builder.multiParts;
        parts = builder.multiParts;
        start();
    }


    @Override
    public void run() {
        try {
            URL url = new URL(url());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if (builder.hasHeader) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            connection.setDoOutput(true);
            connection.setRequestMethod(builder.httpMethod);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            if (builder.hasPart) {
                DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
                for (Map.Entry<String, File> part : parts.entrySet()) {
                    parseFiles(writer, part.getKey(), part.getValue());
                }
                writer.flush();
                writer.close();
            }

            connection.connect();

            if (builder.hasBody) {
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.writeBytes(builder.body);
                wr.flush();
                wr.close();
            }

            int responseCode = connection.getResponseCode();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            connection.disconnect();
            reader.close();

            if (builder != null) {
                Gson gson = new GsonBuilder().create();
                if (responseType == String.class) {
                    T t = (T) responseBuilder.toString();
                    HttpWorker.this.
                            callback.successResponse(responseCode, t);
                } else {
                    T t = gson.fromJson(responseBuilder.toString(), responseType);
                    callback.successResponse(responseCode, t);
                }

            }
        } catch (SocketTimeoutException | UnknownHostException ex) {
            ex.printStackTrace();
            callback.offline();
        } catch (Exception ex) {
            ex.printStackTrace();
            callback.failureResponse(ex);
        }
    }

    private String url() {
        if (this.builder.relativeUrl.startsWith("http")) {
            return builder.relativeUrl;
        } else {
            return builder.baseUrl + builder.relativeUrl;
        }
    }

    private void parseFiles(DataOutputStream writer, String fieldName, File file) throws Exception {
        final String LINE_FEED = "\r\n";
        writer.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\";filename=\"" + file.getName() + "\"" + LINE_FEED);
        writer.writeBytes(LINE_FEED);

        FileInputStream fStream = new FileInputStream(file);
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int length = -1;

        while ((length = fStream.read(buffer)) != -1) {
            writer.write(buffer, 0, length);
        }
        writer.writeBytes(LINE_FEED);
        fStream.close();
    }
}
