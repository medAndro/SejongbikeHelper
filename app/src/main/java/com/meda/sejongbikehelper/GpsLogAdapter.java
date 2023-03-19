package com.meda.sejongbikehelper;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class GpsLogAdapter extends ArrayAdapter implements AdapterView.OnItemClickListener {
    private Context context;
    private List list;
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(context, "clicked", Toast.LENGTH_SHORT).show();
    }
    class ViewHolder {
        public TextView logtime;
        public TextView logDistance;
    }

    public GpsLogAdapter(Context context, ArrayList list){
        super(context, 0, list);
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null){
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            convertView = layoutInflater.inflate(R.layout.gpslog_item, parent, false);
        }

        viewHolder = new ViewHolder();
        viewHolder.logtime = (TextView) convertView.findViewById(R.id.logTime);
        viewHolder.logDistance = (TextView) convertView.findViewById(R.id.textView_distance);
        Button removeBtn = convertView.findViewById(R.id.gpslist_remove_btn);

        final GPS gpslogList = (GPS) list.get(position);
        removeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ((MainActivity)context).removeLog(position,gpslogList.getTime());
                notifyDataSetChanged();
            }
        });
        Button gpsMapView = convertView.findViewById(R.id.gpsMap_view_btn);
        gpsMapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)context).historyMapView(gpslogList.getTime());
            }
        });

        Log.d("변환하려던수",gpslogList.getTime());
        Log.d("거리",""+gpslogList.getDistance());
        Date date = new Date(Long.parseLong(gpslogList.getTime()));
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR); // 연도
        int month = calendar.get(Calendar.MONTH); // 월 (0부터 시작하므로 1을 더해줘야 함)
        int day = calendar.get(Calendar.DAY_OF_MONTH); // 일
        int hour = calendar.get(Calendar.HOUR_OF_DAY); // 24시간 기준 시간
        int minute = calendar.get(Calendar.MINUTE); // 분
        int second = calendar.get(Calendar.SECOND); // 초
        month+=1;
        viewHolder.logtime.setText(year+"년 "+ month +"월 "+day+"일 - "+hour+"시 "+minute+"분");
        double distanceValue = gpslogList.getDistance();
        if(distanceValue>=1000){
            double kilometer = distanceValue / 1000.0; // 미터를 킬로미터로 변환
            DecimalFormat df = new DecimalFormat("#.##");
            String formattedValue = df.format(kilometer);
            viewHolder.logDistance.setText(formattedValue+"\nKm");
        }else{
            DecimalFormat df = new DecimalFormat("#.#");
            String formattedValue = df.format(distanceValue);
            viewHolder.logDistance.setText(formattedValue+"\nM");
        }

        return convertView;
    }

}
