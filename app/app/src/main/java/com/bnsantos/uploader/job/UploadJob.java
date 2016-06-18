package com.bnsantos.uploader.job;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.UriUtils;
import com.bnsantos.uploader.events.FileUploadCancelled;
import com.bnsantos.uploader.events.FileUploadProgressEvent;
import com.bnsantos.uploader.events.UploadFinishEvent;
import com.bnsantos.uploader.network.NetworkUploaderService;
import com.bnsantos.uploader.network.ProgressRequestBody;
import com.bnsantos.uploader.network.UploadResponse;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
  private static  final int MAX_RETRY = 5;
  public static final int PRIORITY = 1;
  public static final String TAG = UploadJob.class.getSimpleName();

  private final Random random;
  private final String filename;
  private final String itemId;
  private final Uri uri;
  private final NetworkUploaderService service;
  private int retry;

  private final WeakReference<Context> weakReference;


  public UploadJob(Context context, String ItemId, Uri uri, NetworkUploaderService service) {
    super(new Params(PRIORITY).requireNetwork());//Cannot persist because URI is not serializable
    this.random = new Random(System.currentTimeMillis());
    this.filename = "UPLOAD_" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date()) + "-" + random.nextInt()  + "." + UriUtils.extractExtension(context, uri);
    this.weakReference = new WeakReference<>(context);
    this.itemId = ItemId;
    this.uri = uri;
    this.service = service;
    this.retry = 0;
    Log.i(TAG, "CREATED UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");
  }

  @Override
  public void onAdded() {
    Log.i(TAG, "ADDED UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");
  }

  @Override
  public void onRun() throws Throwable {
    Log.i(TAG, "RUNNING UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");

    if(MediaUtils.isPhoto(UriUtils.extractMimeType(weakReference.get(), uri))) {
      DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(ImageRequest.fromUri(uri), weakReference.get());
      Throwable failureCause = dataSource.getFailureCause();
      if (failureCause != null) {
        throw failureCause;
      }

      CloseableReference<CloseableImage> result = dataSource.getResult();
      if (result != null) {
        CloseableImage closeableImage = result.get();
        if (closeableImage instanceof CloseableBitmap) {
          Bitmap bitmap = ((CloseableBitmap) closeableImage).getUnderlyingBitmap();
          uploadFile(createImageTempFile(bitmap));
        }
      }
    }else{
      uploadFile(new File(uri.getPath()));
    }
  }

  private File createImageTempFile(Bitmap b) throws IOException {
    File f = File.createTempFile(Integer.toString(random.nextInt()), ".jpg", weakReference.get().getCacheDir());
    FileOutputStream out = new FileOutputStream(f);
    b.compress(Bitmap.CompressFormat.JPEG, 70, out); // bmp is your Bitmap instance
    out.flush();
    out.close();
    return f;
  }

  private void uploadFile(File file) throws IOException {
    Log.i(TAG, "UPLOADING UploadJob for Item[" + itemId + "] Uri[" + uri.toString() + "]");

    Call<UploadResponse> upload = service.upload(filename,
        MultipartBody.Part.createFormData("file", "value",
            new ProgressRequestBody(file, new ProgressRequestBody.UploadCallbacks() {
              @Override
              public void onProgressUpdate(int percentage) {
                EventBus.getDefault().post(new FileUploadProgressEvent(itemId, percentage));
              }

              @Override
              public void onError() {
                Log.e(TAG, "Error: " + itemId);
              }

              @Override
              public void onFinish() {
              }
            })),
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(UriUtils.extractExtension(file.getPath())));
    Response<UploadResponse> execute = upload.execute();

    if (execute.isSuccessful()) {
      UploadResponse body = execute.body();
      Log.i(TAG, "FINISHED-UPLOAD UploadJob for Item[" + itemId + "] Path[" + file.getName() + "] Uri[" + uri.toString() + "] Url[" + body.Location + "]");
      EventBus.getDefault().post(new UploadFinishEvent(itemId, body.Location));
    } else {
      ResponseBody responseBody = execute.errorBody();
      Log.e(TAG, "ERROR UploadJob " + execute.body().Bucket);
      Log.i(TAG, "ERROR UploadJob for Item[" + itemId + "] Path[" + file.getName() + "]");
    }
    Log.i(TAG, "FINISHED UploadJob for Item[" + itemId + "] Path[" + file.getName() + "] + Uri[" + uri.toString() + "]");
  }

  @Override
  protected void onCancel(int cancelReason) {
    Log.i(TAG, "CANCELLED UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");
    EventBus.getDefault().post(new FileUploadCancelled(itemId));
  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    retry++;
    if(retry<MAX_RETRY) {
      return RetryConstraint.createExponentialBackoff(runCount, 1000);
    }else{
      return RetryConstraint.CANCEL;
    }
  }
}
