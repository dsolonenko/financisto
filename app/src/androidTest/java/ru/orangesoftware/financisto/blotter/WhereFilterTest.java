/*
 * Copyright (c) 2011 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.blotter;

import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import org.junit.Assert;
import ru.orangesoftware.financisto.filter.Criteria;
import ru.orangesoftware.financisto.filter.WhereFilter;

import java.util.Arrays;

import static ru.orangesoftware.financisto.filter.WhereFilter.FILTER_EXTRA;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 8/1/11 10:35 PM
 */
public class WhereFilterTest extends AndroidTestCase {

    public void test_many_operands_criteria() {
        WhereFilter filter = WhereFilter.empty();
        filter.put(Criteria.btw("category_left", "1", "2"));
        filter.put(Criteria.btw("category_left0", "11", "12", "21", "22"));
        filter.put(Criteria.btw("category_left1", "11", "12"));
        filter.put(Criteria.in("project_id", "21"));
        filter.put(Criteria.in("payee_id", "31", "32"));
        filter.put(Criteria.in("location_id", "41", "42", "43"));
        filter.put(Criteria.in("template_id", "51", "52", "53", "54"));

        assertEquals("category_left BETWEEN ? AND ? AND (category_left0 BETWEEN ? AND ? OR category_left0 BETWEEN ? AND ?) AND category_left1 BETWEEN ? AND ? AND project_id IN (?) AND payee_id IN (?,?) AND location_id IN (?,?,?) AND template_id IN (?,?,?,?)", 
                filter.getSelection());
        assertEquals(new String[]{"1", "2", "11", "12", "21", "22", "11", "12", "21", "31", "32", "41", "42", "43", "51", "52", "53", "54"}, filter.getSelectionArgs());

        final Bundle b = new Bundle();
        filter.toBundle(b);
        final String[] filterArr = b.getStringArray(FILTER_EXTRA);
        final String[] serialized = {
                "category_left,BTW,1,2",
                "category_left0,BTW,11,12,21,22",
                "category_left1,BTW,11,12",
                "project_id,IN,21",
                "payee_id,IN,31,32",
                "location_id,IN,41,42,43",
                "template_id,IN,51,52,53,54"};
        Assert.assertArrayEquals(serialized, filterArr);


        final WhereFilter restoredFilter = WhereFilter.fromBundle(b);
        assertEquals("category_left BETWEEN ? AND ? AND (category_left0 BETWEEN ? AND ? OR category_left0 BETWEEN ? AND ?) AND category_left1 BETWEEN ? AND ? AND project_id IN (?) AND payee_id IN (?,?) AND location_id IN (?,?,?) AND template_id IN (?,?,?,?)",
                restoredFilter.getSelection());

        assertEquals(new String[]{"1", "2", "11", "12", "21", "22", "11", "12", "21", "31", "32", "41", "42", "43", "51", "52", "53", "54"}, restoredFilter.getSelectionArgs());
        
    }
    
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
