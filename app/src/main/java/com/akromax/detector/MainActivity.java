package com.akromax.detector;

import androidx.appcompat.app.AppCompatActivity;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.akromax.detector.Controller.MyService;

public class MainActivity extends AppCompatActivity {
    public TextView status;
    public TextView dnsresponsetime;
    public TextView radioserverresponsetime;
    public TextView networklatency;
    public Button startButton;
    public Button stopButton;
    public MyService myService;
    private Handler handler;
    private Intent i;
    
    private boolean isBound=false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status=findViewById(R.id.statusText);
        startButton=findViewById(R.id.startButton);
        stopButton=findViewById(R.id.stop_button);
        dnsresponsetime=findViewById(R.id.dnsresponsetime);
        radioserverresponsetime=findViewById(R.id.radioserverresponsetime);
        networklatency=findViewById(R.id.networklatency);
        handler=new Handler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myService!=null){
            
            myService.onDestroy();
        }
    }

    public void startService(View view){
        i=new Intent(this,MyService.class);
        bindService(i,connection,BIND_AUTO_CREATE);
    }
    public void onStopButtonClicked(View view){
        if(myService!=null){
            stopService(i);
            System.exit(0);
//            myService.onDestroy();
//            dnsresponsetime.setText("");
//            radioserverresponsetime.setText("");
//            networklatency.setText("");
//            status.setText(MainActivity.this.getResources().getString(R.string.service_status_stop));
        }
        
    }
    
    public void intActivity(){
        myService.setActivity(this,handler);
        myService.startServerCheck();
    }
    
    public ServiceConnection connection=new ServiceConnection() {
        
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyService.MyBinder binder=(MyService.MyBinder) iBinder;
            myService=binder.getService();
            isBound=true;
            status.setText(MainActivity.this.getResources().getString(R.string.service_status_running));
            intActivity();
        }
        
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound=false;
            status.setText(MainActivity.this.getResources().getString(R.string.service_status_stop));

        }
    };
}