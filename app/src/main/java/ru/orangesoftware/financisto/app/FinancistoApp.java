package ru.orangesoftware.financisto.app;

import static org.koin.core.context.DefaultContextExtKt.stopKoin;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.NonNull;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EApplication;

import ru.orangesoftware.financisto.bus.GreenRobotBus;
import ru.orangesoftware.financisto.utils.MyPreferences;

@EApplication
public class FinancistoApp extends Application {

    @Bean
    public GreenRobotBus bus;

//    @Bean
//    public GoogleDriveClient driveClient;

    @AfterInject
    public void init() {
//        bus.register(driveClient);
        JavaAppKoinKt.start(FinancistoApp_.getInstance());
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(MyPreferences.switchLocale(base));
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        MyPreferences.switchLocale(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        stopKoin();
    }
}
