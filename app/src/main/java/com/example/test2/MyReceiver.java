package com.example.test2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        Intent boot = new Intent(context, MainActivity.class);
        boot.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(boot);
    }
}