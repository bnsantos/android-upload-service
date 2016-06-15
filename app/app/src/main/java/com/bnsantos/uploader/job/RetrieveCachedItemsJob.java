package com.bnsantos.uploader.job;

import android.util.Log;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;
import com.bnsantos.uploader.events.ItemsEvent;
import com.bnsantos.uploader.model.PersistenceManager;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

/**
 * Created by bruno on 15/06/16.
 */
public class RetrieveCachedItemsJob extends Job {
  public static final String TAG = RetrieveCachedItemsJob.class.getSimpleName();
  public static final int PRIORITY = 1;
  private final PersistenceManager persistenceManager;

  public RetrieveCachedItemsJob(PersistenceManager persistenceManager) {
    super(new Params(PRIORITY));
    this.persistenceManager = persistenceManager;
  }

  @Override
  public void onAdded() {

  }

  @Override
  public void onRun() throws Throwable {
    List items = persistenceManager.getItemDAO().queryForAll();
    EventBus.getDefault().post(new ItemsEvent(items));
  }

  @Override
  protected void onCancel(int cancelReason) {
    Log.i(TAG, "RetrieveCachedItemsJob cancelled for reason " + cancelReason);
  }

  @Override
  protected RetryConstraint shouldReRunOnThrowable(Throwable throwable, int runCount, int maxRunCount) {
    return RetryConstraint.createExponentialBackoff(runCount, 1000);
  }
}
