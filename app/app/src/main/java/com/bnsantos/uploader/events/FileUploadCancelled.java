package com.bnsantos.uploader.events;

/**
 * Created by bruno on 17/06/16.
 */
public class FileUploadCancelled {
  public final String itemId;

  public FileUploadCancelled(String itemId) {
    this.itemId = itemId;
  }
}
