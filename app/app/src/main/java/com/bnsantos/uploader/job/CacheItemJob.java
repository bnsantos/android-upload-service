package com.bnsantos.uploader.job;

import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.model.Item;
import com.bnsantos.uploader.model.PersistenceManager;
import com.j256.ormlite.dao.Dao;

/**
 * Created by bruno on 15/06/16.
 */
public class CacheItemJob extends Job{
  public static final String TAG = CacheItemJob.class.getSimpleName();
  public static final int PRIORITY = 1;
  private final PersistenceManager persistenceManager;
  private final Item item;

  public CacheItemJob(PersistenceManager persistenceManager, Item item){
    super(new Params(PRIORITY));
    this.persistenceManager = persistenceManager;
    this.item = item;
  }
  @Override
  public void onAdded() {

  }

  @Override
  public void onRun() throws Throwable {
    Dao.CreateOrUpdateStatus update = persistenceManager.getItemDAO().createOrUpdate(item);
    Log.i(TAG, "Create or update "+update);
  }

  @Override
  protected void onCancel(int cancelReason) {

  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    return RetryConstraint.createExponentialBackoff(runCount, 1000);
  }
}
