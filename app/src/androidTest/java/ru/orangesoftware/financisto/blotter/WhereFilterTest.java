/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.blotter;

import android.content.Intent;
import android.test.AndroidTestCase;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/1/11 10:35 PM
 */
public class WhereFilterTest extends AndroidTestCase {

    public void test_filter_should_support_raw_criteria() {
        WhereFilter filter = givenFilterWithRawCriteria();
        assertFilterSelection(filter);
    }

    public void test_should_save_and_restore_raw_criteria() {
        //given
        WhereFilter filter = givenFilterWithRawCriteria();
        Intent intent = new Intent();
        //when
        filter.toIntent(intent);
        WhereFilter copy = WhereFilter.fromIntent(intent);
        //then
        assertFilterSelection(copy);
    }

    private WhereFilter givenFilterWithRawCriteria() {
        WhereFilter filter = WhereFilter.empty();
        filter.put(Criteria.eq("from_account_id", "1"));
        filter.put(Criteria.raw("parent_id=0 OR is_transfer=-1"));
        return filter;
    }

    private void assertFilterSelection(WhereFilter filter) {
        assertEquals("from_account_id =? AND (parent_id=0 OR is_transfer=-1)", filter.getSelection());
        assertEquals(new String[]{"1"}, filter.getSelectionArgs());
    }

    public static void assertEquals(String[] expected, String[] actual) {
        assertEquals(Arrays.toString(expected), Arrays.toString(actual));
    }

}
