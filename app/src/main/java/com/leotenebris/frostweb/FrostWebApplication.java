package com.leotenebris.frostweb;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.leotenebris.frostweb.History.HistoryItem;
import com.leotenebris.frostweb.Tabs.TabItem;

import java.util.ArrayList;

public class FrostWebApplication extends Application {
    private static FrostWebApplication instance;

    public static final String KEY_TABS = "Tabs";
    public static final String KEY_HISTORY = "History";

    public static ArrayList<TabItem> tabs;
    public static ArrayList<HistoryItem> browserHistory;

    public FrostWebApplication() {
        instance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(this::handleUncaughtException);
    }

    public void handleUncaughtException(Thread thread, Throwable e) {
        String stackTrace = Log.getStackTraceString(e);
        String message = e.getMessage();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"rasitayaz1358@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "FrostWeb crash log");
        intent.putExtra(Intent.EXTRA_TEXT, stackTrace);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        startActivity(intent);
    }

    public static int dpToPx(int dp) {
        float density = instance.getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }

    public void saveHistory() {
        SharedPreferences.Editor editor = getSharedPreferences(KEY_HISTORY, MODE_PRIVATE).edit();
        Gson gson = new Gson();
        String json = gson.toJson(browserHistory);
        editor.putString(KEY_HISTORY, json);
        editor.apply();
    }

    public void saveTabs() {
        SharedPreferences.Editor editor = getSharedPreferences(KEY_TABS, MODE_PRIVATE).edit();
        Gson gson = new Gson();
        String json = gson.toJson(tabs);
        editor.putString(KEY_TABS, json);

        /*for (int i = 0; i < tabs.size(); i++) {
            TabItem tab = tabs.get(i);
            saveBitmapToInternalStorage(tab.getFavicon(), tab.getUniqueId());
        }*/

        editor.apply();
    }

    public static FrostWebApplication getInstance() {
        return instance;
    }
}
