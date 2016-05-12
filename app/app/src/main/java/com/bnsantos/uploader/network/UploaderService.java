package com.bnsantos.uploader.network;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Created by bruno on 12/05/16.
 */
public interface UploaderService {
  @Multipart
  @POST("/upload")
  Call<UploadResponse> upload(@Header("X-File-Name") String filename, @Part() MultipartBody.Part file);
}
