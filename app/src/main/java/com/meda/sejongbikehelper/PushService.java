package com.meda.sejongbikehelper;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PushService extends Service {
    BackgroundTask task;
    ArrayList<Station> stations;
    JsonObject jsonObject;
    JsonArray jsonArray;
    private MainActivity mainActivity;
    public RequestQueue queue;
    NotificationManager manager;
    NotificationCompat.Builder builder;
    boolean firstRun;

    private static String CHANNEL_ID = "channel1";
    private static String CHANEL_NAME = "Channel1";

    public static ArrayList<JsonObject> new_bike_list = new ArrayList();
    public static ArrayList<JsonObject> old_bike_list = new ArrayList();
    int value = 0;
    int notinum = 1;

    public void showNoti(String title, String contents){
        builder = null;
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //버전 오레오 이상일 경우
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, CHANEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            );

            builder = new NotificationCompat.Builder(this,CHANNEL_ID);

            //하위 버전일 경우
        }else{
            builder = new NotificationCompat.Builder(this);
        }

        //알림창 제목
        builder.setContentTitle(title);

        //알림창 메시지
        builder.setContentText(contents);

        //알림창 아이콘
        builder.setSmallIcon(R.mipmap.ic_launcher);

        Notification notification = builder.build();
        notinum++;
        //알림창 실행
        manager.notify(notinum,notification);
    }


    public PushService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
// TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if ("STOP_SERVICE".equals(action)) {
            stopSelf();
            task.cancel(true);
            stopForeground(true);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.meda.SejongbokeHelper.BTN_TEXT_CHANGE_ACTION");
            sendBroadcast(broadcastIntent);
            Toast.makeText(getApplicationContext(), "정류장 알림 서비스를 중지합니다.", Toast.LENGTH_SHORT).show();
            for (Station i : stations) {
                if (i.getNotiAllow()) {
                    i.setNotiAllow(false);
                }
            }


        }else{
            stations = (ArrayList<Station>) intent.getSerializableExtra("stations");
            initializeNotification(); //포그라운드 생성
        }

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        // 강제 종료됐을 때 실행할 코드
        // 예를 들어, 로그를 출력하거나 파일에 저장할 수 있습니다.
        stopSelf();

    }

    //포그라운드 서비스
    public void initializeNotification() {

        task = new BackgroundTask();
        task.execute();

        firstRun=true;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "1");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationCompat.BigTextStyle style = new NotificationCompat.BigTextStyle();
        style.bigText("어울링Helper로 돌아가려면 클릭하세요.");
        style.setBigContentTitle(null);
        style.setSummaryText("서비스 동작중");
        builder.setContentText(null);
        builder.setContentTitle(null);
        builder.setOngoing(true);
        builder.setStyle(style);
        builder.setWhen(0);
        builder.setShowWhen(false);
        builder.setSmallIcon(R.drawable.small_icon);

        // 알림바 버튼 추가
        Intent stopIntent = new Intent(this, PushService.class);
        stopIntent.setAction("STOP_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.app_widget_background, "알림서비스 중지", stopPendingIntent);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("startByWidget","true");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(new NotificationChannel("1", "포그라운드 서비스", NotificationManager.IMPORTANCE_NONE));
        }
        Notification notification = builder.build();
        startForeground(1, notification);
    }

    public void makeRequest() {
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
        }
        new_bike_list.clear();
        old_bike_list.clear();
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
                if(firstRun){
                    initBike();
                    firstRun = false;
                }
                else
                    chkBike();

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        stringRequest.setTag("myPush_tag"); // 태그 달기
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }
    public void initBike(){
        for (JsonObject j : new_bike_list) {
            for (Station i : stations){
                if(j.get("station_id").getAsString().equals(i.getId())){
                    i.setOriginBikeNum(j.get("bike_parking").getAsString());
                    Log.d("serviceId",j.get("station_name").getAsString()+
                            "정류장, 남은대수:"+j.get("bike_parking").getAsString() );
                }
            }
        }
    }
    public void chkBike(){
        for (JsonObject j : new_bike_list) {
            for (Station i : stations){
                if(j.get("station_id").getAsString().equals(i.getId())){
                    int origin = Integer.parseInt(i.getOriginBikeNum());
                    int refresh = Integer.parseInt(j.get("bike_parking").getAsString());
                    //println(j.get("station_name").getAsString()+"정류장:" +refresh + "대");
                    if(i.getNotiAllow()){
                        println(j.get("station_name").getAsString()+"정류장 체크중");
                    }
                    if(origin != refresh && i.getNotiAllow()){
                        if(origin==0 && refresh>0){
                            showNoti(j.get("station_name").getAsString()+" 정류장 이용가능!",
                                    "누군가 자전거를 반납하였습니다!");
                        }else if(origin<=5 && refresh==0){
                            showNoti(j.get("station_name").getAsString()+" 정류장 이용불가!",
                                    "모든 자전거가 소진되었습니다");
                        }else if(origin<=5 && refresh<origin){
                            showNoti(j.get("station_name").getAsString()+" 정류장 소진임박!",
                                    "잔여량이"+ refresh+"대로 감소하였습니다.");
                        }

                        //else if(origin != refresh){
                        //    showNoti(j.get("station_name").getAsString()+" 정류장 변경됨",
                        //            "잔여량이"+ refresh+"대로 변경되였습니다.");
                        //}
                        Log.d("serviceId",j.get("station_name").getAsString()+
                                "정류장, 오리진:"+origin+"리프레쉬"+refresh );
                        i.setOriginBikeNum(refresh+"");
                        Log.d("serviceId",j.get("station_name").getAsString()+
                                "정류장, 남은대수:"+j.get("bike_parking").getAsString() );
                    }
                }
            }
        }
    }

    class BackgroundTask extends AsyncTask<Integer, String, Integer> {

        String result = "";

        @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
        @Override
        protected Integer doInBackground(Integer... values) {
            while(!isCancelled()){
                try{
                    makeRequest();
                    println(value + "번째 체크중");
                    Thread.sleep(30000);
                    value++;
                }catch (InterruptedException ex){}
            }
            return value;
        }

        //상태확인
        @Override
        protected void onProgressUpdate(String... String) {
            println("onProgressUpdate()업데이트");
        }

        @Override
        protected void onPostExecute(Integer integer) {
            println("onPostExecute()");
            value = 0;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            // background task 취소 후 처리할 작업 수행
        }
    }

    //서비스종료
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(task != null) {
            task.cancel(true);
        }

        if (queue != null) {
            queue.cancelAll("myPush_tag"); // 태그를 이용하여 요청 취소
        }

        stopForeground(true);
    }

    public void println(String message){
        Log.d("MyService", message);
    }
}