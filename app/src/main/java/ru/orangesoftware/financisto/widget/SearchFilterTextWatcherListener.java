package ru.orangesoftware.financisto.widget;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;

/**
 * https://stackoverflow.com/a/35268540/365675
 */
public abstract class SearchFilterTextWatcherListener implements TextWatcher {
    private final Handler handler = new Handler(Looper.getMainLooper() /*UI thread*/);
    private Runnable workRunnable;
    private final int delayMs;

    public SearchFilterTextWatcherListener(int delayMs) {
        this.delayMs = delayMs;
    }
    
    public abstract void clearFilter(String oldFilter);
    public abstract void applyFilter(String filter);

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        handler.removeCallbacks(workRunnable);
        clearFilter(s.toString());
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // ignore
    }

    @Override
    public void afterTextChanged(Editable s) {
        workRunnable = () -> {
//                    Toast.makeText(SelectTemplateActivity.this, "Filtering...", Toast.LENGTH_SHORT).show();
            applyFilter(s.toString());
        };
        handler.postDelayed(workRunnable, delayMs);
    }
}
