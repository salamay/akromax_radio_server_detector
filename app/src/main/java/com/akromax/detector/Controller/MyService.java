package com.akromax.detector.Controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.nfc.Tag;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.akromax.detector.Interface.ServerChecker;
import com.akromax.detector.MainActivity;
import com.akromax.detector.R;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyService extends Service implements ServerChecker {
    
    public final IBinder myservicebinder=new MyBinder();
    private final String TAG="MYSERVICE";
    private final String SOUND_NOTI_CHANNEL_ID="Server check with sound";
    private final String NOTI_CHANNEL_ID="Server check";
    private MainActivity mainActivity;
    public Handler handler;
    private MyThread myThread;
    private boolean interupt=false;
    private float radioresponseTime;
    private float latency;
    private float dnsresponseTime;
    private final int notificationId=13;

    public MyService() {
        
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return myservicebinder;
    }
    
    public void startServerCheck() {
        Log.i(TAG,"Service started");
        checkServer(url);
    }
    
    public void setActivity(MainActivity mainActivity, Handler handler){
        this.mainActivity=mainActivity;
        this.handler=handler;
    }
    

    @Override
    public void checkServer(String url) {
        myThread=new MyThread(mainActivity.getApplicationContext());
        myThread.start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Service Stopped");
        if(myThread!=null){
            myThread.interrupt();
            interupt=true;
        }
        
    }

    public class MyBinder extends Binder{
        public MyService getService(){
            return MyService.this;
        }
    }
    
    public class MyThread extends  Thread{
        
        private OkHttpClient client;
        private Request request;
        private OkHttpClient dnsclient;
        private Request dnsrequest;
        private Context context;
        
        public MyThread(Context context){
            this.context=context;
        }
        @Override
        public void run() {
            super.run();
            while(!interupt){
                createNotificationChannelWithSound();
                createNotificationChannelWithoutSound();
                client = new OkHttpClient();
                request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                dnsclient=new OkHttpClient();
                dnsrequest = new Request.Builder()
                        .url(dnsurl)
                        .get()
                        .build();

                startServerCheck();
            }
            Thread.currentThread().stop(); 
        }
        private void startServerCheck(){
            dnsresponseTime=0;
            radioresponseTime=0;
            latency=0;
            startDNSText();
            Log.i(TAG,"STARTING RADIO SERVER TEST");

            try{
                Response response=client.newCall(request).execute();
                Log.i(TAG,response.toString());
                if(response.code()==200){
                    radioresponseTime= response.receivedResponseAtMillis()-response.sentRequestAtMillis();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mainActivity.radioserverresponsetime.setText("Radio Server response time: "+String.valueOf(radioresponseTime/1000)+" seconds");
                        }
                    });
                    createNotificationWithoutSound(getResources().getString(R.string.Radio_server_status),getResources().getString(R.string.Radio_server_ok));
                    response.close();
                }else{
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            radioresponseTime=response.receivedResponseAtMillis()-response.sentRequestAtMillis();
                            createNotificationWithSound(getResources().getString(R.string.Radio_server_status),getResources().getString(R.string.Radio_server_down));
                            Toast.makeText(mainActivity.getApplicationContext(), R.string.Radio_server_down,Toast.LENGTH_SHORT).show();
                            mainActivity.radioserverresponsetime.setText("Radio Server response time: "+String.valueOf(radioresponseTime/1000)+" seconds");
                        }
                    });
                    response.close();
                }
            }catch (Exception e){
                System.out.println(e);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mainActivity.getApplicationContext(), R.string.unable_to_connect,Toast.LENGTH_SHORT).show();
                        mainActivity.radioserverresponsetime.setText(getResources().getString(R.string.unable_to_connect));
                    }
                });
                createNotificationWithoutSound(getResources().getString(R.string.Radio_server_status),getResources().getString(R.string.unable_to_connect));
            }
            if(dnsresponseTime!=0&&radioresponseTime!=0){
                
                handler.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        latency=(dnsresponseTime-radioresponseTime)/1000;
                        mainActivity.networklatency.setText("Network latency (DNS-RADIO): "+String.valueOf(latency)+" seconds");
                        if(latency<-10){
                            createNotificationWithSound(getResources().getString(R.string.Radio_server_status),getResources().getString(R.string.Radio_server_problem));
                        }
                    }
                });
            }
            Log.i(TAG,"RADIO SERVER TEST DONE");
            Log.i(TAG,String.valueOf(dnsresponseTime));
            Log.i(TAG,String.valueOf(radioresponseTime));
            try {
                Thread.sleep(1000*60*15);
            } catch (InterruptedException e) {
                e.printStackTrace();
                myThread.start();
            }
            startServerCheck();
        }
        
        public void startDNSText(){
            Log.i(TAG,"STARTING DNS TEST");
            try{
                Response response=dnsclient.newCall(dnsrequest).execute();
                Log.i(TAG,response.toString());
                if(response.code()==200){
                    dnsresponseTime=response.receivedResponseAtMillis()-response.sentRequestAtMillis();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mainActivity.dnsresponsetime.setText("DNS Response time: "+String.valueOf(dnsresponseTime/1000)+" seconds");
                        }
                    });
                    response.close();
                }else{
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dnsresponseTime=response.receivedResponseAtMillis()-response.sentRequestAtMillis();
                            mainActivity.dnsresponsetime.setText("DNS Response time: "+String.valueOf(dnsresponseTime/1000));
                        }
                    });
                    response.close();
                }
            }catch (Exception e){
                System.out.println(e);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mainActivity.dnsresponsetime.setText(getResources().getString(R.string.unable_to_connect));
                    }
                });
                createNotificationWithoutSound(getResources().getString(R.string.Radio_server_status),getResources().getString(R.string.unable_to_connect));
            }
            Log.i(TAG,"DNS SERVER TEST DONE");

        }
        
        private void createNotificationWithSound(String title,String text){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SOUND_NOTI_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // notificationId is a unique int for each notification that you must define
            startForeground(notificationId, builder.build());
        }
        
        private void createNotificationWithoutSound(String title,String text){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTI_CHANNEL_ID)
                    .setSilent(true)
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(NotificationCompat.PRIORITY_LOW )
                    .setSound(null)
                    .setOngoing(true)
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            // notificationId is a unique int for each notification that you must define
            startForeground(notificationId, builder.build());
        }
        private void createNotificationChannelWithSound() {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(SOUND_NOTI_CHANNEL_ID, name, importance);
                channel.setDescription(description);
                channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),null);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
        private void createNotificationChannelWithoutSound() {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = getString(R.string.channel_name);
                String description = getString(R.string.channel_description);
                int importance = NotificationManager.IMPORTANCE_DEFAULT;
                NotificationChannel channel = new NotificationChannel(NOTI_CHANNEL_ID, name, importance);
                channel.setDescription(description);
                channel.setSound(null,null);
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}