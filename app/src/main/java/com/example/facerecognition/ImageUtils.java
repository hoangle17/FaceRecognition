package com.example.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    public static void saveBitmapAsJpeg(Bitmap bitmap, Context context, String filename) {
        int desiredWidth = 480;
        int desiredHeight = 480;
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, true);

        File directory = context.getFilesDir(); // Get internal storage directory
        File file = new File(directory, filename);
        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            // Compress the bitmap as a JPEG file with 90% quality
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);

            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getJpeg(Context context, String filename) {
        File directory = context.getFilesDir();
        File file = new File(directory, filename);
        if (file.exists()) {
            return file;
        }
        return null;
    }
}
