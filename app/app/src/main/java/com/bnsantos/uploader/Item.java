package com.bnsantos.uploader;

import android.net.Uri;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by bruno on 12/05/16.
 */
public class Item implements Serializable{
  private String id;
  private Uri uri;
  private boolean cloud;

  public Item(Uri uri, boolean cloud) {
    this.uri = uri;
    this.cloud = cloud;
    this.id = UUID.randomUUID().toString();
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

  @Override
  public boolean equals(Object o) {
    return id.equals(((Item)o).id);
  }

  public String getId() {
    return id;
  }
}
