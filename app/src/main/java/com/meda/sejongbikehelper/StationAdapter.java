package com.meda.sejongbikehelper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends ArrayAdapter implements AdapterView.OnItemClickListener {
    private Context context;
    private List list;
    private Switch notiSwitch;
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(context, "clicked", Toast.LENGTH_SHORT).show();
    }
    class ViewHolder {
        public TextView name;
        public Switch notiSwitch;
    }

    public StationAdapter(Context context, ArrayList list){
        super(context, 0, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null){
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.station_item, parent, false);
        }

        viewHolder = new ViewHolder();
        viewHolder.name = (TextView) convertView.findViewById(R.id.textView_name);
        viewHolder.notiSwitch = (Switch) convertView.findViewById(R.id.notiChkSwitch);
        notiSwitch = (Switch) convertView.findViewById(R.id.notiChkSwitch);
        final Station station = (Station) list.get(position);
        viewHolder.name.setText(station.getName());
        viewHolder.name.setTag(station.getName());
        viewHolder.notiSwitch.setChecked(station.getNotiAllow());
        Button removeBtn = convertView.findViewById(R.id.list_remove_btn);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).removeStation(position);
                notifyDataSetChanged();
            }
        });
        Switch notiChkSwitch = convertView.findViewById(R.id.notiChkSwitch);
        notiChkSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("DEBug",notiChkSwitch.isChecked()+"");
                ArrayList<Station> stations = ((MainActivity)context).stations;

                int checkedNum = 0;
                for (Station i : stations) {
                    if (i.getNotiAllow()) {
                        checkedNum++;
                        Log.d("DEBug",i.name+"");
                    }
                }

                if(notiChkSwitch.isChecked()){
                    Log.d("토글체크","스위치 눌림");
                    ((MainActivity)context).setStationNotiToggle(position,true);
                    if(checkedNum==0){
                        //처음 체크되었을때
                        if (!((MainActivity)context).isServiceRunning()) {
                            ((MainActivity)context).startService();
                            Toast.makeText(getContext(), "정류장 알림 서비스를 시작합니다", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        //두번째 이상 체크되었을때
                        if (((MainActivity)context).isServiceRunning()) {
                            ((MainActivity)context).stopService();
                            ((MainActivity)context).startService();
                            //Toast.makeText(getContext(), "정류장 알림 서비스를 시작합니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                }else{
                    ((MainActivity)context).setStationNotiToggle(position,false);
                    if(checkedNum==1){
                        //마지막 체크되었을때
                        if (((MainActivity)context).isServiceRunning()) {
                            ((MainActivity)context).stopService();
                            Toast.makeText(getContext(), "정류장 알림 서비스를 중지합니다", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        //마지막 체크가 아닐때
                        if (((MainActivity)context).isServiceRunning()) {
                            ((MainActivity)context).stopService();
                            ((MainActivity)context).startService();
                            //Toast.makeText(getContext(), "정류장 알림 서비스를 중지합니다", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

            }
        });
        TextView bikeNum = convertView.findViewById(R.id.textView_BikeNum);
        //LinearLayout numBorder = convertView.findViewById(R.id.numBorder);
        int bknum = Integer.parseInt(station.getBikeNum());

        bikeNum.setText(station.getBikeNum()+"대");
        ImageView imageView = convertView.findViewById(R.id.imageView);
        if ( Integer.parseInt(station.getBikeNum())==0){
            imageView.setImageResource(R.drawable.new_bike100);
        }else if(Integer.parseInt(station.getBikeNum())==1){
            imageView.setImageResource(R.drawable.new_bike90);
        }else if(Integer.parseInt(station.getBikeNum())==2){
            imageView.setImageResource(R.drawable.new_bike80);
        }else if(Integer.parseInt(station.getBikeNum())==3){
            imageView.setImageResource(R.drawable.new_bike70);
        }else if(Integer.parseInt(station.getBikeNum())==4){
            imageView.setImageResource(R.drawable.new_bike60);
        }else if(Integer.parseInt(station.getBikeNum())==5){
            imageView.setImageResource(R.drawable.new_bike50);
        }else if(Integer.parseInt(station.getBikeNum())==6){
            imageView.setImageResource(R.drawable.new_bike40);
        }else if(Integer.parseInt(station.getBikeNum())==7){
            imageView.setImageResource(R.drawable.new_bike30);
        }else if(Integer.parseInt(station.getBikeNum())==8){
            imageView.setImageResource(R.drawable.new_bike20);
        }else if(Integer.parseInt(station.getBikeNum())==9){
            imageView.setImageResource(R.drawable.new_bike100);
        }else{
            imageView.setImageResource(R.drawable.new_bike);
        }
        return convertView;
    }
}
