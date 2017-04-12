package ru.orangesoftware.financisto.app;

import android.support.multidex.MultiDexApplication;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;

import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.export.drive.GoogleDriveClient;

@EApplication
public class FinancistoApp extends MultiDexApplication {

    @Bean
    public GreenRobotBus bus;

    @Bean
    public GoogleDriveClient driveClient;

    @AfterInject
    public void init() {
        bus.register(driveClient);
    }

}
