package com.example.test2;


import android.graphics.drawable.Drawable;

public class AppItem {
    private String appName;
    private Drawable appIcon;
    private String appTime;

    public AppItem(String appName, Drawable appIcon, String appTime) {
        this.appName = appName;
        this.appIcon = appIcon;
        this.appTime = appTime;
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
}

