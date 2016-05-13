package com.bnsantos.uploader.events;

import com.bnsantos.uploader.Item;

/**
 * Created by bruno on 13/05/16.
 */
public class UploadFinishEvent {
  public final Item item;

  public UploadFinishEvent(Item item) {
    this.item = item;
  }
}
