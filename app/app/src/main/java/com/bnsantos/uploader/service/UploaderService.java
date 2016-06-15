package com.bnsantos.uploader.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.birbit.android.jobqueue.JobManager;
import com.bnsantos.uploader.App;
import com.bnsantos.uploader.R;
import com.bnsantos.uploader.events.FileUploadCompleteEvent;
import com.bnsantos.uploader.events.FileUploadProgressEvent;
import com.bnsantos.uploader.events.UploadFinishEvent;
import com.bnsantos.uploader.job.UploadJob;
import com.bnsantos.uploader.network.NetworkUploaderService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;

/**
 * Created by bruno on 13/05/16.
 */
public class UploaderService extends Service {
  private static final int NOTIFICATION_ID = 823;
  private static int COUNT = 0;
  private JobManager jobManager;
  private NetworkUploaderService service;
  private NotificationManagerCompat notificationManager;
  private NotificationCompat.Builder builder;

  @Override
  public void onCreate() {
    super.onCreate();
    App app = (App) getApplication();
    jobManager = app.getJobManager();
    service = app.getNetworkUploaderService();
    notificationManager = NotificationManagerCompat.from(this);
    EventBus.getDefault().register(this);
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(intent!=null){
      updateNotification(0);

      Uri uri = intent.getData();
      String id = intent.getStringExtra("id");

      COUNT++;
      jobManager.addJobInBackground(new UploadJob(this, id, uri, service));
      jobManager.start();
    }
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    EventBus.getDefault().unregister(this);
  }

  private void updateNotification(int progress){
    if(builder==null){
      builder = new NotificationCompat.Builder(this)
          .setWhen(Calendar.getInstance().getTimeInMillis())
          .setAutoCancel(false)
          .setProgress(100, progress, false)
          .setSmallIcon(R.drawable.ic_cloud_upload)
          .setTicker(getString(R.string.notification_ticker))
          .setContentTitle(getString(R.string.notification_title));
    }
    if(progress==100){
      builder.setContentText(getString(R.string.upload_finished));
      builder.setProgress(0, 0, false);
    }else {
      builder.setProgress(100, progress, false);
      builder.setContentText("Files remaining " + COUNT);
    }

    notificationManager.notify(NOTIFICATION_ID, builder.build());
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onUploadResponse(UploadFinishEvent event){
    COUNT--;

    Toast.makeText(UploaderService.this, "Url " + event.url, Toast.LENGTH_SHORT).show();

    if(COUNT == 0){
      updateNotification(100);
      stopSelf();
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onUploadProgress(FileUploadProgressEvent event){
    updateNotification(event.progress);
  }

  @Subscribe(threadMode = ThreadMode.MAIN) //TODO not being called, need to understand why
  public void onUploadComplete(FileUploadCompleteEvent event){
    Toast.makeText(UploaderService.this, "Finished " + event.id, Toast.LENGTH_SHORT).show();
    updateNotification(100);
  }
}
