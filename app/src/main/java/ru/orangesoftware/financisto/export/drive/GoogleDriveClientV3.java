package ru.orangesoftware.financisto.export.drive;

import android.content.Context;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EBean;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.db.DatabaseAdapter;

@EBean(scope = EBean.Scope.Singleton)
public class GoogleDriveClientV3 {

    private final Context mContext;

    @Bean
    GreenRobotBus bus;

    @Bean
    DatabaseAdapter db;

    public GoogleDriveClientV3(Context context) {
        mContext = context.getApplicationContext();
    }

    @AfterInject
    public void init() {
        bus.register(this);
    }

    public void uploadFile(File f) {
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void doBackup(DoDriveBackupEvent event) { }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void listFiles(DoDriveListFilesEvent event) { }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void doRestore(DoDriveRestoreEvent event) { }
}
