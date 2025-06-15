package com.meda.sejongbikehelper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMapSdk;

import java.lang.reflect.Type;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Toolbar toolbar;
    public Fragment mapFragment;
    public Fragment bookmarkFragment;
    public Fragment gpsLogFragment;
    private BroadcastReceiver mBroadcastReceiver;
    private BroadcastReceiver gpsBroadcastReceiver;
    private MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;
    Intent serviceIntent;
    public static ArrayList<JsonObject> new_bike_list = new ArrayList();
    public static ArrayList<JsonObject> old_bike_list = new ArrayList();
    private RequestQueue queue;
    JsonObject jsonObject;
    JsonArray jsonArray;
    ProgressDialog spinDialog;
    private FragmentManager fragmentManager;
    ArrayList<Station> stations;
    ArrayList<GPS> gpsLogs;

    private static final int REQUEST_POST_NOTIFICATIONS = 112;

    public static class MyDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "gps.db";
        private static final int DATABASE_VERSION = 1;


        private static final String SQL_CREATE_TABLE_GPS = "CREATE TABLE " +
                "gps_data(gpsDataID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "gpsListID TEXT REFERENCES gps_list(gpsListTime), " +
                "latitude REAL, " +
                "longitude REAL, " +
                "timestamp TEXT)";
        private static final String SQL_CREATE_TABLE_GPS_LIST = "CREATE TABLE " +
                "gps_list (" +
                "gpsListTime TEXT PRIMARY KEY," +
                "distance REAL)";

        public MyDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL(SQL_CREATE_TABLE_GPS);
            db.execSQL(SQL_CREATE_TABLE_GPS_LIST);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 데이터베이스 업그레이드시 실행되는 코드
        }
    }


    public boolean addStation(String id, String name, String bikeNum){
        boolean idchkFlag = true;
        for(Station i : stations) { //for문을 통한 전체출력
            if(i.getId().equals(id)){
                idchkFlag = false;
                break;
            }
        }
        if(idchkFlag){
            stations.add(new Station(id, name, bikeNum));
            SaveStationData(stations);
            return true;
        }else{
            return false;
        }

    }
    public void removeStation(int position) {
        stations.remove(position);
        SaveStationData(stations);
    }
    public void removeLog(int position, String time) {
        db.execSQL("DELETE FROM gps_data WHERE gpsListID = "+time);
        db.execSQL("DELETE FROM gps_list WHERE gpsListTime = "+time);
        gpsLogs.remove(position);
    }

    public void historyMapView(String time) {
        String selectQuery = "SELECT * FROM gps_data WHERE gpsListID = "+time+ " ORDER BY timestamp ASC";
        List<LatLng> arrList = new ArrayList<>();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                int gpsDataID = cursor.getInt(0);
                String gpsListID = cursor.getString(1);
                double latitude = cursor.getDouble(2);
                double longitude = cursor.getDouble(3);
                String timestamp = cursor.getString(4);
                arrList.add( new LatLng(latitude, longitude));
            } while (cursor.moveToNext());
        }

        if(arrList.size()<2){
            Toast.makeText(this, "거리가 너무 짧아 표시할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }else{
            long first_timestamp = 0;
            long last_timestamp = 0;
            String firstQuery = "SELECT * FROM gps_data WHERE gpsListID = " + time + " ORDER BY timestamp ASC LIMIT 1";
            cursor = db.rawQuery(firstQuery, null);
            if (cursor.moveToFirst()) {
                first_timestamp = Long.parseLong(cursor.getString(4));
            }
            String lastQuery = "SELECT * FROM gps_data WHERE gpsListID = " + time + " ORDER BY timestamp DESC LIMIT 1";
            cursor = db.rawQuery(lastQuery, null);
            if (cursor.moveToFirst()) {
                last_timestamp =  Long.parseLong(cursor.getString(4));
            }
            String getDistanceQuery = "SELECT distance FROM gps_list WHERE gpsListTime = "+time;
            cursor = db.rawQuery(getDistanceQuery, null);
            double distanceInMeters = 0.0;
            if (cursor.moveToFirst()) {
                distanceInMeters = cursor.getDouble(0);
            }
            cursor.close();

            long timeInMillis = last_timestamp-first_timestamp;
            double timeInSeconds = timeInMillis / 1000.0; // 밀리초를 초 단위로 변환

            double speedInMps = distanceInMeters / timeInSeconds; // 미터/초
            double speedInKph = (speedInMps * 3600) / 1000; // 시속(km/h) 단위로 변환

            String formattedSpeed = String.format("%.2f", speedInKph);

            Date first_date = new Date(first_timestamp);
            Calendar fcalendar = Calendar.getInstance();
            fcalendar.setTime(first_date);
            int fmonth = fcalendar.get(Calendar.MONTH); // 월 (0부터 시작하므로 1을 더해줘야 함)
            int fday = fcalendar.get(Calendar.DAY_OF_MONTH); // 일
            int fhour = fcalendar.get(Calendar.HOUR_OF_DAY); // 24시간 기준 시간
            int fminute = fcalendar.get(Calendar.MINUTE); // 분
            int fsecond = fcalendar.get(Calendar.SECOND); // 초
            fmonth+=1;


            Date last_date = new Date(last_timestamp);
            Calendar lcalendar = Calendar.getInstance();
            lcalendar.setTime(last_date);
            int lmonth = lcalendar.get(Calendar.MONTH); // 월 (0부터 시작하므로 1을 더해줘야 함)
            int lday = lcalendar.get(Calendar.DAY_OF_MONTH); // 일
            int lhour = lcalendar.get(Calendar.HOUR_OF_DAY); // 24시간 기준 시간
            int lminute = lcalendar.get(Calendar.MINUTE); // 분
            int lsecond = lcalendar.get(Calendar.SECOND); // 초
            lmonth+=1;

            int seconds = 0;
            int minutes = 0;
            int hours   = 0;
            String distanceTxt = "";
            if(distanceInMeters>1000)
                distanceTxt = String.format("%.2fKm", distanceInMeters/1000);
            else
                distanceTxt = String.format("%.2fM", distanceInMeters);

            long diffInMillis = last_timestamp - first_timestamp; // 밀리초 단위의 차이 계산
            seconds = (int) (diffInMillis / 1000) % 60;
            minutes = (int) ((diffInMillis / (1000*60)) % 60);
            hours   = (int) ((diffInMillis / (1000*60*60)) % 24);
            String mapLogText = "주행시작 : "+fmonth +"월 "+fday+"일 - "+fhour+"시 "+fminute+"분 " +fsecond+"초\n"+
                    "주행종료 : "+lmonth +"월 "+lday+"일 - "+lhour+"시 "+lminute+"분 " +lsecond+"초\n"+
                    "평균속력 : "+formattedSpeed+"km/h\n" +
                    "주행거리 : "+distanceTxt+"\n" +
                    "주행시간 : ";

            if(hours > 0)
                mapLogText+=hours+"시간 ";
            if(minutes > 0)
                mapLogText+=minutes+"분 ";
            mapLogText+=seconds+"초";

            TextView mTextView = findViewById(R.id.maplogInfoTextView);
            mTextView.setText(mapLogText);
            mTextView.setVisibility(View.VISIBLE);

            ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).showGpsHistory(arrList);
            if(mapFragment != null) fragmentManager.beginTransaction().show(mapFragment).commit();
            if(bookmarkFragment != null) fragmentManager.beginTransaction().hide(bookmarkFragment).commit();
            if(gpsLogFragment != null) fragmentManager.beginTransaction().hide(gpsLogFragment).commit();
            TabLayout tabs = findViewById(R.id.tabs);
            TabLayout.Tab mapTab = tabs.getTabAt(0); // "지도" 탭의 인덱스는 0
            mapTab.select();
        }
    }


    public void refreshSavedStation() {
        makeRequest();
    }
    public void setStationNotiToggle(int position, boolean togleVal) {
        stations.get(position).setNotiAllow(togleVal);
        SaveStationData(stations);
    }
    private void SaveStationData(ArrayList<Station> stations) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(stations);
        editor.putString("MyStations", json);
        editor.commit();
    }

    private void ReadStationData() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("MyStations", "");
        if(json == null || json == "") {
            Log.d("jsontest", "json 값 없음");
            stations = new ArrayList<>();
        }else{
            Log.d("jsontest", "json 값 :"+json);
            Type type = new TypeToken<ArrayList<Station>>() {}.getType();
            stations = gson.fromJson(json, type);
        }

    }

    private void ReadGpsLogData() {
        Log.d("jsontest", "값 없음");
        int cnt = 0;

        gpsLogs = new ArrayList<>();
        gpsLogs.clear();
        String selectQuery = "SELECT * FROM gps_list ORDER BY gpsListTime DESC";
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                String gpsListTime = cursor.getString(0);
                double distance = cursor.getDouble(1);
                Log.d("GPS Data", "GPS Time: "+gpsListTime+"distance: "+distance);
                gpsLogs.add(new GPS(gpsListTime, distance));
            } while (cursor.moveToNext());
        }

    }


    public boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d("서비스목록", service.service.getClassName());

            if ("com.meda.sejongbikehelper.PushService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isGpsServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d("서비스목록", service.service.getClassName());

            if ("com.meda.sejongbikehelper.GpsService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dbHelper = new MainActivity.MyDatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        ReadStationData();
        ReadGpsLogData();
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        for (Station i : stations) {
            if (i.getNotiAllow()) {
                i.setNotiAllow(false);
            }
        }
        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("112")){
                getNotificationPermission();
            }
        }
        spinDialog = new ProgressDialog(this);
        spinDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        spinDialog.setMessage("남은 자전거수를 갱신하는 중입니다.");
        spinDialog.setCancelable(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                for (Station i : stations) {
                    if (i.getNotiAllow()) {
                        i.setNotiAllow(false);
                    }
                }
                SaveStationData(stations);
                ((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")).listDataChange();
                Log.d("알림서비스","리시브받음");
                if(((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")) != null) {
                    ((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")).setStopService();
                }
            }
        };
        gpsBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("주행기록","리시브받음");
                if(((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")) != null) {
                   stopGpsService();
                   ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).setStopGpsService();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.meda.SejongbokeHelper.BTN_TEXT_CHANGE_ACTION");
        registerReceiver(mBroadcastReceiver, intentFilter, RECEIVER_NOT_EXPORTED);

        IntentFilter intentFilterGps = new IntentFilter();
        intentFilterGps.addAction("com.meda.SejongbokeHelper.GPSBTN_TEXT_CHANGE_ACTION");
        registerReceiver(gpsBroadcastReceiver, intentFilterGps, RECEIVER_NOT_EXPORTED);


        fragmentManager = getSupportFragmentManager();
        mapFragment = new MapFragment();
        fragmentManager.beginTransaction().replace(R.id.container, mapFragment,"mapFragment").commit();
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText("지도"));
        tabs.addTab(tabs.newTab().setText("북마크"));
        tabs.addTab(tabs.newTab().setText("주행기록"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d("MainActivity", "선택된 탭 : "+position );

                if(position == 0){
                    if(mapFragment == null) {
                        mapFragment = new MapFragment();
                        fragmentManager.beginTransaction().add(R.id.container, mapFragment, "mapFragment").commit();
                    }else if(mapFragment != null) fragmentManager.beginTransaction().show(mapFragment).commit();
                    if(bookmarkFragment != null) fragmentManager.beginTransaction().hide(bookmarkFragment).commit();
                    if(gpsLogFragment != null) fragmentManager.beginTransaction().hide(gpsLogFragment).commit();
                }else if(position == 1){
                    if(bookmarkFragment == null) {
                        bookmarkFragment = new BookmarkFragment();
                        fragmentManager.beginTransaction().add(R.id.container, bookmarkFragment, "bookmarkFragment").commit();
                    }else if(bookmarkFragment != null){
                        fragmentManager.beginTransaction().show(bookmarkFragment).commit();
                        ((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")).listDataChange();
                        ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).setNillArrow();
                        refreshSavedStation();
                    }
                    ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).setNillArrow();
                    TextView mTextView = findViewById(R.id.maplogInfoTextView);
                    mTextView.setVisibility(View.GONE);
                    if(mapFragment != null) fragmentManager.beginTransaction().hide(mapFragment).commit();
                    if(gpsLogFragment != null) fragmentManager.beginTransaction().hide(gpsLogFragment).commit();
                }else if(position == 2){
                    if(gpsLogFragment == null) {
                        gpsLogFragment = new GpsLogFragment();
                        fragmentManager.beginTransaction().add(R.id.container, gpsLogFragment, "gpsLogFragment").commit();
                    }else if(gpsLogFragment != null){
                        fragmentManager.beginTransaction().show(gpsLogFragment).commit();
                        ((GpsLogFragment) getSupportFragmentManager().findFragmentByTag("gpsLogFragment")).listDataChange();
                    }
                    ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).setNillArrow();
                    TextView mTextView = findViewById(R.id.maplogInfoTextView);
                    mTextView.setVisibility(View.GONE);
                    if(mapFragment != null) fragmentManager.beginTransaction().hide(mapFragment).commit();
                    if(bookmarkFragment != null) fragmentManager.beginTransaction().hide(bookmarkFragment).commit();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }
    public void makeRequest() {
        new_bike_list.clear();
        old_bike_list.clear();
        spinDialog.show();
        String url = "https://api.sejongbike.kr:5425/v1/station/list/extra";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                jsonObject = (JsonObject) JsonParser.parseString(response);
                jsonArray = jsonObject.get("data").getAsJsonObject().get("sbike_station").getAsJsonArray();
                for(JsonElement element : jsonArray) {
                    new_bike_list.add(element.getAsJsonObject());
                }
                jsonArray = jsonObject.get("data").getAsJsonObject().get("bike_station").getAsJsonArray();
                for(JsonElement element : jsonArray) {
                    old_bike_list.add(element.getAsJsonObject());
                }
                for (JsonObject j : new_bike_list) {
                    for (Station i : stations){
                        if(j.get("station_id").getAsString().equals(i.getId())){
                            i.setBikeNum(j.get("bike_parking").getAsString());
                            Log.d("serviceId",j.get("station_name").getAsString()+
                                    "정류장, 남은대수2:"+j.get("bike_parking").getAsString() );
                        }
                    }
                }
                ((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")).listDataChange();
                spinDialog.dismiss();
                SaveStationData(stations);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    //push서비스 실행
    public void startService()
    {
        serviceIntent = new Intent(this, PushService.class);
        serviceIntent.putExtra("stations",stations);
        startService(serviceIntent);
    }

    //push서비스 중지
    public void stopService()
    {
        serviceIntent = new Intent(this, PushService.class);
        stopService(serviceIntent);
    }

    //gps서비스 실행
    public void startGpsService()
    {
        serviceIntent = new Intent(this, GpsService.class);
        startService(serviceIntent);
    }

    //gps서비스 중지
    public void stopGpsService()
    {
        serviceIntent = new Intent(this, GpsService.class);
        stopService(serviceIntent);
        ReadGpsLogData();
    }
    public void getNotificationPermission(){
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS);
            }

        }catch (Exception e){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_POST_NOTIFICATIONS:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // allow

                }  else {
                    //deny
                }
                return;

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}