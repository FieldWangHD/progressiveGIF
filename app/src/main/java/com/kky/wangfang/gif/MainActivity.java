package com.kky.wangfang.gif;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageView = (ImageView) findViewById(R.id.gif_view);
        try {
            InputStream inputStream = getResources().getAssets().open("f.gif");

            final GifManager manager = new GifManager.Builder().load("http://ww2.sinaimg.cn/large/e4e2bea6jw1fariz3h6alg206k06o4qp.gif").into(imageView).build();

            manager.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
