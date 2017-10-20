package com.vise.bledemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * @Description:
 * @author: <a href="http://xiaoyaoyou1212.360doc.com">DAWI</a>
 * @date: 2017/10/20 17:35
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mDevice_scan;
    private Button mDevice_connect;
    private Button mDevice_send_data;
    private Button mDevice_read_data;
    private Button mDevice_receive_data;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        bindViews();
        bindEvent();
    }

    private void bindViews() {
        mDevice_scan = (Button) findViewById(R.id.device_scan);
        mDevice_connect = (Button) findViewById(R.id.device_connect);
        mDevice_send_data = (Button) findViewById(R.id.device_send_data);
        mDevice_read_data = (Button) findViewById(R.id.device_read_data);
        mDevice_receive_data = (Button) findViewById(R.id.device_receive_data);
    }

    private void bindEvent() {
        mDevice_scan.setOnClickListener(this);
        mDevice_connect.setOnClickListener(this);
        mDevice_send_data.setOnClickListener(this);
        mDevice_read_data.setOnClickListener(this);
        mDevice_receive_data.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.device_scan:
                break;
            case R.id.device_connect:
                break;
            case R.id.device_send_data:
                break;
            case R.id.device_read_data:
                break;
            case R.id.device_receive_data:
                break;
        }
    }
}
