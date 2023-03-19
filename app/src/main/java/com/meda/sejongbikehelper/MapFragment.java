package com.meda.sejongbikehelper;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

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
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.util.MarkerIcons;

import java.util.ArrayList;
import java.util.HashMap;
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
    ViewGroup rootView;
    Button addBookmarkBtn;
    Button refreshBtn;
    Marker clickedMarker = new Marker();
    ProgressDialog spinDialog;

    //위치 권한요구
    private FusedLocationSource locationSource;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };


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
            nMap.setLocationSource(locationSource);  //현재 위치
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
                nMap.setLocationTrackingMode(LocationTrackingMode.Follow);
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

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        nMap.setLocationSource(locationSource);  //현재 위치
        nMap.setLocationTrackingMode(LocationTrackingMode.Follow);
        naverMap.setCameraPosition(cameraPosition);
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