package touma.pkumap.view;

import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.util.AttributeSet;
import android.widget.TextView;

import touma.pkumap.IntroActivity;
import touma.pkumap.MainActivity;
import touma.pkumap.PKUMapActivity;
import touma.pkumap.R;
import touma.pkumap.util.MyPoiInfo;

/**
 * Created by apple on 2017/11/3.
 */

public class PoiInfoView extends LinearLayout {

    private Button btn_src;
    private Button btn_dest;
    private Button btn_intro;
    private PKUMapActivity activity;

    public PoiInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        activity = (PKUMapActivity)context;

        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.view_poi_info, this);

        // 获取控件
        btn_src = (Button) findViewById(R.id.btn_src);
        btn_dest = (Button) findViewById(R.id.btn_dest);
        btn_intro = (Button) findViewById(R.id.btn_intro);
        btn_src.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.getSrcPoi() != activity.getSelectedPoi()) {
                    activity.setSrcPoi(activity.getSelectedPoi());
                    btn_src.setText("取消起点");
                }
                else {
                    activity.setSrcPoi(null);
                    btn_src.setText("起点");
                }
            }
        });
        btn_dest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.getDestPoi() != activity.getSelectedPoi()) {
                    activity.setDestPoi(activity.getSelectedPoi());
                    btn_dest.setText("取消终点");
                }
                else {
                    activity.setDestPoi(null);
                    btn_dest.setText("终点");
                }
            }
        });
        btn_intro.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, IntroActivity.class);
                intent.putExtra("Poi", activity.getSelectedPoi());
                activity.startActivityForResult(intent, MainActivity.CodeEnum.BTN_INTRO.ordinal());
            }
        });
    }
}