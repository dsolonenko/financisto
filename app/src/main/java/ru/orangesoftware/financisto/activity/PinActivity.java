/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package ru.orangesoftware.financisto.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mtramin.rxfingerprint.RxFingerprint;

import io.reactivex.disposables.Disposable;
import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.PinProtection;
import ru.orangesoftware.financisto.view.PinView;

public class PinActivity extends Activity implements PinView.PinListener {

    public static final String SUCCESS = "PIN_SUCCESS";

    private Disposable disposable;

    private final Handler handler = new Handler();

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String pin = MyPreferences.getPin(this);
        if (pin == null) {
            onSuccess(null);
        } else if (RxFingerprint.isAvailable(this) && MyPreferences.isPinLockUseFingerprint(this)) {
            setContentView(R.layout.lock_fingerprint);
            askForFingerprint();
        } else {
            usePinLock();
        }
    }

    private void usePinLock() {
        String pin = MyPreferences.getPin(this);
        PinView v = new PinView(this, this, pin, R.layout.lock);
        setContentView(v.getView());
    }

    private void askForFingerprint() {
        View usePinButton = findViewById(R.id.use_pin);
        if (MyPreferences.isUseFingerprintFallbackToPinEnabled(this)) {
            usePinButton.setOnClickListener(v -> {
                disposeFingerprintListener();
                usePinLock();
            });
        } else {
            usePinButton.setVisibility(View.GONE);
        }
        disposable = RxFingerprint.authenticate(this).subscribe(
                result -> {
                    switch (result.getResult()) {
                        case AUTHENTICATED:
                            setFingerprintStatus(R.string.fingerprint_auth_success, R.drawable.ic_check_circle_black_48dp, R.color.material_teal);
                            handler.postDelayed(() -> onSuccess(null), 200);
                            break;
                        case FAILED:
                            setFingerprintStatus(R.string.fingerprint_auth_failed, R.drawable.ic_error_black_48dp, R.color.material_orange);
                            break;
                        case HELP:
                            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                            break;
                    }
                },
                throwable -> {
                    setFingerprintStatus(R.string.fingerprint_error, R.drawable.ic_error_black_48dp, R.color.holo_red_dark);
                    Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
    }

    private void setFingerprintStatus(int messageResId, int iconResId, int colorResId) {
        TextView status = findViewById(R.id.fingerprint_status);
        ImageView icon = findViewById(R.id.fingerprint_icon);
        int color = getResources().getColor(colorResId);
        status.setText(messageResId);
        status.setTextColor(color);
        icon.setImageResource(iconResId);
        icon.setColorFilter(color);
    }

    @Override
    public void onConfirm(String pinBase64) {
    }

    @Override
    public void onSuccess(String pinBase64) {
        disposeFingerprintListener();
        PinProtection.pinUnlock(this);
        Intent data = new Intent();
        data.putExtra(SUCCESS, true);
        setResult(RESULT_OK, data);
        finish();
    }

    private void disposeFingerprintListener() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
