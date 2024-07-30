package com.example.test2;


import android.graphics.drawable.Drawable;

public class AppItem {
    private String appName;
    private Drawable appIcon;
    private String appTime;
    private String packageName;

    public AppItem(String appName, Drawable appIcon, String appTime, String packageName) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.appTime = appTime;
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public Drawable getAppIcon() {
        return appIcon;
    }

    public String getAppTime() {
        return appTime;
    }

    public String getPackageName() {
        return packageName;
    }
}

