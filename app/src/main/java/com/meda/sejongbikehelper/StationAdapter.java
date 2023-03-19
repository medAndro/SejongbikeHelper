package com.meda.sejongbikehelper;

import android.content.Context;
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
                if(notiChkSwitch.isChecked()){
                    ((MainActivity)context).setStationNotiToggle(position,true);
                }else{
                    ((MainActivity)context).setStationNotiToggle(position,false);
                }
            }
        });
        TextView bikeNum = convertView.findViewById(R.id.textView_BikeNum);
        //LinearLayout numBorder = convertView.findViewById(R.id.numBorder);
        int bknum = Integer.parseInt(station.getBikeNum());

        bikeNum.setText(station.getBikeNum()+"대");
        ImageView imageView = convertView.findViewById(R.id.imageView);
        if ( Integer.parseInt(station.getBikeNum())<5){
            imageView.setImageResource(R.drawable.new_bike_red);
        }else if(Integer.parseInt(station.getBikeNum())<10){
            imageView.setImageResource(R.drawable.new_bike);
        }else{
            imageView.setImageResource(R.drawable.new_bike_green);
        }
        return convertView;
    }
}
