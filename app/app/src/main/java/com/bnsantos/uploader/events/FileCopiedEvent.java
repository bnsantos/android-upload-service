package com.bnsantos.uploader.events;

import android.net.Uri;

/**
 * Created by bruno on 16/06/16.
 */
public class FileCopiedEvent {
  public final String itemId;
  public final Uri copy;

  public FileCopiedEvent(String itemId, Uri copy) {
    this.itemId = itemId;
    this.copy = copy;
  }
}
