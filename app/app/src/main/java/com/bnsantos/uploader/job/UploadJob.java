package com.bnsantos.uploader.job;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.Item;
import com.bnsantos.uploader.UriUtils;
import com.bnsantos.uploader.events.UploadFinishEvent;
import com.bnsantos.uploader.network.UploadResponse;
import com.bnsantos.uploader.network.UploaderService;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Created by bruno on 12/05/16.
 */
public class UploadJob extends Job {
  public static final int PRIORITY = 1;
  public static final String TAG = UploadJob.class.getSimpleName();

  private final Item item;
  private final UploaderService service;
  private final WeakReference<Context> weakReference;

  public UploadJob(Context context, Item item, UploaderService service) {
    super(new Params(PRIORITY).requireNetwork());//Cannot persist because URI is not serializable
    this.item = item;
    this.service = service;
    this.weakReference = new WeakReference<>(context);
  }

  @Override
  public void onAdded() {
    Log.i(TAG, "Job added for item " + item);
  }

  @Override
  public void onRun() throws Throwable {
    Log.i(TAG, "Job started to run for item " + item);
    String path = UriUtils.getPath(weakReference.get(), item.getUri());
    File file;
    if(path==null){
      file = File.createTempFile("IMG - " + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date()), "jpg", weakReference.get().getCacheDir());


      InputStream inputStream = weakReference.get().getContentResolver().openInputStream(item.getUri());
      OutputStream outputStream = new FileOutputStream(file);

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      inputStream.close();
      outputStream.close();
    }else {
      file = new File(path);
    }

    String filename = "IMG_" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date())+ ".jpg";
    Response<UploadResponse> execute = service.upload(filename, MultipartBody.Part.createFormData("file", "value", RequestBody.create(MediaType.parse("image/jpg"), file))).execute();

    if(execute.isSuccessful()){
      UploadResponse body = execute.body();
      item.setUri(Uri.parse(body.Location));
      item.setCloud(true);
      EventBus.getDefault().post(new UploadFinishEvent(item));
    }else{
      ResponseBody responseBody = execute.errorBody();
      Log.i(TAG, "Error");
    }
    Log.i(TAG, "Job finished to run for item " + item);
  }

  @Override
  protected void onCancel(int cancelReason) {
    Log.i(TAG, "Job cancelled for item " + item);

  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    return RetryConstraint.createExponentialBackoff(runCount, 1000);
  }
}
