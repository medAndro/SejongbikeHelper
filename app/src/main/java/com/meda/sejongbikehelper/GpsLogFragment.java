package com.meda.sejongbikehelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class GpsLogFragment extends Fragment {
    ListView customListView;
    ViewGroup rootView;
    public static Context context;
    private static GpsLogAdapter gpslogAdapter;
    private Button startBtnBtn;
    private static final String KEY_BUTTON_TEXT = "key_button_text";
    private String mButtonText;

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
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_gpslog,
                container, false);
        startBtnBtn = rootView.findViewById(R.id.startBtn);


        customListView = (ListView) rootView.findViewById(R.id.gpslogListView);
        gpslogAdapter = new GpsLogAdapter(getContext(),((MainActivity)getActivity()).gpsLogs);
        customListView.setAdapter(gpslogAdapter);

        return rootView;
    }
    public void listDataChange(){
        Log.d("주행기록 새로고침","ㅇㅇ");
        gpslogAdapter = new GpsLogAdapter(getContext(),((MainActivity)getActivity()).gpsLogs);
        customListView.setAdapter(gpslogAdapter);
        gpslogAdapter.notifyDataSetChanged();
        //((MainActivity)getActivity()).refreshSavedStation();
    }

}