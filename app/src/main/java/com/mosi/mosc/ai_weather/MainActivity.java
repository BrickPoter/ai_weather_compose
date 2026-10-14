package com.mosi.mosc.ai_weather;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
//import android.app.Activity; // 旧的 activity
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.util.Random;
import android.util.Log;
import android.content.Intent;
import android.widget.TextView;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Process;
import android.os.RemoteException;
import android.os.Parcel;

// broadcast
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class MainActivity extends AppCompatActivity {
    // 声明界面组件
    private ImageView touziImageView;
    private Button rollButton;
    private String TAG = "WeatherAIDLServices-Activity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    // music service
    private boolean isForegroundMusic = false;
    private boolean isMusicPlaying = false;
    private Button startMusicButton;
    private Button startForegroundMusicButton;
    private Button stopMusicButton;
    private Button btnRegSpeech;
    private Button btnUnregSpeech;
    private TextView tvLeaveTime;
    private TextView tvStatus;
    private TextView tvBroadCastStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 关联布局文件,给资源分配heap空间

        Log.d(TAG, "Activity Run onCreate");

        Intent intent = new Intent(this, WeatherAIDLServices.class);
        intent.setAction("START");
        startService(intent);


        // music button
        startMusicButton = findViewById(R.id.btn_start_music);
        startForegroundMusicButton = findViewById(R.id.btn_start_foreground_music);
        stopMusicButton = findViewById(R.id.btn_stop_music);
        startMusicButton.setOnClickListener(
                this::startMusic
        );
        startForegroundMusicButton.setOnClickListener(
                this::startForegroundMusic
        );
        stopMusicButton.setOnClickListener(
                this::stopMusic
        );


        tvLeaveTime = findViewById(R.id.tv_leave_time);
        tvStatus = findViewById(R.id.tv_status);
        tvBroadCastStatus = findViewById(R.id.tv_broadcast_result);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Activity Run onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity Run onPause");
        // 用户离开，开始计时
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Activity Run onResume");
        // 用户回来，显示离开时间
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity Run onDestroy");
    }

    private void startMusic(View view){
        Intent intent = new Intent(this, WeatherAIDLServices.class);
        intent.setAction("com.mosi.mosc.ai_weather.REGISTER");

        isMusicPlaying = true;
        isForegroundMusic = false;

        startService(intent);
    }

    private void startForegroundMusic(View view){
        Intent intent = new Intent(this, WeatherAIDLServices.class);
        intent.setAction("com.mosi.mosc.ai_weather.UNREGISTER");

        isMusicPlaying = true;
        isForegroundMusic = true;

        startService(intent);
    }

    // Method to stop the service
    public void stopMusic(View view) {

        isMusicPlaying = false;

        stopService( new Intent(this, WeatherAIDLServices.class) );
    }

}