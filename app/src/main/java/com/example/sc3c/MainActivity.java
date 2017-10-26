package com.example.sc3c;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    private final String TAG="SC3C-APP";

    private Button button_center,
            button_left,
            button_top,
            button_right,
            button_bottom;


    private BlueTooth mBlueTooth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView(){
        button_center=(Button)findViewById(R.id.center);
        button_left=(Button)findViewById(R.id.left);
        button_top=(Button)findViewById(R.id.top);
        button_right=(Button)findViewById(R.id.right);
        button_bottom=(Button)findViewById(R.id.bottom);
        button_center.setWidth(50);
        button_center.setHeight(50);
        button_center.setBackgroundColor(Color.WHITE);

        mBlueTooth=new BlueTooth(MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultMode,Intent intent){
        super.onActivityResult(requestCode,resultMode,intent);
        //判断结果码是否与回传的结果码相同
        if(1 == requestCode){
            //蓝牙已经开启
            Log.d(TAG,"蓝牙已开启");
        }
    }


}
