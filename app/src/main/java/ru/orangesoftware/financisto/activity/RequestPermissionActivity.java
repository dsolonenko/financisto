package ru.orangesoftware.financisto.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;

import ru.orangesoftware.financisto.R;

@EActivity(R.layout.activity_request_permissions)
public class RequestPermissionActivity extends Activity {

    @Extra("requestedPermission")
    String requestedPermission;

    @ViewById(R.id.toggleWriteStorageWrap)
    LinearLayout toggleWriteStorageWrap;

    @ViewById(R.id.toggleWriteStorage)
    ToggleButton toggleWriteStorage;


    @ViewById(R.id.toggleGetAccountsWrap)
    LinearLayout toggleGetAccountsWrap;

    @ViewById(R.id.toggleGetAccounts)
    ToggleButton toggleGetAccounts;

    @ViewById(R.id.toggleCameraWrap)
    LinearLayout toggleCameraWrap;

    @ViewById(R.id.toggleCamera)
    ToggleButton toggleCamera;

    @AfterViews
    public void initViews() {
        checkPermissions();
    }

    private void checkPermissions() {
        disableToggleIfGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE, toggleWriteStorage, toggleWriteStorageWrap);
        disableToggleIfGranted(Manifest.permission.GET_ACCOUNTS, toggleGetAccounts, toggleGetAccountsWrap);
        disableToggleIfGranted(Manifest.permission.CAMERA, toggleCamera, toggleCameraWrap);
    }

    private void disableToggleIfGranted(String permission, ToggleButton toggleButton, LinearLayout wrapLayout) {
        if (isGranted(permission)) {
            toggleButton.setChecked(true);
            toggleButton.setEnabled(false);
            wrapLayout.setBackgroundResource(0);
        } else if (permission.equals(requestedPermission)) {
            wrapLayout.setBackgroundResource(R.drawable.highlight_border);
        }
    }

    @Click(R.id.toggleWriteStorage)
    public void onGrantWriteStorage() {
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, toggleWriteStorage);
    }

    @Click(R.id.toggleGetAccounts)
    public void onGrantGetAccounts() {
        requestPermission(Manifest.permission.GET_ACCOUNTS, toggleGetAccounts);
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
