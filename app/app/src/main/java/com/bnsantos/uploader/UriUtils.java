package com.bnsantos.uploader;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.facebook.common.media.MediaUtils;

import java.io.File;

/**
 * Created by bruno on 28/04/16.
 */
public class UriUtils {
  /**
   * Get a file path from a Uri. This will get the the path for Storage Access
   * Framework Documents, as well as the _data field for the MediaStore and
   * other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @author paulburke
   */
  @SuppressLint("NewApi")
  public static String getPath(final Context context, final Uri uri) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      if (isExternalStorageDocument(uri)) { // ExternalStorageProvider
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }else {
          Toast.makeText(context, "TODO", Toast.LENGTH_SHORT).show();
          /*//Below logic is how External Storage provider build URI for documents
          StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
          final StorageVolume[] volumes = mStorageManager.getVolumeList();
          for (StorageVolume volume : volumes) {
            final boolean mounted = Environment.MEDIA_MOUNTED.equals(volume.getState()) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(volume.getState());
            //if the media is not mounted, we need not get the volume details
            if (!mounted) continue;
            //Primary storage is already handled.
            if (volume.isPrimary() && volume.isEmulated()) continue;
            //Build the actual path based on the uuid
            if (volume.getUuid() != null && volume.getUuid().equals(type)) {
              return volume.getPath() + "/" +split[1];
            }
          }*/
        }
      }
      else if (isDownloadsDocument(uri)) {  // DownloadsProvider
        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

        return getDataColumn(context, contentUri, null, null);
      }
      else if (isMediaDocument(uri)) {  // MediaProvider
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] { split[1] };

        return getDataColumn(context, contentUri, selection, selectionArgs);
      }
    }
    else if ("content".equalsIgnoreCase(uri.getScheme())) { // MediaStore (and general)
      return getDataColumn(context, uri, null, null);
    }
    else if ("file".equalsIgnoreCase(uri.getScheme())) {  // File
      return uri.getPath();
    }

    return null;
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is ExternalStorageProvider.
   */
  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is DownloadsProvider.
   */
  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is MediaProvider.
   */
  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * Get the value of the data column for this Uri. This is useful for
   * MediaStore Uris, and other file-based ContentProviders.
   *
   * @param context The context.
   * @param uri The Uri to query.
   * @param selection (Optional) Filter used in the query.
   * @param selectionArgs (Optional) Selection arguments used in the query.
   * @return The value of the _data column, which is typically a file path.
   */
  public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = { column };

    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(column_index);
      }
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  public static boolean isImage(Context c, Uri u){
    return MediaUtils.isPhoto(extractMimeType(c, u));
  }

  public static String extractExtension(Context context, Uri uri){
    File f = new File(uri.getPath());
    if(f.isFile()&&f.exists()){
      return extractExtension(f.getPath());
    }else{
      return MimeTypeMap.getSingleton().getExtensionFromMimeType(context.getContentResolver().getType(uri));
    }
  }

  public static String extractMimeType(Context context, Uri uri){
    File f = new File(uri.getPath());
    if(f.isFile()){
      return MediaUtils.extractMime(f.getPath());
    }else{
      return context.getContentResolver().getType(uri);
    }
  }

  public static String extractExtension(String path) {
    int pos = path.lastIndexOf('.');
    if (pos < 0 || pos == path.length() - 1) {
      return null;
    }
    return path.substring(pos + 1);
  }


// Source https://gist.github.com/prasad321/9852037

}
