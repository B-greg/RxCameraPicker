package com.smartsoftasia.rxcamerapickersample;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import com.smartsoftasia.rxcamerapicker.HiddenActivity;
import com.smartsoftasia.rxcamerapicker.RxImageConverters;
import com.smartsoftasia.rxcamerapicker.RxImagePicker;
import com.smartsoftasia.rxcamerapicker.Sources;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

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
        RxImagePicker.with(getApplicationContext()).requestImage(Sources.VIDEO).flatMap(new Func1<Uri, Observable<?>>() {
          @Override
          public Observable<String> call(Uri uri) {
            return  RxImageConverters.uriToFullPath(getApplicationContext(), uri);
          }
        }).subscribe(new Action1<Object>() {
          @Override
          public void call(Object o) {
            Log.e("", o.toString());
          }
        });
      }
    });
  }
}
