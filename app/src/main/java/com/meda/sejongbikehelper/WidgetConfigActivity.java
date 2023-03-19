package com.meda.sejongbikehelper;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

public class WidgetConfigActivity extends AppCompatActivity {
    public static final String SHARED_PREFS = "prefs";
    public static final String KEY_BUTTON_TEXT = "keyButtonText";
    private final String ACTION_BTN = "ButtonClick";
    public static ArrayList<JsonObject> new_bike_list = new ArrayList();
    public static ArrayList<JsonObject> old_bike_list = new ArrayList();
    JsonObject jsonObject;
    JsonArray jsonArray;
    ProgressDialog spinDialog;
    static CharSequence widgetText;
    static CharSequence widgetNum;
    private RequestQueue queue;
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    Spinner spinner;
    ArrayList<Station> stations;
    ArrayList<String> stationIdList;

    private void ReadStationData() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Gson gson = new Gson();
        String json = sharedPrefs.getString("MyStations", "");
        if(json == null || json == "") {
            stations = new ArrayList<>();
        }else{
            Type type = new TypeToken<ArrayList<Station>>() {}.getType();
            stations = gson.fromJson(json, type);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widget_config);
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }
        ReadStationData();
        stationIdList = new ArrayList<String>();
        stationIdList.clear();

        Log.d("이벤트클릭", stations+"");
        spinner = findViewById(R.id.spinner);
        for (Station i : stations){
            Log.d("이벤트클릭", "i.getId()");
            insert_spinner(i.getName());
        }
        Intent configIntent = getIntent();
        Bundle extras = configIntent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_CANCELED, resultValue);

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }
    }

    public void insert_spinner(String stationID){
        stationIdList.add(stationID);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_item, stationIdList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    Date mDate;
    SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm");
    private String getTime(){
        long mNow;
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }
    public void makeRequest(View v) {
        spinDialog = new ProgressDialog(this);
        spinDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        spinDialog.setMessage("정류장 정보를 갱신하는 중입니다.");
        spinDialog.setCancelable(false);
        spinDialog.show();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);

        Intent intent = new Intent(this, BikeWidget.class).setAction(ACTION_BTN);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,0, intent, PendingIntent.FLAG_IMMUTABLE);

        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_BUTTON_TEXT + appWidgetId, stations.get(spinner.getSelectedItemPosition()).getId());
        editor.apply();

        RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.bike_widget);
        views.setOnClickPendingIntent(R.id.bike_widget_text, pendingIntent);

        new_bike_list.clear();
        old_bike_list.clear();

        Log.d("jsontest", "1번");
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
                Log.d("jsontest", jsonArray+"");
                //Toast.makeText(context, "업데이트2", Toast.LENGTH_SHORT).show();
                widgetText = "로딩실패";
                widgetNum = "";

                for (JsonObject j : new_bike_list) {
                    if(j.get("station_id").getAsString().equals(stations.get(spinner.getSelectedItemPosition()).getId())){ // "SJ_00444")
                        widgetText = j.get("station_name").getAsString();
                        widgetNum = "[ "+j.get("bike_parking").getAsString()+"대 ]";
                        break;
                    }
                }

                views.setCharSequence(R.id.bike_widget_text, "setText", widgetText);
                views.setCharSequence(R.id.bike_widget_num, "setText", widgetNum);
                views.setTextViewText(R.id.bike_widget_time, "\uD83D\uDD52"+getTime());
                appWidgetManager.updateAppWidget(appWidgetId, views);

                spinDialog.dismiss();
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }
}