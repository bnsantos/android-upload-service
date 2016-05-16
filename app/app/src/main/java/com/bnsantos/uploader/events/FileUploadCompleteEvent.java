package com.bnsantos.uploader.events;

/**
 * Created by bruno on 14/05/16.
 */
public class FileUploadCompleteEvent {
  public final String id;

  public FileUploadCompleteEvent(String id) {
    this.id = id;
  }
}
