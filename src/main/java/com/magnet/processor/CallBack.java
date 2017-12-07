package com.magnet.processor;

/**
 * Created by yadhukrishnan.e@oneteam.us
 */

public interface CallBack<T> {
    void successResponse(int responseCode, T t);
    void failureResponse(Exception ex);
    void offline();
}
