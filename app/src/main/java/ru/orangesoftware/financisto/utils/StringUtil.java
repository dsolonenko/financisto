/*
 * Copyright (c) 2014 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 1/17/14
 * Time: 12:15 AM
 */
public class StringUtil {

    public static String capitalize(String s) {
        if (s == null || s.length() == 0) return s;
        char[] stringArray = s.toCharArray();
        stringArray[0] = Character.toUpperCase(stringArray[0]);
        return new String(stringArray);
    }

    public static boolean isEmpty(String appId) {
        return appId == null || getTrimmedLength(appId) == 0;
    }

    private static int getTrimmedLength(CharSequence s) {
        int len = s.length();

        int start = 0;
        while (start < len && s.charAt(start) <= ' ') {
            start++;
        }

        int end = len;
        while (end > start && s.charAt(end - 1) <= ' ') {
            end--;
        }

        return end - start;
    }

    public static String getShortString(String str, int width) {
        if(!isEmpty(str) && str.length() > width) {
            return str.substring(0, width - 3) + "...";
        } else {
            return str;
        }
    }

    /**
     * Fast replacement w/o using regexps
     * from <a href=https://stackoverflow.com/a/12026782/365675>here</a>
     */
    public static String replaceAllIgnoreCase(String source, String target, String replacement) {
        if (source == null || target == null || target.isEmpty()) {
            return source;
        }
        if (target.length() > source.length()) {
            return source;
        }

        StringBuilder sbSource = new StringBuilder(source);
        StringBuilder sbSourceLower = new StringBuilder(source.toLowerCase());
        String searchString = target.toLowerCase();

        int idx = 0;
        while((idx = sbSourceLower.indexOf(searchString, idx)) != -1) {
            sbSource.replace(idx, idx + searchString.length(), replacement);
            sbSourceLower.replace(idx, idx + searchString.length(), replacement);
            idx+= replacement.length();
        }
        return sbSource.toString();
    }

}
