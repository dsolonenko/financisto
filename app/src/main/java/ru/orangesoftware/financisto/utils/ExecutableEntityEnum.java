/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

/**
 * Created by IntelliJ IDEA.
 * User: solomonk
 * Date: 5/2/12
 * Time: 11:14 PM
 */
public interface ExecutableEntityEnum<V> extends EntityEnum {
    
    public void execute(V value);
    
}
