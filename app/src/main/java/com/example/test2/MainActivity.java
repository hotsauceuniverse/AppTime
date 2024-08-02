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
import android.graphics.Color;
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
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.BuildConfig;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private CheckPackageNameThread checkPackageNameThread;
    boolean operation = false;
    private UsageStatsManager usageStatsManager;
    private Handler mHandler;
    private ImageView preBtn;
    private ImageView nextBtn;
    private TextView monthDay;
    private BarChart barChart;
    private AppAdapter mAppAdapter;

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

        CalendarUtil.selectDate = LocalDate.now();

        preBtn = findViewById(R.id.pre_btn);
        nextBtn = findViewById(R.id.next_btn);
        monthDay = findViewById(R.id.month_day);
        barChart = findViewById(R.id.bar_chart);

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

        this.mHandler = new Handler();

        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            return;
        }
        mRunnable.run();

        if (!checkPermission()) {
            Intent PermissionIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:" + getPackageName()));
            startActivity(PermissionIntent);
        } else {
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

    private class CheckPackageNameThread extends Thread {
        public void run() {
            while (operation) {
                if (!checkPermission()) continue;
                Log.d("Current Package", getPackageName(getApplicationContext()));
                try {
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
                    Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }

        return granted;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static String getPackageName(@NonNull Context context) {
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long lastRunAppTimeStamp = 0L;
        final long INTERVAL = 1000 * 60 * 5;
        final long end = System.currentTimeMillis();
        final long begin = end - INTERVAL;

        LongSparseArray<String> packageNameMap = new LongSparseArray<>();
        final UsageEvents usageEvents = usageStatsManager.queryEvents(begin, end);

        while (usageEvents.hasNextEvent()) {
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);

            if (isForeGroundEvent(event)) {
                packageNameMap.put(event.getTimeStamp(), event.getPackageName());

                if (event.getTimeStamp() > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.getTimeStamp();
                }
            }
        }
        return packageNameMap.get(lastRunAppTimeStamp, "").toString();
    }

    private static boolean isForeGroundEvent(UsageEvents.Event event) {
        if (event == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED;
        }
        return event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showAppUsageStats(LocalDate date) {
        long endTime = getEndOfDay(date);
        long beginTime = getBeginOfDay(date);

        List<UsageStats> stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);

        if (stats != null) {
            ArrayList<BarEntry> entries = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            ArrayList<AppItem> installedApps = getUserInstalledApps();
            PackageManager pm = getPackageManager();
            Map<String, String> appNameMap = new HashMap<>();

            for (AppItem appItem : installedApps) {
                appNameMap.put(appItem.getPackageName(), appItem.getAppName());
            }

            for (int i = 0; i < stats.size(); i++) {
                UsageStats usageStats = stats.get(i);
                long timeInForeground = usageStats.getTotalTimeInForeground();

                int hours = (int) ((timeInForeground / (1000 * 60 * 60)) % 24);
                int minutes = (int) ((timeInForeground / (1000 * 60)) % 60);
                int seconds = (int) (timeInForeground / 1000) % 60;

                if (hours > 0 || minutes > 0 || seconds > 0) {
                    String packageName = usageStats.getPackageName();
                    String appName;
                    try {
                        appName = pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString();
                    } catch (PackageManager.NameNotFoundException e) {
                        appName = packageName;
                    }

                    float usageTimeInSeconds = hours * 3600 + minutes * 60 + seconds;
                    entries.add(new BarEntry(entries.size(), usageTimeInSeconds));
                    labels.add(appName);
                }
            }

            BarDataSet dataSet = new BarDataSet(entries, "사용 시간(초)");
            BarData barData = new BarData(dataSet);
            barChart.setData(barData);

            XAxis xAxis = barChart.getXAxis();
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setGranularity(1f);

            // Y축 설정 (시간 단위로 정수 값 표시)
            YAxis leftAxis = barChart.getAxisLeft();
            leftAxis.setGranularity(1f); // Y축의 최소 단위, 정수 값으로 설정
            leftAxis.setGranularityEnabled(true); // 최소 단위 사용을 강제
            leftAxis.setAxisMinimum(0f); // Y축의 최소 값

            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    if (value == 0) {
                        return "0시간";
                    } else {
                        // Y축의 값을 시간으로 변환
                        int hours = (int) (value / 2999); // 3000을 기준으로 시간으로 변환
                        return hours + "시간";
                    }
                }
            });

            barChart.getAxisRight().setEnabled(false);
            barChart.setFitBars(true);
            barChart.invalidate();
        }
    }

    private long getBeginOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

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