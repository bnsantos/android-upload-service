package com.bnsantos.uploader.events;

/**
 * Created by bruno on 14/05/16.
 */
public class FileUploadErrorEvent {
  public final String id;

  public FileUploadErrorEvent(String id) {
    this.id = id;
  }
}
