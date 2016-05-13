package com.bnsantos.uploader.events;

/**
 * Created by bruno on 13/05/16.
 */
public class UploadFinishEvent {
  public final String id;
  public final String url;

  public UploadFinishEvent(String id, String url) {
    this.id = id;
    this.url = url;
  }
}
