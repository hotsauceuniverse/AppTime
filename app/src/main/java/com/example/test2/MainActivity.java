package com.example.test2;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.util.LongSparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.BuildConfig;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    CheckPackageNameThread checkPackageNameThread;
    boolean operation = false;
    private UsageStatsManager usageStatsManager;
    RecyclerView appRecyclerView;
    AppAdapter mAppAdapter;
    Handler mHandler;
    private boolean isMidnightAlreadyHandled = false; // 자정 처리 여부를 나타내는 플래그

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            showAppUsageStats();
            mHandler.postDelayed(this, 1000);

            // 자정에 리스트 초기화
            if (isMidnight() && !isMidnightAlreadyHandled) {
                mAppAdapter.clear(); // 리스트 초기화
                isMidnightAlreadyHandled = true; // 자정 처리 완료
            }

            // 자정 다음에 다시 초기화 플래그를 리셋
            if (!isMidnight()) {
                isMidnightAlreadyHandled = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appRecyclerView = findViewById(R.id.app_recyclerView);
        mAppAdapter = new AppAdapter();
        appRecyclerView.setAdapter(mAppAdapter);
        appRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        this.mHandler = new Handler();

        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return;
        }
        mRunnable.run();

        // 앱 시작 시 권한을 체크하고, 권한이 없으면 설정 화면으로 이동
        if (!checkPermission()) {
            Intent PermissionIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivity(PermissionIntent);
        } else {
            // 권한이 있으면 데이터를 수집 시작
            operation = true;
            checkPackageNameThread = new CheckPackageNameThread();
            checkPackageNameThread.start();
        }
    }

    // 현재 포그라운드 앱 패키지 로그로 띄우는 함수
    private class CheckPackageNameThread extends Thread {

        public void run() {
            // operation == true 일때만 실행
            while (operation) {
                if (!checkPermission())
                    continue;

                // 현재 포그라운드 앱 패키지 이름 가져오기
                Log.d("Current Package", getPackageName(getApplicationContext()));
                try {
                    // 2초마다 패키치 이름을 로그창에 출력
                    sleep(2000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean checkPermission() {
        boolean granted = false;

        AppOpsManager appOps = (AppOpsManager) getApplicationContext()
                .getSystemService(Context.APP_OPS_SERVICE);

        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getApplicationContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (getApplicationContext().checkCallingOrSelfPermission(
                    android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }

        return granted;
    }

    public static String getAppName(Context context) {
        String appName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo i = pm.getPackageInfo(context.getPackageName(), 0);
            appName = i.applicationInfo.loadLabel(pm) + "";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appName;
    }

    // 자신의 앱의 최소 타겟을 롤리팝 이전으로 설정
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    // 현재 포그라운드 앱 패키지를 가져오는 함수
    public static String getPackageName(@NonNull Context context) {

        // UsageStatsManager 선언
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        // 마지막 실행 앱 타임스탬프
        long lastRunAppTimeStamp = 0L;

        // 얼마만큼의 시간동안 수집한 앱의 이름을 가져오는지 정하기 (begin ~ end 까지의 앱 이름을 수집한다)
        final long INTERVAL = 1000 * 60 * 5;
        final long end = System.currentTimeMillis();
        final long begin = end - INTERVAL; // 5분전

        LongSparseArray packageNameMap = new LongSparseArray<>();

        // 수집한 이벤트들을 담기 위한 UsageEvents
        final UsageEvents usageEvents = usageStatsManager.queryEvents(begin, end);

        // 이벤트가 여러개 있을 경우 (최소 존재는 해야 hasNextEvent가 null이 아니니까)
        while (usageEvents.hasNextEvent()) {

            // 현재 이벤트를 가져오기
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);

            // 현재 이벤트가 포그라운드 상태라면(현재 화면에 보이는 앱이라면)
            if (isForeGroundEvent(event)) {

                // 해당 앱 이름을 packageNameMap에 넣는다.
                packageNameMap.put(event.getTimeStamp(), event.getPackageName());

                // 가장 최근에 실행 된 이벤트에 대한 타임스탬프를 업데이트 해준다.
                if (event.getTimeStamp() > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.getTimeStamp();
                }
            }
        }
        // 가장 마지막까지 있는 앱의 이름을 리턴해준다.
        return packageNameMap.get(lastRunAppTimeStamp, "").toString();
    }

    // 앱이 포그라운드 상태인지 체크
    private static boolean isForeGroundEvent(UsageEvents.Event event) {

        // 이벤트가 없으면 false 반환
        if (event == null) {
            return false;
        }

        // 이벤트가 포그라운드 상태라면 true 반환
        if (BuildConfig.VERSION_CODE >= 29) {
            return event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED;
        }

        return event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND;
    }

    // 앱 사용 시간을 계산하고 로그로 출력하는 함수
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showAppUsageStats() {
        long endTime = System.currentTimeMillis(); // 현재 시간을 종료 시간으로 설정
        long beginTime = getTodayMidnight(); // 오늘 자정 시간을 시작 시간으로 설정

        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);

        if (stats != null) {
            ArrayList<AppItem> appItems = new ArrayList<>();
            for (UsageStats usageStats : stats) {
                long timeInForeground = usageStats.getTotalTimeInForeground();
                int hours = (int) ((timeInForeground / (1000 * 60 * 60)) % 24);
                int minutes = (int) ((timeInForeground / (1000 * 60)) % 60);
                int seconds = (int) (timeInForeground / 1000) % 60;

                if (hours > 0 || minutes > 0 || seconds > 0) {
                    String packageName = usageStats.getPackageName();
                    PackageManager pm = getPackageManager();
                    String appName;
                    Drawable appIcon;
                    try {
                        appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                        appIcon = pm.getApplicationIcon(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                        appIcon = getDrawable(R.mipmap.ic_launcher); // 기본 아이콘
                    }

                    String usageTime = hours + "h:" + minutes + "m:" + seconds + "s";
                    appItems.add(new AppItem(appName, appIcon, usageTime));
                }
            }
            mAppAdapter.setAppItems(appItems);
        }
    }

    // 오늘 자정 시간을 반환하는 메서드
    private long getTodayMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // 자정 시간인지 확인하는 메서드
    private boolean isMidnight() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        return hour == 0 && minute == 0 && second == 0;
    }

    // 패키지 이름으로 앱 이름 가져오기
    private String getAppName(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return ai.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }
}