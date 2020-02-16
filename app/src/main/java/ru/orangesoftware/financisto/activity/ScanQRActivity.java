package ru.orangesoftware.financisto.activity;

import static ru.orangesoftware.financisto.activity.AbstractTransactionActivity.QR_DATA_EXTRA;
import static ru.orangesoftware.financisto.activity.RequestPermission.isRequestingPermission;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView.OnQRCodeReadListener;
import ru.orangesoftware.financisto.R;

public class ScanQRActivity extends Activity implements OnQRCodeReadListener {

    private ViewGroup mainLayout;
    private QRCodeReaderView qrCodeReaderView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
        mainLayout = findViewById(R.id.main_layout);
        if (isRequestingPermission(this, Manifest.permission.CAMERA)) {
            return;
        }
        initQRCodeReaderView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (qrCodeReaderView != null) {
            qrCodeReaderView.startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (qrCodeReaderView != null) {
            qrCodeReaderView.stopCamera();
        }
    }

    // Called when a QR is decoded
    // "text" : the text encoded in QR
    // "points" : points where QR control points are placed
    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        Intent intent = new Intent();
        intent.putExtra(QR_DATA_EXTRA, text);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void initQRCodeReaderView() {
        View content = getLayoutInflater().inflate(R.layout.content_decoder, mainLayout, true);

        qrCodeReaderView = content.findViewById(R.id.qrdecoderview);
        CheckBox flashlightCheckBox = content.findViewById(R.id.flashlight_checkbox);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            qrCodeReaderView.setAutofocusInterval(2000L);
        }
        qrCodeReaderView.setOnQRCodeReadListener(this);
        qrCodeReaderView.setBackCamera();
        flashlightCheckBox.setOnCheckedChangeListener(
            (compoundButton, isChecked) -> qrCodeReaderView.setTorchEnabled(isChecked)
        );
        qrCodeReaderView.setQRDecodingEnabled(true);
        qrCodeReaderView.startCamera();
    }
}