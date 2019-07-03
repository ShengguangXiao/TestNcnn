package com.example.testncnn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;

public class PhotoUtil {
    // get picture in photo
    public static void use_photo(Activity activity, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    // get photo from Uri
    public static String get_path_from_URI(Context context, Uri uri) {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor == null) {
            result = uri.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    // compress picture
    public static Bitmap getScaleBitmap(String filePath) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opt);

        int bmpWidth = opt.outWidth;
        int bmpHeight = opt.outHeight;

        int maxSize = 500;

        // compress picture with inSampleSize
        opt.inSampleSize = 1;
        while (true) {
            if (bmpWidth / opt.inSampleSize < maxSize || bmpHeight / opt.inSampleSize < maxSize) {
                break;
            }
            opt.inSampleSize *= 2;
        }
        opt.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, opt);
    }

    public static Bitmap getSizedBitmap(String filePath, int size) {
        //BitmapFactory.Options opt = new BitmapFactory.Options();
        //opt.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath);

        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888, true);

        if (rgba.getWidth() == rgba.getHeight()) {
            return Bitmap.createScaledBitmap(rgba, size, size, false);
        }else if (rgba.getHeight() > rgba.getWidth()) {
            int new_height = size;
            int new_width = (int) (size * 1.f * rgba.getWidth() / rgba.getHeight() + 0.5f);
            Bitmap bitmapResized = Bitmap.createScaledBitmap(rgba, new_width, new_height, false);

            Bitmap bmpResult = Bitmap.createBitmap(size, size, rgba.getConfig());
            Canvas canvas = new Canvas(bmpResult);
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(bitmapResized, (size - new_width) / 2, 0, null);
            return bmpResult;
        }else {
            int new_width = size;
            int new_height = (int) (size * 1.f * rgba.getHeight() / rgba.getWidth() + 0.5f);
            Bitmap bitmapResized = Bitmap.createScaledBitmap(rgba, new_width, new_height, false);

            Bitmap bmpResult = Bitmap.createBitmap(size, size, rgba.getConfig());
            Canvas canvas = new Canvas(bmpResult);
            canvas.drawColor(Color.BLACK);
            canvas.drawBitmap(bitmapResized, 0, (size - new_height) / 2, null);
            return bmpResult;
        }
    }
}


