package com.magnet.processor;

/**
 * Created by yadhukrishnan.e@oneteam.us
 */

class Util {
    static <T>  String covertToString(T value) {
        if (value instanceof Integer) return value + "";
        else if (value instanceof Float) return value + "";
        else if (value instanceof Long) return value + "";
        else if (value instanceof Double) return value + "";
        else return (String) value;
    }
}
