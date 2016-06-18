package com.bnsantos.uploader.job;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.UriUtils;
import com.bnsantos.uploader.events.FileCopiedEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Random;

/**
 * Created by bruno on 16/06/16.
 */
public class CopyJob extends Job{
  public static final int PRIORITY = 2;
  public static final String TAG = CopyJob.class.getSimpleName();

  private final WeakReference<Context> weakReference;
  private final Random random;
  private final String itemId;
  private final Uri uri;

  public CopyJob(Context context, String itemId, Uri uri) {
    super(new Params(PRIORITY));
    this.random = new Random(System.currentTimeMillis());
    this.itemId = itemId;
    this.uri = uri;
    this.weakReference = new WeakReference<>(context);
    Log.i(TAG, "CREATED CopyJob for Item["+itemId+"] Uri["+uri.toString()+"]");
  }

  @Override
  public void onAdded() {
    Log.i(TAG, "ADDED UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");
  }

  @Override
  public void onRun() throws Throwable {
    Log.i(TAG, "RUNNING UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");
    String path = UriUtils.getPath(weakReference.get(), uri);
    File copy;
    if(path==null){
      copy = File.createTempFile(Integer.toString(random.nextInt()), "." + UriUtils.extractExtension(weakReference.get(), uri), weakReference.get().getCacheDir());
      InputStream inputStream = weakReference.get().getContentResolver().openInputStream(uri);
      OutputStream outputStream = new FileOutputStream(copy);

      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      inputStream.close();
      outputStream.close();
    }else {
      copy = File.createTempFile(Integer.toString(random.nextInt()), "." + UriUtils.extractExtension(path), weakReference.get().getCacheDir());
      File original = new File(path);
      copy(original, copy);
    }

    //Update item?

    EventBus.getDefault().post(new FileCopiedEvent(itemId, Uri.fromFile(copy)));
  }

  @Override
  protected void onCancel(int cancelReason) {
    Log.i(TAG, "CANCELLED UploadJob for Item["+itemId+"] Uri["+uri.toString()+"]");

  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    return RetryConstraint.createExponentialBackoff(runCount, 0);
  }

  private void copy(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }
}
