package com.bnsantos.uploader;

import android.content.ClipData;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.birbit.android.jobqueue.JobManager;
import com.bnsantos.uploader.databinding.ActivityMainBinding;
import com.bnsantos.uploader.events.ImageCopiedEvent;
import com.bnsantos.uploader.events.ItemsEvent;
import com.bnsantos.uploader.events.UploadFinishEvent;
import com.bnsantos.uploader.job.CacheItemJob;
import com.bnsantos.uploader.job.RetrieveCachedItemsJob;
import com.bnsantos.uploader.job.UpdateItemJob;
import com.bnsantos.uploader.model.Item;
import com.bnsantos.uploader.model.PersistenceManager;
import com.bnsantos.uploader.service.UploaderService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private static final int INTENT_IMAGE_GALLERY = 123;
  private ActivityMainBinding binding;
  private Adapter adapter;
  private PersistenceManager persistenceManager;
  private JobManager jobManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

    adapter = new Adapter(new ArrayList<Item>(), getResources().getDimensionPixelSize(R.dimen.image_size));
    binding.recyclerView.setAdapter(adapter);

    binding.fab.setOnClickListener(this);

    App app = (App) getApplication();
    persistenceManager = app.getPersistenceManager();
    jobManager = app.getJobManager();

    jobManager.addJobInBackground(new RetrieveCachedItemsJob(persistenceManager));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if(item.getItemId() == R.id.clear){
      clear();
      return true;
    }
    return false;
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
      pickPhoto.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
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
        jobManager.addJobInBackground(new CacheItemJob(persistenceManager, item));
        adapter.add(item);
        copy(item);
      }else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
          ClipData clipData = intent.getClipData();
          if (clipData!=null) {
            for (int i = 0; i < clipData.getItemCount() ; i++) {
              Uri uri = clipData.getItemAt(i).getUri();
              Item item = new Item(uri, false);
              jobManager.addJobInBackground(new CacheItemJob(persistenceManager, item));
              copy(item);
              adapter.add(item);
            }
          }
        }
      }
    }
  }

  private void upload(Item item) {
    Intent uploadService = new Intent(this, UploaderService.class);
    uploadService.putExtra("upload", true);
    uploadService.setData(item.getUri());
    uploadService.putExtra("id", item.getId());
    startService(uploadService);
  }

  private void copy(Item item){
    Intent uploadService = new Intent(this, UploaderService.class);
    uploadService.putExtra("upload", false);
    uploadService.setData(item.getUri());
    uploadService.putExtra("id", item.getId());
    startService(uploadService);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onMessageEvent(UploadFinishEvent event){
    adapter.replace(event.id, event.url);
    jobManager.addJobInBackground(new UpdateItemJob(persistenceManager, event.id, event.url, true));
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onItems(ItemsEvent event){
    adapter.add(event.itemList);
    for (Item item : event.itemList) {
      if(!item.isCloud()){
        if(item.getUri()==null){
          item.setUri(Uri.parse(item.getPath()));
        }
        upload(item);
      }
    }
  }

  @Subscribe(threadMode = ThreadMode.BACKGROUND)
  public void onImageCopied(ImageCopiedEvent event){
    jobManager.addJobInBackground(new UpdateItemJob(persistenceManager, event.itemId, event.copy.toString(), false));
    upload(new Item(event.itemId, event.copy.toString(), false));
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

  private void clear(){
    adapter.clear();
    persistenceManager.clearTables();
  }
}
