package com.meda.sejongbikehelper;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Implementation of App Widget functionality.
 */
public class BikeWidget extends AppWidgetProvider {
    public static final String SHARED_PREFS = "prefs";
    public static final String KEY_BUTTON_TEXT = "keyButtonText";
    private final String ACTION_BTN = "ButtonClick";
    private int widgetID = 0;
    public static ArrayList<JsonObject> new_bike_list = new ArrayList();
    public static ArrayList<JsonObject> old_bike_list = new ArrayList();
    JsonObject jsonObject;
    JsonArray jsonArray;
    static CharSequence widgetText;
    static CharSequence widgetNum;
    private RequestQueue queue;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d("widgettest","on업데이트");

        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bike_widget);
            Intent intent = new Intent(context, BikeWidget.class).setAction(ACTION_BTN);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.bike_widget_text, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        makeRequest(context, appWidgetIds, false );
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("위젯테스트", "갱신함"+intent.getAction());
        super.onReceive(context, intent);
        if(intent.getAction().equals(ACTION_BTN)){
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, BikeWidget.class));
            makeRequest(context, appWidgetIds, true);
        }
    }
    Date mDate;
    SimpleDateFormat mFormat = new SimpleDateFormat("HH:mm");
    private String getTime(){
        long mNow;
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }
    public void makeRequest(Context context, int[] appWidgetIds, boolean ifTouch) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
        }
        Log.d("위젯업데이트", "위젯 업데이트 시작");
        new_bike_list.clear();
        old_bike_list.clear();
        SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if(ifTouch){
            for (int appWidgetId: appWidgetIds){
                RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bike_widget);
                remoteViews.setTextViewText(R.id.bike_widget_text, "로딩중...");
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            }
        }

        String url = "https://api.sejongbike.kr:5425/v1/station/list/extra";
        Log.d("위젯업데이트", "api 접속시작");
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("위젯업데이트", "api 응답받음");
                jsonObject = (JsonObject) JsonParser.parseString(response);
                jsonArray = jsonObject.get("data").getAsJsonObject().get("sbike_station").getAsJsonArray();
                for(JsonElement element : jsonArray) {
                    new_bike_list.add(element.getAsJsonObject());
                    Log.d("new_bike_list_add", element.getAsJsonObject()+"");
                }
                jsonArray = jsonObject.get("data").getAsJsonObject().get("bike_station").getAsJsonArray();
                for(JsonElement element : jsonArray) {
                    old_bike_list.add(element.getAsJsonObject());
                }
                widgetText = "로딩실패";
                widgetNum = "";
                //Toast.makeText(context, "새로고침 되었습니다!", Toast.LENGTH_SHORT).show();


                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                for (int appWidgetId: appWidgetIds){

                    String bikeId = prefs.getString(KEY_BUTTON_TEXT + appWidgetId, "Press me");
                    for (JsonObject j : new_bike_list) {
                        if(j.get("station_id").getAsString().equals(bikeId)){ // "SJ_00444")
                            widgetText = j.get("station_name").getAsString();
                            widgetNum = "[ "+j.get("bike_parking").getAsString()+"대 ]";
                            break;
                        }
                    }
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bike_widget);
                    remoteViews.setTextViewText(R.id.bike_widget_text, widgetText);
                    //Toast.makeText(context, "\uD83D\uDD52"+getTime(), Toast.LENGTH_SHORT).show();
                    remoteViews.setTextViewText(R.id.bike_widget_time, "\uD83D\uDD52"+getTime());
                    remoteViews.setTextViewText(R.id.bike_widget_num, widgetNum);


                    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("위젯업데이트", "api 응답실패");
            }
        });
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }
}