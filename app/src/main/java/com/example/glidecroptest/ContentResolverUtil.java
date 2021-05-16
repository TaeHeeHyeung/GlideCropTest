package com.example.glidecroptest;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

public class ContentResolverUtil {
    public static final String[] projection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.ImageColumns.ORIENTATION,
            MediaStore.Images.ImageColumns.MIME_TYPE,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.HEIGHT,
            MediaStore.Images.ImageColumns.WIDTH,
            MediaStore.Images.ImageColumns.SIZE,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
    };
    static public Cursor getImageCursor(Context context, String path) {
        return context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{path},
                MediaStore.Images.Media.DATE_ADDED + " desc ," + MediaStore.Images.Media.DATE_TAKEN + " desc ," + MediaStore.Images.Media._ID + " desc ");
    }
}
