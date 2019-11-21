/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import ru.orangesoftware.financisto.db.AbstractDbTest;

import static org.junit.Assert.*;

public class IntegrityCheckAutobackupTest extends AbstractDbTest {

    IntegrityCheckAutobackup integrity;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        integrity = new IntegrityCheckAutobackup(getContext(), TimeUnit.MILLISECONDS.toMillis(100));
    }

    @Test
    public void should_check_if_autobackup_has_been_disabled() {
        // when reminder is enabled
        givenAutobackupReminderEnabledIs(true);
        givenAutobackupEnabledIs(false);

        givenFirstRunAfterRelease();
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);

        sleepMillis(50);
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);
        sleepMillis(51);
        // raise the info notification
        assertEquals(IntegrityCheck.Level.INFO, integrity.check().level);
        // and reset the check at the same time
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);

        givenAutobackupEnabledIs(true);
        sleepMillis(101);
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);

        // when reminder is disabled
        givenAutobackupReminderEnabledIs(false);
        givenAutobackupEnabledIs(false);
        sleepMillis(101);
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);
    }

    @Test
    public void should_check_if_the_last_autobackup_has_failed() {
        // when reminder is enabled
        givenAutobackupWarningEnabledIs(true);
        givenAutobackupEnabledIs(true);

        givenFirstRunAfterRelease();
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);

        givenTheLastAutobackupHasFailed();
        // raise the info notification
        assertEquals(IntegrityCheck.Level.ERROR, integrity.check().level);
        // and reset the check at the same time
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);

        givenTheLastAutobackupHasFailed();
        givenTheLastAutobackupHasSucceeded();
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);

        // when reminder is disabled
        givenAutobackupWarningEnabledIs(false);
        givenTheLastAutobackupHasFailed();
        assertEquals(IntegrityCheck.Level.OK, integrity.check().level);
    }

    private void givenTheLastAutobackupHasSucceeded() {
        MyPreferences.notifyAutobackupSucceeded(getContext());
    }

    private void givenTheLastAutobackupHasFailed() {
        MyPreferences.notifyAutobackupFailed(getContext(), new Exception("Error!"));
    }

    private void givenAutobackupEnabledIs(boolean isEnabled) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.edit().putBoolean("auto_backup_enabled", isEnabled).commit();
    }

    private void givenAutobackupReminderEnabledIs(boolean isEnabled) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.edit().putBoolean("auto_backup_reminder_enabled", isEnabled).commit();
    }

    private void givenAutobackupWarningEnabledIs(boolean isEnabled) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.edit().putBoolean("auto_backup_warning_enabled", isEnabled).commit();
    }

    private void givenFirstRunAfterRelease() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.edit()
                .remove("last_autobackup_check")
                .remove("auto_backup_failed_notify")
                .commit();
    }

    private void sleepMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
