package com.meda.sejongbikehelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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

public class BookmarkFragment extends Fragment {
    ListView customListView;
    ViewGroup rootView;
    public static Context context;
    private static StationAdapter stationAdapter;
    private Button startBtnBtn;
    private static final String KEY_BUTTON_TEXT = "key_button_text";
    private String mButtonText;
    public void setStopService(){
        mButtonText = "서비스 시작";
        startBtnBtn.setText(mButtonText);

        SharedPreferences prefs = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BUTTON_TEXT, mButtonText).apply();
    }
    public void setmButtonText(){
        if (((MainActivity)getActivity()).isServiceRunning()) {
            ((MainActivity)getActivity()).stopService();
            Toast.makeText(getContext(), "정류장 알림 서비스가 중지되었습니다.", Toast.LENGTH_SHORT).show();
            mButtonText = "서비스 시작";
        } else {
            ((MainActivity)getActivity()).startService();
            Toast.makeText(getContext(), "정류장 알림 서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show();
            mButtonText = "서비스 중지";
        }
        startBtnBtn.setText(mButtonText);

        SharedPreferences prefs = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BUTTON_TEXT, mButtonText).apply();

    }
    public void listDataChange(){
        stationAdapter.notifyDataSetChanged();
        //((MainActivity)getActivity()).refreshSavedStation();
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // mButtonText 값을 SharedPreferences에 저장
        SharedPreferences prefs = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_BUTTON_TEXT, mButtonText).apply();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout for this fragment
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_bookmark,
                container, false);
        startBtnBtn = rootView.findViewById(R.id.startBtn);

        // SharedPreferences에서 mButtonText 값을 불러옴
        SharedPreferences prefs = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        mButtonText = prefs.getString(KEY_BUTTON_TEXT, "서비스 시작"); //기본값이 서비스 시작
        if (!((MainActivity)getActivity()).isServiceRunning()) {
            mButtonText = "서비스 시작";
        }
        startBtnBtn.setText(mButtonText);


        customListView = (ListView) rootView.findViewById(R.id.bookmarkListView);
        stationAdapter = new StationAdapter(getContext(),((MainActivity)getActivity()).stations);
        customListView.setAdapter(stationAdapter);

        Button refreshBtnBtn = rootView.findViewById(R.id.RefreshSavedListBtn);
        refreshBtnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity)getActivity()).refreshSavedStation();
            }
        });

        startBtnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setmButtonText();
            }
        });


        return rootView;
    }

}