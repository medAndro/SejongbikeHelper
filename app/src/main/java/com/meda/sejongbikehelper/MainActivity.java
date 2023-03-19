package com.meda.sejongbikehelper;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
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
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.naver.maps.geometry.LatLng;

import java.lang.reflect.Type;

import java.util.ArrayList;
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


        spinDialog = new ProgressDialog(this);
        spinDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        spinDialog.setMessage("남은 자전거수를 갱신하는 중입니다.");
        spinDialog.setCancelable(false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
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
                    ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).setStopGpsService();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.meda.SejongbokeHelper.BTN_TEXT_CHANGE_ACTION");
        registerReceiver(mBroadcastReceiver, intentFilter);

        IntentFilter intentFilterGps = new IntentFilter();
        intentFilterGps.addAction("com.meda.SejongbokeHelper.GPSBTN_TEXT_CHANGE_ACTION");
        registerReceiver(gpsBroadcastReceiver, intentFilterGps);


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
                    if(mapFragment != null) fragmentManager.beginTransaction().hide(mapFragment).commit();
                    if(gpsLogFragment != null) fragmentManager.beginTransaction().hide(gpsLogFragment).commit();
                }else if(position == 2){

                    if(gpsLogFragment == null) {
                        gpsLogFragment = new GpsLogFragment();
                        fragmentManager.beginTransaction().add(R.id.container, gpsLogFragment, "gpsLogFragment").commit();
                    }else if(gpsLogFragment != null){
                        fragmentManager.beginTransaction().show(gpsLogFragment).commit();
                        ((GpsLogFragment) getSupportFragmentManager().findFragmentByTag("gpsLogFragment")).listDataChange();
                        ((MapFragment) getSupportFragmentManager().findFragmentByTag("mapFragment")).setNillArrow();
                    }
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}