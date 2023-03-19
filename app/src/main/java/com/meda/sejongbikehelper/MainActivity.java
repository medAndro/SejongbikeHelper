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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Toolbar toolbar;
    Fragment mapFragment;
    private BroadcastReceiver mBroadcastReceiver;
    public Fragment bookmarkFragment;
    Intent serviceIntent;
    public static ArrayList<JsonObject> new_bike_list = new ArrayList();
    public static ArrayList<JsonObject> old_bike_list = new ArrayList();
    private RequestQueue queue;
    JsonObject jsonObject;
    JsonArray jsonArray;
    ProgressDialog spinDialog;
    private FragmentManager fragmentManager;
    ArrayList<Station> stations;
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
    public void startbtnClear(){
        Button startBtn = findViewById(R.id.startBtn);
        startBtn.setText("dd");
        Log.d("실행?", "ㅇㅇㅇㅇㅇㅇ 값 없음");
    }
    public void removeStation(int position) {
        stations.remove(position);
        SaveStationData(stations);
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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ReadStationData();
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
                if(bookmarkFragment != null) {
                    ((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")).setStopService();
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.meda.SejongbokeHelper.BTN_TEXT_CHANGE_ACTION");
        registerReceiver(mBroadcastReceiver, intentFilter);


        fragmentManager = getSupportFragmentManager();
        mapFragment = new MapFragment();
        fragmentManager.beginTransaction().replace(R.id.container, mapFragment).commit();
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText("지도"));
        tabs.addTab(tabs.newTab().setText("북마크"));
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
                }else if(position == 1){
                    if(bookmarkFragment == null) {
                        bookmarkFragment = new BookmarkFragment();
                        fragmentManager.beginTransaction().add(R.id.container, bookmarkFragment, "bookmarkFragment").commit();
                    }else if(bookmarkFragment != null){
                        fragmentManager.beginTransaction().show(bookmarkFragment).commit();
                        ((BookmarkFragment) getSupportFragmentManager().findFragmentByTag("bookmarkFragment")).listDataChange();
                        refreshSavedStation();
                    }
                    if(mapFragment != null) fragmentManager.beginTransaction().hide(mapFragment).commit();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}