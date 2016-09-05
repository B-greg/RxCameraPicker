package com.smartsoftasia.rxcamerapickersample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import com.smartsoftasia.rxcamerapicker.HiddenActivity;
import com.smartsoftasia.rxcamerapicker.RxImagePicker;
import com.smartsoftasia.rxcamerapicker.Sources;
import java.util.List;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        RxImagePicker.with(getApplicationContext()).requestImage(Sources.VIDEO)
            .subscribe(new Action1<Uri>() {
              @Override
              public void call(Uri uri) {
                //Get image by uri using one of image loading libraries. I use Glide in sample app.
              }
            });
      }
    });
  }
}
