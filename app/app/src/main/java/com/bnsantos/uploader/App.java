package com.bnsantos.uploader;

import android.app.Application;
import android.util.Log;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;
import com.bnsantos.uploader.model.PersistenceManager;
import com.bnsantos.uploader.network.NetworkUploaderService;
import com.facebook.drawee.backends.pipeline.Fresco;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by bruno on 12/05/16.
 */
public class App extends Application {
  private Retrofit retrofit;
  private NetworkUploaderService networkUploaderService;
  private JobManager jobManager;
  private PersistenceManager persistenceManager;

  @Override
  public void onCreate() {
    super.onCreate();
    Fresco.initialize(this);

    initRetrofit();
    initJobManager();
    initDB();
  }

  private void initDB() {
    persistenceManager = new PersistenceManager(this);
  }

  private void initRetrofit() {
    retrofit = new Retrofit.Builder()
        .baseUrl("http://192.168.1.33:3000")
        .addConverterFactory(GsonConverterFactory.create())

        .build();

    networkUploaderService = retrofit.create(NetworkUploaderService.class);
  }

  public NetworkUploaderService getNetworkUploaderService() {
    return networkUploaderService;
  }

  private void initJobManager() {
    Configuration.Builder builder = new Configuration.Builder(this)
        .customLogger(new CustomLogger() {
          private static final String TAG = "JOBS";
          @Override
          public boolean isDebugEnabled() {
            return true;
          }

          @Override
          public void d(String text, Object... args) {
            Log.d(TAG, String.format(text, args));
          }

          @Override
          public void e(Throwable t, String text, Object... args) {
            Log.e(TAG, String.format(text, args), t);
          }

          @Override
          public void e(String text, Object... args) {
            Log.e(TAG, String.format(text, args));
          }
        })
        .minConsumerCount(1)//always keep at least one consumer alive
        .maxConsumerCount(3)//up to 3 consumers at a time
        .loadFactor(3)//3 jobs per consumer
        .consumerKeepAlive(120);//wait 2 minute

    jobManager = new JobManager(builder.build());
  }

  public JobManager getJobManager() {
    return jobManager;
  }

  public PersistenceManager getPersistenceManager() {
    return persistenceManager;
  }
}
