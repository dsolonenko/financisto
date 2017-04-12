package ru.orangesoftware.financisto.export.drive;

import com.google.android.gms.common.api.Status;

public class DriveBackupFailure {

    public final Status status;

    public DriveBackupFailure(Status status) {
        this.status = status;
    }

}
