package com.bnsantos.uploader.job;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.UriUtils;
import com.bnsantos.uploader.events.FileUploadProgressEvent;
import com.bnsantos.uploader.events.UploadFinishEvent;
import com.bnsantos.uploader.network.NetworkUploaderService;
import com.bnsantos.uploader.network.ProgressRequestBody;
import com.bnsantos.uploader.network.UploadResponse;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by bruno on 12/05/16.
 */
public class UploadJob extends Job {
  public static final int PRIORITY = 1;
  public static final String TAG = UploadJob.class.getSimpleName();

  private final Random random;
  private final String filename;
  private final String id;
  private final Uri uri;
  private final NetworkUploaderService service;
  private final WeakReference<Context> weakReference;


  public UploadJob(Context context, String id, Uri uri, NetworkUploaderService service) {
    super(new Params(PRIORITY).requireNetwork());//Cannot persist because URI is not serializable
    this.random = new Random(System.currentTimeMillis());
    this.filename = "IMG_" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date()) + "-" + random.nextInt()  + ".jpg";

    this.id = id;
    this.uri = uri;
    this.service = service;
    this.weakReference = new WeakReference<>(context);
    Log.i(TAG, "CREATED UploadJob for Item["+id+"] Uri["+uri.toString()+"]");
  }

  @Override
  public void onAdded() {
    Log.i(TAG, "ADDED UploadJob for Item["+id+"] Uri["+uri.toString()+"]");
  }

  @Override
  public void onRun() throws Throwable {
    Log.i(TAG, "RUNNING UploadJob for Item["+id+"] Uri["+uri.toString()+"]");
    String path = UriUtils.getPath(weakReference.get(), uri);
    File file = new File(path);
    Log.i(TAG, "UPLOADING UploadJob for Item["+id+"] Uri["+uri.toString()+"]");

    Call<UploadResponse> upload = service.upload(filename,
        MultipartBody.Part.createFormData("file", "value",
        new ProgressRequestBody(file, new ProgressRequestBody.UploadCallbacks() {
          @Override
          public void onProgressUpdate(int percentage) {
            EventBus.getDefault().post(new FileUploadProgressEvent(id, percentage));
          }

          @Override
          public void onError() {
            Log.e(TAG, "Error: " + id);
          }

          @Override
          public void onFinish() {
          }
        })));
    Response<UploadResponse> execute = upload.execute();

    if(execute.isSuccessful()){
      UploadResponse body = execute.body();
      Log.i(TAG, "FINISHED-UPLOAD UploadJob for Item["+id+"] Path["+path+"] Uri["+uri.toString()+"] Url["+body.Location+"]");
      EventBus.getDefault().post(new UploadFinishEvent(id, body.Location));
    }else{
      ResponseBody responseBody = execute.errorBody();
      Log.i(TAG, "ERROR UploadJob for Item["+id+"] Path["+path+"]");
    }
    Log.i(TAG, "FINISHED UploadJob for Item["+id+"] Path["+path+"] + Uri["+uri.toString()+"]");
  }

  @Override
  protected void onCancel(int cancelReason) {
    Log.i(TAG, "CANCELLED UploadJob for Item["+id+"] Uri["+uri.toString()+"]");
  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    return RetryConstraint.createExponentialBackoff(runCount, 1000);
  }
}
