package com.bnsantos.uploader.job;

import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.model.Item;
import com.bnsantos.uploader.model.PersistenceManager;

/**
 * Created by bruno on 15/06/16.
 */
public class UpdateItemJob extends Job {
  public static final String TAG = UpdateItemJob.class.getSimpleName();
  public static final int PRIORITY = 1;
  private final PersistenceManager persistenceManager;
  private final String id;
  private final String url;

  public UpdateItemJob(PersistenceManager persistenceManager, String id, String url) {
    super(new Params(PRIORITY));
    this.persistenceManager = persistenceManager;
    this.id = id;
    this.url = url;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun() throws Throwable {
    Item item = (Item) persistenceManager.getItemDAO().queryForId(id);
    if(item!=null){
      item.setPath(url);
      item.setCloud(true);
      int update = persistenceManager.getItemDAO().update(item);
      Log.i(TAG, "Updated item [" + id + "] " + update);
    }
  }

  @Override
  protected void onCancel(int cancelReason) {

  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    return RetryConstraint.createExponentialBackoff(runCount, 1000);
  }
}
