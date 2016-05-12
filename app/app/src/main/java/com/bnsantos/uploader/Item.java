package com.bnsantos.uploader;

import android.net.Uri;

/**
 * Created by bruno on 12/05/16.
 */
public class Item {
  private Uri uri;
  private boolean cloud;

  public Item(Uri uri, boolean cloud) {
    this.uri = uri;
    this.cloud = cloud;
  }

  public Uri getUri() {
    return uri;
  }

  public void setUri(Uri uri) {
    this.uri = uri;
  }

  public boolean isCloud() {
    return cloud;
  }

  public void setCloud(boolean cloud) {
    this.cloud = cloud;
  }
}
