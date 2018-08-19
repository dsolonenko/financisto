package ru.orangesoftware.financisto.utils;

import java.lang.reflect.Array;
import java.util.List;

public abstract class ArrUtils {
    private ArrUtils() {}

    @SuppressWarnings("unchecked")
    public static <T> T[] listToArr(List<T> list, Class<T> clazz) {
        if (list == null) return null;
        T[] res = (T[]) Array.newInstance(clazz, list.size());
        return list.toArray(res);
    }

    public static String[] strListToArr(List<String> list) {
        if (list == null) return null;
        return list.toArray(new String[list.size()]);
    }

    public static String[] joinArrays(String[] a1, String[] a2) {
        if (a1.length == 0) {
            return a2;
        }
        String[] a = new String[a1.length+a2.length];
        System.arraycopy(a1, 0, a, 0, a1.length);
        System.arraycopy(a2, 0, a, a1.length, a2.length);
        return a;
    }
}
