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

}
