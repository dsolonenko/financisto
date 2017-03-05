package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ToggleButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import ru.orangesoftware.financisto.R;

@EActivity(R.layout.activity_request_permissions)
public class RequestPermissionActivity extends Activity {

    @ViewById(R.id.toggleWriteStorage)
    ToggleButton toggleWriteStorage;

    @ViewById(R.id.toggleCamera)
    ToggleButton toggleCamera;

    @AfterViews
    public void initViews() {
        checkPermissions();
    }

    private void checkPermissions() {
        disableToggleIfGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, toggleWriteStorage);
        disableToggleIfGranted(Manifest.permission.CAMERA, toggleCamera);
    }

    private void disableToggleIfGranted(String permission, ToggleButton toggleButton) {
        if (isGranted(permission)) {
            toggleButton.setChecked(true);
            toggleButton.setEnabled(false);
        }
    }

    @Click(R.id.toggleWriteStorage)
    public void onGrantWriteStorage() {
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, toggleWriteStorage);
    }

    @Click(R.id.toggleCamera)
    public void onGrantCamera() {
        requestPermission(Manifest.permission.CAMERA, toggleCamera);
    }

    private void requestPermission(String permission, ToggleButton toggleButton) {
        toggleButton.setChecked(false);
        ActivityCompat.requestPermissions(this, new String[]{permission}, 0);
    }

    private boolean isGranted(String permission) {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        checkPermissions();
    }

}
