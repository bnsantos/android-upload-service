package com.bnsantos.uploader;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bruno on 12/05/16.
 */
public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
  private List<Item> itemList;
  private int width;
  private int height;

  public Adapter(ArrayList<Item> itemList, int imageSize) {
    this.itemList = itemList;
    this.width = imageSize;
    this.height = imageSize;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    holder.bind(itemList.get(position));
  }

  @Override
  public int getItemCount() {
    return itemList.size();
  }

  public void add(Item item){
    itemList.add(item);
    notifyItemInserted(itemList.size()-1);
  }

  public class ViewHolder extends RecyclerView.ViewHolder{
    private final SimpleDraweeView image;
    private final ImageView cloud;

    public ViewHolder(View itemView) {
      super(itemView);
      image = (SimpleDraweeView) itemView.findViewById(R.id.image);
      cloud = (ImageView) itemView.findViewById(R.id.cloud);
    }

    public void bind(Item item){
      DraweeController controller = Fresco.newDraweeControllerBuilder()
          .setImageRequest(ImageRequestBuilder.newBuilderWithSource(item.getUri()).setResizeOptions(new ResizeOptions(width, height)).build())
          .setAutoPlayAnimations(true)
          .setOldController(image.getController())
          .setTapToRetryEnabled(true)
          .build();
      image.setController(controller);

      if(item.isCloud()){
        cloud.setImageResource(R.drawable.ic_cloud);
      }else{
        cloud.setImageResource(R.drawable.ic_cloud_off);
      }
    }
  }
}
