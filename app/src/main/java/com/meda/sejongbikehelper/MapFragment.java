package com.meda.sejongbikehelper;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.ArrowheadPathOverlay;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;
import com.naver.maps.map.widget.CompassView;
import com.naver.maps.map.widget.LocationButtonView;
import com.naver.maps.map.widget.ScaleBarView;
import com.naver.maps.map.widget.ZoomControlView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback
{
    //지도 객체 변수
    private MapView mapView;
    public static NaverMap nMap;
    public static ArrayList<JsonObject> new_bike_list = new ArrayList();
    public static ArrayList<JsonObject> old_bike_list = new ArrayList();
    HashMap<Marker,JsonObject> markerHashMap = new HashMap<Marker,JsonObject>();//HashMap생성
    private ArrayList<Marker> markerList = new ArrayList();
    private ArrayList<InfoWindow> infoWindowsList = new ArrayList();
    JsonObject jsonObject;
    JsonArray jsonArray;
    private RequestQueue queue;
    private Context context;
    private String gpsButtonText;
    private static final String KEY_GPSBUTTON_TEXT = "key_gpsbutton_text";
    ViewGroup rootView;
    Button addBookmarkBtn;
    Button refreshBtn;
    Button gpsBtn;
    private static final int PERMISSION_REQUEST_LOCATION = 1;
    Marker clickedMarker = new Marker();
    ProgressDialog spinDialog;
    PathOverlay Path;
    int GPSbtnColor = Color.parseColor("#FF6200EE");
    //위치 권한요구
    private FusedLocationSource locationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };

    public void setStopGpsService(){
        gpsButtonText = "주행 기록하기";
        gpsBtn.setText(gpsButtonText);
        gpsBtn.setBackgroundColor(Color.parseColor("#FF6200EE"));
        SharedPreferences prefs = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GPSBUTTON_TEXT, gpsButtonText).apply();
    }
    public void println(String data) {
        Toast.makeText(getContext(),data, Toast.LENGTH_LONG).show();
    }

    public MapFragment() { }

    public static MapFragment newInstance()
    {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

    }
    public void setGpsButtonText(){
        if (((MainActivity)getActivity()).isGpsServiceRunning()) {
            ((MainActivity)getActivity()).stopGpsService();
            Toast.makeText(getContext(), "주행정보 기록을 중지합니다.", Toast.LENGTH_SHORT).show();
            gpsButtonText = "주행 기록하기";
            GPSbtnColor = Color.parseColor("#FF6200EE");

        } else {
            ((MainActivity)getActivity()).startGpsService();
            Toast.makeText(getContext(), "주행정보 기록을 시작합니다.", Toast.LENGTH_SHORT).show();
            gpsButtonText = "주행기록 중지";
            GPSbtnColor = Color.parseColor("#FF0000");
        }
        gpsBtn.setText(gpsButtonText);
        gpsBtn.setBackgroundColor(GPSbtnColor);
        SharedPreferences prefs = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_GPSBUTTON_TEXT, gpsButtonText).apply();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_map,
                container, false);
        spinDialog = new ProgressDialog(getActivity());
        spinDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        spinDialog.setMessage("데이터를 불러오는 중입니다.");
        spinDialog.setCancelable(false);
        spinDialog.show();
        mapView = (MapView) rootView.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        context = container.getContext();
        addBookmarkBtn = rootView.findViewById(R.id.button5);
        addBookmarkBtn.setVisibility(View.GONE);

        addBookmarkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBookmarkBtn.setVisibility(View.GONE);
                JsonObject markerJson = markerHashMap.get(clickedMarker);
                for (InfoWindow i : infoWindowsList) {
                    i.close();
                }
                if(((MainActivity)getActivity()).addStation(markerJson.get("station_id").getAsString(),
                        markerJson.get("station_name").getAsString(),markerJson.get("bike_parking").getAsString())){
                    Toast.makeText(context, markerJson.get("station_name").getAsString()+"이(가) 북마크에 추가됨", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(context, markerJson.get("station_name").getAsString()+"은(는) 이미 북마크에 존재합니다.", Toast.LENGTH_LONG).show();
                }
            }
        });

        refreshBtn = rootView.findViewById(R.id.button6);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinDialog.setMessage("정류장 데이터를 새로고치는 중입니다.");
                spinDialog.show();

                addBookmarkBtn.setVisibility(View.GONE);
                makeRequest();
            }
        });

        gpsBtn = rootView.findViewById(R.id.gpsLogBtn);
        gpsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
                int backgroundPermissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (permissionCheck == PackageManager.PERMISSION_DENIED || backgroundPermissionCheck == PackageManager.PERMISSION_DENIED) {
                        // 권한이 거부되어 있는 경우
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",  getContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        Toast.makeText(getContext(), "백그라운드에서 기록하기 위해\n위치권한을 항상허용으로 설정하세요", Toast.LENGTH_SHORT).show();
                    } else {
                        // 권한이 허용되어 있는 경우
                        setGpsButtonText();

                    }
                }else{
                    if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                        // 권한이 거부되어 있는 경우
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package",  getContext().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                        Toast.makeText(getContext(), "백그라운드에서 기록하기 위해\n위치권한을 허용으로 설정하세요", Toast.LENGTH_SHORT).show();
                    } else {
                        // 권한이 허용되어 있는 경우
                        setGpsButtonText();
                    }
                }
            }
        });

        if (queue == null) {
            queue = Volley.newRequestQueue(context);
            makeRequest();
        }
        return rootView;
    }

    public void makeRequest() {
        for (Marker i : markerList) {
            i.setMap(null);
        }
        markerList.clear();
        new_bike_list.clear();
        old_bike_list.clear();
        Log.d("ArrayListSize", markerList.size()+"삭제후 개수");
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

                drawMarker();
                Log.d("ArrayListSize", markerList.size()+"개");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    private void drawMarker() {
        for (int i =0 ; i< new_bike_list.size(); i++){
            JsonObject thisMarker = new_bike_list.get(i);
            double y = Double.parseDouble(thisMarker.get("y_pos").getAsString());
            double x = Double.parseDouble(thisMarker.get("x_pos").getAsString());

            Marker marker = new Marker();
            markerList.add(marker);
            marker.setPosition(new LatLng(y, x));
            marker.setIcon(OverlayImage.fromResource(R.drawable.map_marker));
            marker.setIconTintColor(Color.rgb(71,129,227));
            marker.setCaptionText(thisMarker.get("bike_parking").getAsString());
            marker.setCaptionAligns(Align.Top);
            marker.setCaptionOffset(-57);
            marker.setWidth(100);
            marker.setHeight(100);
            marker.setCaptionColor(Color.WHITE);
            marker.setCaptionHaloColor(Color.BLACK);
            marker.setCaptionTextSize(14);
            InfoWindow infoWindow = new InfoWindow();
            infoWindowsList.add(infoWindow);
            markerHashMap.put(marker,thisMarker);
            infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(context) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    return thisMarker.get("station_name").getAsString()+"\n뉴어울링"+thisMarker.get("station_id").getAsString();
                }
            });
            marker.setOnClickListener(new Overlay.OnClickListener() {
                @Override
                public boolean onClick(@NonNull Overlay overlay)
                {
                    for (InfoWindow i : infoWindowsList) {
                        i.close();
                    }

                    if (overlay instanceof Marker) {
                        Marker marker = (Marker) overlay;
                        if (marker.getInfoWindow() != null) {
                            infoWindow.close();
                        }
                        else {
                            infoWindow.open(marker);
                            addBookmarkBtn.setVisibility(View.VISIBLE);
                            clickedMarker = marker;

                        }
                        return true;
                    }
                    return false;
                }
            });
            marker.setMap(nMap);


        }
        spinDialog.dismiss();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!locationSource.isActivated()) { // 권한 거부됨
                nMap.setLocationTrackingMode(LocationTrackingMode.Face);

            }
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap)
    {
        nMap = naverMap;
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setLayerGroupEnabled(naverMap.LAYER_GROUP_BUILDING, true);

        CameraPosition cameraPosition = new CameraPosition(
                new LatLng(36.6093252, 127.2898829),   // 위치 지정
                13
        );
        nMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                addBookmarkBtn.setVisibility(View.GONE);
                for (InfoWindow i : infoWindowsList) {
                    i.close();
                }
            }
        });
        UiSettings uiSettings = nMap.getUiSettings();
        uiSettings.setCompassEnabled(false); // 기본값 : true
        uiSettings.setZoomControlEnabled(false); // 기본값 : true
        uiSettings.setLocationButtonEnabled(false); // 기본값 : false

        CompassView compassView = rootView.findViewById(R.id.compass);
        compassView.setMap(nMap);
        ZoomControlView zoomControlView = rootView.findViewById(R.id.zoom);
        zoomControlView.setMap(nMap);
        LocationButtonView locationButtonView = rootView.findViewById(R.id.location);
        locationButtonView.setMap(nMap);



        nMap.setLocationSource(locationSource);  //현재 위치

        nMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.setCameraPosition(cameraPosition);
    }

    public void showGpsHistory(List<LatLng> gpslist){
        Path = new PathOverlay();

        Path.setCoords(gpslist);

        Path.setWidth(20);
        Path.setColor(Color.BLUE);
        nMap.moveCamera(CameraUpdate.fitBounds(LatLngBounds.from(gpslist)));

        if(nMap.getCameraPosition().zoom>1){
            nMap.moveCamera(CameraUpdate.zoomTo(nMap.getCameraPosition().zoom-0.5));
        }
        Path.setPatternImage(OverlayImage.fromResource(R.drawable.map_guide));
        Path.setPatternInterval(40);
        Path.setMap(nMap);
    }

    public void setNillArrow(){
        if(Path!=null){
            Path.setMap(null);
        }

    }

    @Override
    public void onStart()
    {
        super.onStart();
        mapView.onStart();

    }

    @Override
    public void onResume()
    {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}