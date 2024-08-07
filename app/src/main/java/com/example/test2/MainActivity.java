package com.example.test2;

import android.Manifest;
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
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.BuildConfig;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CheckPackageNameThread checkPackageNameThread;
    boolean operation = false;
    private UsageStatsManager usageStatsManager;
    private RecyclerView appRecyclerView;
    private AppAdapter mAppAdapter;
    private Handler mHandler;
    private ImageView preBtn;
    private ImageView nextBtn;
    private TextView monthDay;

    // 1초마다 자동 갱신
    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            showAppUsageStats(CalendarUtil.selectDate);
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // build.gradle defaultConfig minSdk 26 target upgrade
        CalendarUtil.selectDate = LocalDate.now();

        preBtn = findViewById(R.id.pre_btn);
        nextBtn = findViewById(R.id.next_btn);
        monthDay = findViewById(R.id.month_day);

        setMonthDay();

        preBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CalendarUtil.selectDate = CalendarUtil.selectDate.minusDays(1);
                setMonthDay();
                showAppUsageStats(CalendarUtil.selectDate);
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CalendarUtil.selectDate = CalendarUtil.selectDate.plusDays(1);
                setMonthDay();
                showAppUsageStats(CalendarUtil.selectDate);
            }
        });

        appRecyclerView = findViewById(R.id.app_recyclerView);
        appRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ArrayList<AppItem> userInstalledApps = getUserInstalledApps();
        mAppAdapter = new AppAdapter();
        mAppAdapter.setAppItems(userInstalledApps);
        appRecyclerView.setAdapter(mAppAdapter);

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

    private String monthDayFormat(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월 dd일");
        return date.format(formatter);
    }

    private void setMonthDay() {
        monthDay.setText(monthDayFormat(CalendarUtil.selectDate));
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
                Process.myUid(), getApplicationContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (getApplicationContext().checkCallingOrSelfPermission(
                    Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }

        return granted;
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
//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private void showAppUsageStats(LocalDate date) {
//        long endTime = getEndOfDay(date); // 현재 시간을 종료 시간으로 설정
//        Log.d("endTime", String.valueOf(endTime));
//
//        long beginTime = getBeginOfDay(date); // 오늘 자정 시간을 시작 시간으로 설정
//        Log.d("beginTime", String.valueOf(beginTime));
//
//        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);
//
//        if (stats != null) {
//            ArrayList<AppItem> appItems = new ArrayList<>();
////            for (UsageStats usageStats : stats) {     // for each 문 => for (각 요소 값 : 배열이나 컨테이너 값) { 반복 수행할 작업 }
//            for (int i = 0; i < stats.size(); i++) {
//                UsageStats usageStats = stats.get(i);
//                long timeInForeground = usageStats.getTotalTimeInForeground();
//                // 1초 = 1,000
//                // 1분 = 1,000 * 60
//                // 1시간 = 1,000 * 60 * 60
//                // 1일 = 1,000 * 60 * 60 * 24
//                int hours = (int) ((timeInForeground / (1000 * 60 * 60)) % 24);     // 24로 나눠 24시간 형식으로 시간 제한
//                int minutes = (int) ((timeInForeground / (1000 * 60)) % 60);        // 60으로 나눠 분 단위를 0 ~ 59까지 제한
//                int seconds = (int) (timeInForeground / 1000) % 60;                 // 60으로 나눠 초 단위를 0 ~ 59까지 제한
//
//                if (hours > 0 || minutes > 0 || seconds > 0) {
//                    String packageName = usageStats.getPackageName();
//                    PackageManager pm = getPackageManager();
//                    String appName;
//                    Drawable appIcon;
//                    try {
//                        appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
//                        appIcon = pm.getApplicationIcon(packageName);
//                    } catch (PackageManager.NameNotFoundException e) {
//                        appName = packageName;
//                        appIcon = getDrawable(R.mipmap.ic_launcher);
//                    }
//                    String usageTime = hours + "h:" + minutes + "m:" + seconds + "s";
//                    appItems.add(new AppItem(appName, appIcon, usageTime, packageName));
//                }
//            }
//            mAppAdapter.setAppItems(appItems);
//        }
//    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showAppUsageStats(LocalDate date) {
        long endTime = getEndOfDay(date); // 현재 시간을 종료 시간으로 설정
        long beginTime = getBeginOfDay(date); // 오늘 자정 시간을 시작 시간으로 설정

        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);

        // 24시간에 대한 시간 슬롯 리스트를 생성합니다.
        ArrayList<AppItem> appItems = new ArrayList<>(24);
        for (int i = 0; i < 24; i++) {
            appItems.add(new AppItem("", null, "", "")); // 빈 값으로 초기화
        }

        if (stats != null) {
            for (UsageStats usageStats : stats) {
                long timeInForeground = usageStats.getTotalTimeInForeground();
                int hours = (int) ((timeInForeground / (1000 * 60 * 60)) % 24); // 24시간 형식으로 제한

                if (timeInForeground > 0) {
                    String packageName = usageStats.getPackageName();
                    PackageManager pm = getPackageManager();
                    String appName;
                    Drawable appIcon;
                    try {
                        appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                        appIcon = pm.getApplicationIcon(packageName);
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                        appIcon = getDrawable(R.mipmap.ic_launcher);
                    }
                    String usageTime = hours + "h:" + ((timeInForeground / (1000 * 60)) % 60) + "m:" + (timeInForeground / 1000 % 60) + "s";
                    appItems.set(hours, new AppItem(appName, appIcon, usageTime, packageName));
                }
            }
        }
        mAppAdapter.setAppItems(appItems);
    }

    // 특정 날짜의 자정 시간을 반환 (00시 00분 00초 000밀리초)
    private long getBeginOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // 특정 날짜의 끝 시간을 반환 (23시 59분 59초 999밀리초)
    private long getEndOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
    }

    // 사용자가 설치한 파일 확인
    private ArrayList<AppItem> getUserInstalledApps() {
        ArrayList<AppItem> installedApps = new ArrayList<>();
        PackageManager packageManager = getPackageManager();
        List<PackageInfo> packageList = packageManager.getInstalledPackages(0);

        for (PackageInfo packageInfo : packageList) {
            // 시스템 앱이 아닌 사용자 앱만을 선택
            // ApplicationInfo.FLAG_SYSTEM 플래그가 설정되지 않은 앱만을 필터링
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String appName = packageInfo.applicationInfo.loadLabel(packageManager).toString();
                Log.d("appName", appName);
                String packageName = packageInfo.packageName;
                Drawable appIcon = packageInfo.applicationInfo.loadIcon(packageManager);
                installedApps.add(new AppItem(appName, appIcon, "", packageName));
            }
        }
        return installedApps;
    }
}