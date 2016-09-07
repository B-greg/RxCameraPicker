package com.smartsoftasia.rxcamerapicker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class RxImageConverters {

  public static Observable<File> uriToFile(final Context context, final Uri uri, final File file) {
    return Observable.create(new Observable.OnSubscribe<File>() {
      @Override
      public void call(Subscriber<? super File> subscriber) {
        try {
          InputStream inputStream = context.getContentResolver().openInputStream(uri);
          copyInputStreamToFile(inputStream, file);
          subscriber.onNext(file);
        } catch (Exception e) {
          Log.e(RxImageConverters.class.getSimpleName(), "Error converting uri", e);
          subscriber.onError(e);
        }
      }
    })
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread());
  }

  private static void copyInputStreamToFile(InputStream in, File file) throws IOException {
    OutputStream out = new FileOutputStream(file);
    byte[] buf = new byte[10 * 1024];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    out.close();
    in.close();
  }

  public static Observable<Bitmap> uriToBitmap(final Context context, final Uri uri) {
    return Observable.create(new Observable.OnSubscribe<Bitmap>() {
      @Override
      public void call(Subscriber<? super Bitmap> subscriber) {
        try {
          Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
          subscriber.onNext(bitmap);
        } catch (IOException e) {
          Log.e(RxImageConverters.class.getSimpleName(), "Error converting uri", e);
          subscriber.onError(e);
        }
      }
    })
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static Observable<String> uriToFullPath(final Context context, final Uri originalUri) {
    return Observable
        .create(new Observable.OnSubscribe<String>() {
          @Override
          public void call(Subscriber<? super String> subscriber) {
            try {
              String fullPath = getFullPathFromUri(context, originalUri);
              subscriber.onNext(fullPath);
              subscriber.onCompleted();
            } catch (Throwable throwable) {
              subscriber.onError(throwable);
            }
          }
        })
        .subscribeOn(Schedulers.newThread())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static Observable<String> uriToCompressImageFullPath(final Context context, final String folderName, final Uri uri){
    return Observable.create(new Observable.OnSubscribe<String>() {
      @Override
      public void call(Subscriber<? super String> subscriber) {

        try {
          String imagePath = getFullPathFromUri(context, uri);

          ExifInterface oldExif = new ExifInterface(imagePath);
          String exifOrientation = oldExif.getAttribute(ExifInterface.TAG_ORIENTATION);

          Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          bitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);

          //you can create a new file name "test.jpg" in sdcard folder.
          File folderFile = Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_PICTURES + File.separator + folderName);
          if (!folderFile.exists()) {
            folderFile.mkdirs();
          }

          File f = new File(
              folderFile.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg");
          f.createNewFile();
          //write the bytes in file
          FileOutputStream fo = new FileOutputStream(f);
          fo.write(bytes.toByteArray());

          // remember close de FileOutput
          fo.close();

          String newImagePath = f.getAbsolutePath();
          if (exifOrientation != null) {
            ExifInterface newExif = new ExifInterface(newImagePath);
            newExif.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);
            newExif.saveAttributes();
          }

          subscriber.onNext(newImagePath);
          subscriber.onCompleted();
        } catch (Throwable e) {
          Log.e(RxImageConverters.class.getSimpleName(), "Error converting uri", e);
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
  }

  private static String getFullPathFromUri(Context context, Uri originalUri) throws Throwable {
    Cursor imageCursor = null;
    String finalPath = null;
    String imageId = null;
    String mediaId = null;
    String mediaData = null;
    String[] imageColumns = null;
    Uri uri;

    if (originalUri.getPath().contains("image")){
      mediaId =  MediaStore.Images.Media._ID;
      imageColumns  = new String[] { MediaStore.Images.Media.DATA };
      mediaData = MediaStore.Images.Media.DATA;
      if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
        uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
      } else {
        uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI;
      }

    }else if(originalUri.getPath().contains("video")){
      mediaId =  MediaStore.Video.Media._ID;
      imageColumns  = new String[] { MediaStore.Video.Media.DATA };
      mediaData = MediaStore.Video.Media.DATA;
      if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
      } else {
        uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI;
      }
    }else {
      throw new Throwable("No image found");
    }

    String[] imageSplit = originalUri.getLastPathSegment().split("%3A");
    if (!imageSplit[0].contains(":")){
      imageId = originalUri.getLastPathSegment().split("%3A")[0];
    }else if(imageSplit.length > 1){
      imageId = originalUri.getLastPathSegment().split("%3A")[0].split(":")[1];
    }

    if (imageId == null){
      throw new Throwable("No image found");
    }

    imageCursor = context.getContentResolver()
        .query(uri, imageColumns, mediaId + "=" + imageId, null, null);
    if (imageCursor != null && imageCursor.moveToFirst()) {
      finalPath = imageCursor.getString(
          imageCursor.getColumnIndexOrThrow(mediaData));
    } else {
      throw new Throwable("No image found");
    }

    imageCursor.close();

    return finalPath;
  }

}
