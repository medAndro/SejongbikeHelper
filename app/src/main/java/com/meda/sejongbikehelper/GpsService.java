package com.meda.sejongbikehelper;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.location.LocationListener;
import com.android.volley.RequestQueue;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Date;

public class GpsService extends Service implements LocationListener {

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private LocationManager locationManager;
    private Location lastLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    ArrayList<Station> stations;
    BackgroundTask task;
    private MainActivity mainActivity;
    NotificationManager manager;
    NotificationCompat.Builder builder;
    boolean firstRun;
    private MainActivity.MyDatabaseHelper dbHelper;
    private SQLiteDatabase db;

    private static String CHANNEL_ID = "channel2";
    private static String CHANEL_NAME = "Channel2";


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

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new MainActivity.MyDatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 위치 업데이트 요청 생성
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // 위치 업데이트 간격 (5초)
        locationRequest.setFastestInterval(2000); // 가장 빠른 업데이트 간격 (2초)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // 정확도 우선
        Log.d("위치요청","위치요청");
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // 위치 업데이트를 수신하면 실행될 코드 작성
                Log.d("위치요청수신함","위치요청수신함");
                lastLocation = locationResult.getLastLocation();
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if ("STOP_GPS_SERVICE".equals(action)) {
            stopSelf();
            //task.cancel(true);
            stopForeground(true);
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.meda.SejongbokeHelper.GPSBTN_TEXT_CHANGE_ACTION");
            sendBroadcast(broadcastIntent);
            Toast.makeText(getApplicationContext(), "주행 정보 기록을 중지하였습니다.", Toast.LENGTH_SHORT).show();

        }else{
            startLocationUpdates();
            stations = (ArrayList<Station>) intent.getSerializableExtra("stations");
            initializeNotification(); //포그라운드 생성
        }

        return START_STICKY;
    }

    public GpsService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
        style.setSummaryText("주행기록 로깅중");
        builder.setContentText(null);
        builder.setContentTitle(null);
        builder.setOngoing(true);
        builder.setStyle(style);
        builder.setWhen(0);
        builder.setShowWhen(false);
        builder.setSmallIcon(R.drawable.small_icon);

        // 알림바 버튼 추가
        Intent stopIntent = new Intent(this, GpsService.class);
        stopIntent.setAction("STOP_GPS_SERVICE");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
        builder.addAction(R.drawable.app_widget_background, "주행기록 중지", stopPendingIntent);

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
    class BackgroundTask extends AsyncTask<Integer, String, Integer> {
        int value = 0;
        String result = "";
        double templat = 0.0;
        double templon = 0.0;
        String initTime = String.valueOf(System.currentTimeMillis());
        @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
        @Override
        protected Integer doInBackground(Integer... values) {
            ContentValues GPSListvalues = new ContentValues();
            Log.d("음수인가?",""+initTime);
            GPSListvalues.put("gpsListTime", ""+initTime); // 초기 시간
            db.insert("gps_list", null, GPSListvalues);
            while(!isCancelled()){
                try{
                    startLocationUpdates();
                    Thread.sleep(3000);
                    //println("스레드작동");
                    Location location = lastLocation;
                    if (location!=null && location.getLatitude() != templat && location.getLatitude() != templon  ){
                        templat = location.getLatitude();
                        templon = location.getLatitude();
                        ContentValues GPSvalues = new ContentValues();
                        GPSvalues.put("gpsListID", ""+initTime);
                        GPSvalues.put("latitude", location.getLatitude());
                        GPSvalues.put("longitude", location.getLongitude());
                        GPSvalues.put("timestamp", ""+String.valueOf(System.currentTimeMillis())); // 현재 시간
                        db.insert("gps_data", null, GPSvalues);
                        println(value + "번째 체크중 - 현재 위치: " + location.getLatitude() + ", " + location.getLongitude());
                        String selectQuery = "SELECT * FROM gps_data WHERE gpsListID = "+initTime;
                        Cursor cursor = db.rawQuery(selectQuery, null);
                        if (cursor.moveToFirst()) {
                            do {
                                int gpsDataID = cursor.getInt(0);
                                String gpsListID = cursor.getString(1);
                                double latitude = cursor.getDouble(2);
                                double longitude = cursor.getDouble(3);
                                String timestamp = cursor.getString(4);
                                //cursor.getColumnIndex("timestamp")

                                // 조회한 데이터를 사용할 수 있습니다.
                                // 예를 들어, 로그로 출력해 볼 수 있습니다.

                                Log.d("GPS Data", "GPS Data ID: " + gpsDataID + ", GPS List ID: " + gpsListID + ", Latitude: " + latitude + ", Longitude: " + longitude + ", Timestamp: " + timestamp);
                            } while (cursor.moveToNext());
                        }

                        value++;
                    }

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
    public void println(String message){
        Log.d("GpsService", message);
    }

    //서비스종료
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(task != null) {
            task.cancel(true);
            db.close();
        }
        stopLocationUpdates();
        stopForeground(true);
    }
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;
        Log.d("위치변경됨","ㅇㅇ");
        // 위치 업데이트를 수신하면 실행될 코드 작성
    }

    @Override
    public void onProviderEnabled(String provider) {
        // GPS 활성화 시 실행될 코드 작성
    }

    @Override
    public void onProviderDisabled(String provider) {
        // GPS 비활성화 시 실행될 코드 작성
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // GPS 상태 변경 시 실행될 코드 작성
    }


}