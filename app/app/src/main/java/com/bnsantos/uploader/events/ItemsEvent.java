package com.bnsantos.uploader.events;

import com.bnsantos.uploader.model.Item;

import java.util.List;

/**
 * Created by bruno on 15/06/16.
 */
public class ItemsEvent {
  public final List<Item> itemList;

  public ItemsEvent(List<Item> itemList) {
    this.itemList = itemList;
  }
}
