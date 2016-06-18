package com.bnsantos.uploader.model;

import android.net.Uri;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by bruno on 12/05/16.
 */
@DatabaseTable(tableName = "items")
public class Item implements Serializable{
  @DatabaseField(canBeNull = false, id = true, columnName = "id")
  private String id;
  private Uri uri;
  @DatabaseField(columnName = "path")
  private String path;
  @DatabaseField(columnName = "cloud")
  private boolean cloud;

  public Item() {
  }

  public Item(Uri uri, boolean cloud) {
    this.uri = uri;
    this.cloud = cloud;
    this.id = UUID.randomUUID().toString();
    this.path = uri.toString();
  }

  public Item(String id, String uri, boolean cloud) {
    this.id = id;
    this.uri = Uri.parse(uri);
    this.cloud = cloud;
  }

  public Uri getUri() {
    return uri != null ? uri : Uri.parse(path);
  }

  public void setUri(Uri uri) {
    this.uri = uri;
    this.path = uri.toString();
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

  public void setId(String id) {
    this.id = id;
  }

  public void setPath(String path) {
    this.path = path;
    this.uri = Uri.parse(path);
  }

  public String getPath() {
    return path;
  }
}
