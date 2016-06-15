package com.bnsantos.uploader;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.widget.Toast;

import com.bnsantos.uploader.databinding.ActivityMainBinding;
import com.bnsantos.uploader.events.UploadFinishEvent;
import com.bnsantos.uploader.service.UploaderService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private static final int INTENT_IMAGE_GALLERY = 123;
  private ActivityMainBinding binding;
  private Adapter adapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

    adapter = new Adapter(new ArrayList<Item>(), getResources().getDimensionPixelSize(R.dimen.image_size));
    binding.recyclerView.setAdapter(adapter);

    binding.fab.setOnClickListener(this);

    Item item = retrieveItem();
    if(item!=null){
      adapter.add(item);
    }
  }

  @Override
  public void onClick(View view) {
    Intent pickPhoto;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      pickPhoto = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    }else {
      pickPhoto = new Intent(Intent.ACTION_GET_CONTENT);
    }

    pickPhoto.setType("image/*");
    pickPhoto.addCategory(Intent.CATEGORY_OPENABLE);
    pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      pickPhoto.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); //TODO only one picture so far
    }

    if(pickPhoto.resolveActivity(getPackageManager())!=null){
      startActivityForResult(pickPhoto, INTENT_IMAGE_GALLERY);
    }else{
      Toast.makeText(MainActivity.this, R.string.error_no_gallery_app, Toast.LENGTH_SHORT).show();
    }
  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if(requestCode == INTENT_IMAGE_GALLERY && resultCode == RESULT_OK){
      final int takeFlags = (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        if(data.getData()!=null) {
          getContentResolver().takePersistableUriPermission(data.getData(), takeFlags);
        }else if(data.getClipData()!=null&&data.getClipData().getItemCount()>0){
          for (int i = 0; i < data.getClipData().getItemCount(); i++) {
            getContentResolver().takePersistableUriPermission(data.getClipData().getItemAt(i).getUri(), takeFlags);
          }
        }
      }
      handleImage(data);
    }
  }

  private void handleImage(Intent intent) {
    if(intent!=null){
      if(intent.getData()!=null){
        Item item = new Item(intent.getData(), false);
        putItem(item);
        adapter.add(item);
        upload(item);
      }else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          ClipData clipData = intent.getClipData();
          if (clipData!=null) {
            for (int i = 0; i < clipData.getItemCount() ; i++) {
              Uri uri = clipData.getItemAt(i).getUri();
              adapter.add(new Item(uri, false));
            }
          }
        }
      }
    }
  }

  private void upload(Item item) {
    Intent uploadService = new Intent(this, UploaderService.class);
    Intent uriIntent = new Intent("uri", item.getUri());
    uploadService.putExtra("uri", uriIntent);
    uploadService.putExtra("id", item.getId());
    startService(uploadService);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onMessageEvent(UploadFinishEvent event){
    adapter.replace(event.id, event.url);
  }

  @Override
  protected void onStart() {
    super.onStart();
    EventBus.getDefault().register(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }

  private static final String ITEM_ID = "item_id";
  private static final String ITEM_URI = "item_uri";
  private static final String ITEM_CLOUD = "item_cloud";

  private void putItem(Item item){
    SharedPreferences.Editor editor = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE).edit();
    editor.putString(ITEM_ID, item.getId());
    editor.putString(ITEM_URI, item.getUri().toString());
    editor.putBoolean(ITEM_CLOUD, item.isCloud());
    editor.apply();
  }

  private Item retrieveItem(){
    SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
    Item item = null;
    if(sharedPreferences.contains(ITEM_ID)){
      item = new Item(sharedPreferences.getString(ITEM_ID, null), sharedPreferences.getString(ITEM_URI, null), sharedPreferences.getBoolean(ITEM_CLOUD, false));
    }
    return item;
  }
}
