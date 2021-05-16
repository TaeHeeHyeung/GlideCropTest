package com.example.glidecroptest;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class MainActivity extends AppCompatActivity {

    private ImageView iv_crop;
    private ImageView iv_ori;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_crop = findViewById(R.id.iv_crop);
        iv_ori = findViewById(R.id.iv_ori);
        Glide.with(MainActivity.this)
                .load(R.drawable.test)
                .into(iv_ori);
        iv_crop.post(new Runnable() {
            @Override
            public void run() {
                Drawable drawable = getResources().getDrawable(R.drawable.test);
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

                //실제 이미지의 가로 세로 크기
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                //이미지 뷰의 가로 세로 크기
                int ivW = iv_crop.getWidth();
                int ivH = iv_crop.getHeight();
                //실제 이미지 크기로부터 잘라야되는 이미지 크기
                RectF cropRectF = new RectF(width / 4f, height / 4f, width * 3 / 4f, height * 3 / 4f);
                CustomTransFormation customTransFormation = new CustomTransFormation(
                        MainActivity.this, R.drawable.test,
                        ivW,
                        ivH,
                        cropRectF,
                        width,
                        height, false);
                customTransFormation.getAsyncCallback(
                        new CustomTransFormation.BitmapCallback() {
                            @Override
                            public void getBitmap(Bitmap bitmap) {
                                iv_crop.setImageBitmap(bitmap);
                            }
                        }
                );
            }
        });


    }
}