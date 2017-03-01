/*
 * Copyright (c) 2011 Timo Lindhorst.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
package ru.orangesoftware.financisto.utils;


import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import ru.orangesoftware.financisto.activity.PinActivity;

public class PinProtection {

    private static final int MIN_DELTA_TIME_MS = 3000;

    private static interface LockState {
        LockState lock(Context c);
        LockState unlock(Context c);
    }

    private static final LockState LOCKED = new LockState() {
        @Override
        public LockState lock(Context c) {
            return this;
        }
        @Override
        public LockState unlock(Context c) {
            if (MyPreferences.isPinProtected(c)) {
                askForPin(c);
                return this;
            }
            return UNLOCKED;
        }
    };

    private static final LockState UNLOCKED = new LockState() {
        private long lockTime = 0;

        @Override
        public LockState lock(Context c) {
            lockTime = System.currentTimeMillis();
            return this;
        }

        @Override
        public LockState unlock(Context c) {
            int lockWaitTime = MyPreferences.getLockTimeSeconds(c);
            if (lockWaitTime > 0) {
                long curTime = System.currentTimeMillis();
                long lockTimeMs = Math.max(MIN_DELTA_TIME_MS, TimeUnit.MILLISECONDS.convert(lockWaitTime, TimeUnit.SECONDS));
                long deltaTimeMs = curTime - lockTime;
                if (deltaTimeMs > lockTimeMs) {
                    askForPin(c);
                    return LOCKED;
                }
            }
            return this;
        }
    };

    private static LockState currentState = LOCKED;

    private static void askForPin(Context c) {
        Intent intent = new Intent(c, PinActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        c.startActivity(intent);
    }

    public static void lock(Context c) {
        currentState = currentState.lock(c);
    }

    public static void unlock(Context c) {
        currentState = currentState.unlock(c);
    }

    public static void immediateLock(Context c) {
        currentState = LOCKED;
    }

    public static void pinUnlock(Context c) {
        currentState = UNLOCKED;
        // little hack to reset lockTime in the state
        currentState.lock(c);
    }

    public static boolean isUnlocked() {
        return currentState == UNLOCKED;
    }

}
