package com.example.glidecroptest;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Synthetic;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// extends BitmapTransformation
public class CustomTransFormation {
    private final int id;
    private String TAG = CustomTransFormation.class.getSimpleName();
    private String filePath;
    //캐시에 사용 됨
    private final Context context;


    private static final String ID = "com.bumptech.glide.load.resource.bitmap.CenterCrop";
    String STRING_CHARSET_NAME = "UTF-8";
    Charset CHARSET = Charset.forName(STRING_CHARSET_NAME);
    //dither: 잡음 방지 , FILTER_BITMAP_FLAG: 샘플링 가능
    public static final int PAINT_FLAGS = Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG;
    private static final Paint DEFAULT_PAINT = new Paint(PAINT_FLAGS);
    // See #738.
    private static final Set<String> MODELS_REQUIRING_BITMAP_LOCK = new HashSet<>(
            Arrays.asList(// Moto X gen 2
                    "XT1085",
                    "XT1092",
                    "XT1093",
                    "XT1094",
                    "XT1095",
                    "XT1096",
                    "XT1097",
                    "XT1098",
                    // Moto G gen 1
                    "XT1031",
                    "XT1028",
                    "XT937C",
                    "XT1032",
                    "XT1008",
                    "XT1033",
                    "XT1035",
                    "XT1034",
                    "XT939G",
                    "XT1039",
                    "XT1040",
                    "XT1042",
                    "XT1045",
                    // Moto G gen 2
                    "XT1063",
                    "XT1064",
                    "XT1068",
                    "XT1069",
                    "XT1072",
                    "XT1077",
                    "XT1078",
                    "XT1079"));
    private static final Lock BITMAP_DRAWABLE_LOCK =
            MODELS_REQUIRING_BITMAP_LOCK.contains(Build.MODEL) ? new ReentrantLock() : new NoLock();

    private final float cropViewL;
    private final float cropViewT;
    private final float cropViewW;
    private final float cropViewH;

    private final float overrideW;
    private final float overrideH;
    public RequestBuilder<Bitmap> requestBuilder;

    //
    public CustomTransFormation(Context context, int id, float viewW, float viewH, RectF cropRectF,
                                float width, float height, boolean isCloud) {
        this.context = context;
//        this.filePath = FilePath;
        this.id = id;
        float cropX = cropRectF.left;
        float cropY = cropRectF.top;
        float cropW = cropRectF.width();
        float cropH = cropRectF.height();
        float viewRatio = viewW / viewH;
        //크롭이 정해져 있지 않다.
        //이미지가 뷰 보다 더 넓으면 세로에 가득 채워서 표시
        if (cropW <= 0 || cropH <= 0) {
            float imgRatio = width / height;
            if (imgRatio > viewRatio) {
                cropH = height;
                cropW = cropH * viewRatio;
            } else {
                cropW = width;
                cropH = cropW / viewRatio;
            }
            cropX = (width - cropW) / 2;
            cropY = (height - cropH) / 2;
        }// end 크롭이 정해져 있지 않다.
        float cropRatio = cropW / cropH;
        //세로에 꽉차도록
        if (viewRatio >= cropRatio) {
            cropViewH = viewH;
            cropViewW = viewH * cropRatio;
        }
        //가로에 꽉차도록
        else {
            cropViewW = viewW;
            cropViewH = viewW / cropRatio;
        }

        float cropViewRatio = cropViewW / cropW;

        cropViewL = cropX * cropViewRatio;
        cropViewT = cropY * cropViewRatio;

        float scale = cropViewW / cropW;
        float cropViewR = width * (scale) - (cropViewL + cropViewW);
        float cropViewB = height * (scale) - (cropViewT + cropViewH);

        overrideW = cropViewL + cropViewW + cropViewR;
        overrideH = cropViewT + cropViewH + cropViewB;
        Log.d("TAG", "");

        if (!isCloud) {
            requestBuilder = Glide.with(this.context).asBitmap().load(id).override((int) overrideW, (int) overrideH).signature(new ObjectKey(id));
            //파일 경로 읽을 시 파일 수정정보를 통해 캐시
            /*Cursor cursor = ContentResolverUtil.getImageCursor(this.context, filePath);
            String modified = "";
            if (cursor != null && cursor.moveToNext()) {
                modified = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED));
                requestBuilder = Glide.with(this.context).asBitmap().load(id).override((int) overrideW, (int) overrideH).signature(new ObjectKey(filePath + modified));
            } else {
                requestBuilder = Glide.with(this.context).asBitmap().load(id).override((int) overrideW, (int) overrideH).signature(new ObjectKey(System.currentTimeMillis()));
            }*/
        } else {
            requestBuilder = Glide.with(this.context).asBitmap().load(id).override((int) overrideW, (int) overrideH).signature(new ObjectKey(filePath));
        }
    }

    public interface BitmapCallback {
        void getBitmap(Bitmap bitmap);
    }

    //동기식 이미지 얻음
    public Bitmap getBitmapResult() {
        try {
            Bitmap bitmap = requestBuilder.submit().get();
            BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
            return transform_(bitmapPool, bitmap, (int) overrideW, (int) overrideH);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    //비 동기식 이미지 얻음
    public void getAsyncCallback(BitmapCallback bitmapCallback) {
        requestBuilder.into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                BitmapPool bitmapPool = Glide.get(context).getBitmapPool();
                Bitmap bitmap = transform_(bitmapPool, resource, (int) overrideW, (int) overrideH);
                bitmapCallback.getBitmap(bitmap);
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                bitmapCallback.getBitmap(null);
            }
        });
    }

    //자동 호출이 안된다.
    protected Bitmap transform_(@NonNull BitmapPool pool, @NonNull Bitmap inBitmap, int ivW, int ivH) {
        Log.d(TAG, filePath + "transform \t ivW:" + ivW + "\tivH" + ivH);
        Matrix m = new Matrix();
        //확대, 축소
        //x, y 축 이동
        m.postTranslate((int) -cropViewL, (int) -cropViewT);

        //비트맵 재사용
        Bitmap result = pool.get((int) cropViewW, (int) cropViewH, getNonNullConfig(inBitmap));
        //투명도 복사
        TransformationUtils.setAlpha(inBitmap, result);

        BITMAP_DRAWABLE_LOCK.lock();
        try {
            Canvas canvas = new Canvas(result);
            canvas.drawBitmap(inBitmap, m, DEFAULT_PAINT);
            clear(canvas);
        } finally {
            BITMAP_DRAWABLE_LOCK.unlock();
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CustomTransFormation;
    }

    @Override
    public int hashCode() {
        return ID.hashCode();
    }

    @NonNull
    private static Bitmap.Config getNonNullConfig(@NonNull Bitmap bitmap) {
        return bitmap.getConfig() != null ? bitmap.getConfig() : Bitmap.Config.ARGB_8888;
    }


    private static final class NoLock implements Lock {
        @Synthetic
        NoLock() {
        }

        @Override
        public void lock() {
            // do nothing
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            // do nothing
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, @NonNull TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void unlock() {
            // do nothing
        }

        @NonNull
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Should not be called");
        }
    }

    private static void clear(Canvas canvas) {
        canvas.setBitmap(null);
    }
}
