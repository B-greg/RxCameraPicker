package com.smartsoftasia.rxcamerapicker;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HiddenActivity extends Activity {
  public static final String TAG = "HiddenActivity";
  private static final String KEY_CAMERA_PICTURE_URL = "cameraPictureUrl";

  public static final String IMAGE_SOURCE = "image_source";
  public static final String ALLOW_MULTIPLE_IMAGES = "allow_multiple_images";

  private static final int SELECT_PHOTO = 100;
  private static final int TAKE_PHOTO = 101;
  public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;

  private Uri cameraPictureUrl;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null) {
      handleIntent(getIntent());
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putParcelable(KEY_CAMERA_PICTURE_URL, cameraPictureUrl);
    super.onSaveInstanceState(outState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    cameraPictureUrl = savedInstanceState.getParcelable(KEY_CAMERA_PICTURE_URL);
  }

  @Override
  protected void onNewIntent(Intent intent) {
    handleIntent(intent);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                         int[] grantResults) {

    Log.d(TAG, "Permission callback called-------");
    switch (requestCode) {
      case REQUEST_ID_MULTIPLE_PERMISSIONS: {

        Map<String, Integer> perms = new HashMap<>();
        // Initialize the map with both permissions
        perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
        perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
        // Fill with actual results from user
        if (grantResults.length > 0) {
          for (int i = 0; i < permissions.length; i++)
            perms.put(permissions[i], grantResults[i]);
          // Check for both permissions
          if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
              && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE)
              == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "sms & location services permission granted");
            // process the normal flow
            //else any one or both the permissions are not granted
          } else {
            Log.d(TAG, "Some permissions are not granted ask again ");
            //permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
            //                        // shouldShowRequestPermissionRationale will return true
            //show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA) || ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
              showDialogOK("SMS and Location Services Permission required for this app",
                  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                      switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                          checkPermission();
                          break;
                        case DialogInterface.BUTTON_NEGATIVE:
                          // proceed with logic by disabling the related features or quit the app.
                          break;
                      }
                    }
                  });
            }
            //permission is denied (and never ask again is  checked)
            //shouldShowRequestPermissionRationale will return false
            else {
              Toast.makeText(this, "Go to settings and enable permissions", Toast.LENGTH_LONG)
                  .show();
              //                            //proceed with logic by disabling the related features or quit the app.
            }
          }
        }
      }
    }

/*    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      handleIntent(getIntent());
    } else {
      finish();
    }*/
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (resultCode == RESULT_OK) {
      switch (requestCode) {
        case SELECT_PHOTO:
          handleGalleryResult(data);
          break;
        case TAKE_PHOTO:
          RxImagePicker.with(this).onImagePicked(cameraPictureUrl);
          break;
      }
    }
    finish();
  }

  private void handleGalleryResult(Intent data) {
    if (getIntent().getBooleanExtra(ALLOW_MULTIPLE_IMAGES, false)) {
      ArrayList<Uri> imageUris = new ArrayList<>();
      ClipData clipData = data.getClipData();
      if (clipData != null) {
        for (int i = 0; i < clipData.getItemCount(); i++) {
          imageUris.add(clipData.getItemAt(i).getUri());
        }
      } else {
        imageUris.add(data.getData());
      }
      RxImagePicker.with(this).onImagesPicked(imageUris);
    } else {
      RxImagePicker.with(this).onImagePicked(data.getData());
    }
  }

  private void handleIntent(Intent intent) {
    if (!checkPermission()) {
      return;
    }

    Sources sourceType = Sources.values()[intent.getIntExtra(IMAGE_SOURCE, 0)];
    int chooseCode = 0;
    Intent pictureChooseIntent = null;

    switch (sourceType) {
      case CAMERA:
        cameraPictureUrl = createImageUri();
        pictureChooseIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        pictureChooseIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPictureUrl);
        chooseCode = TAKE_PHOTO;
        break;
      case GALLERY:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
          pictureChooseIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          pictureChooseIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,
              getIntent().getBooleanExtra(ALLOW_MULTIPLE_IMAGES, false));
          pictureChooseIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        } else {
          pictureChooseIntent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        pictureChooseIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        pictureChooseIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pictureChooseIntent.setType("image/*");
        chooseCode = SELECT_PHOTO;
        break;
    }

    startActivityForResult(pictureChooseIntent, chooseCode);
  }

  private boolean checkPermission() {
    int permissionSendMessage = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
    int locationPermission = ContextCompat.checkSelfPermission(this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE);
    List<String> listPermissionsNeeded = new ArrayList<>();
    if (locationPermission != PackageManager.PERMISSION_GRANTED) {
      listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
      listPermissionsNeeded.add(Manifest.permission.CAMERA);
    }
    if (!listPermissionsNeeded.isEmpty()) {
      ActivityCompat.requestPermissions(this,
          listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
          REQUEST_ID_MULTIPLE_PERMISSIONS);
      return false;
    }
    return true;
  }

  private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
    new AlertDialog.Builder(this).setMessage(message)
        .setPositiveButton(getString(android.R.string.ok), okListener)
        .setNegativeButton(getString(android.R.string.cancel), okListener)
        .create()
        .show();
  }

  private Uri createImageUri() {
    ContentResolver contentResolver = getContentResolver();
    ContentValues cv = new ContentValues();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(
        new Date());
    cv.put(MediaStore.Images.Media.TITLE, timeStamp);
    return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
  }
}