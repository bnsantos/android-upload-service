package com.bnsantos.uploader.events;

/**
 * Created by bruno on 14/05/16.
 */
public class FileUploadProgressEvent {
  public final String id;
  public final int progress;

  public FileUploadProgressEvent(String id, int progress) {
    this.id = id;
    this.progress = progress;
  }
}
