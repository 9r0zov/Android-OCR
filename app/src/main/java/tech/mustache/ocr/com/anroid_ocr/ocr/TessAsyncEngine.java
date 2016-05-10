package tech.mustache.ocr.com.anroid_ocr.ocr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

import tech.mustache.ocr.com.anroid_ocr.R;

public class TessAsyncEngine extends AsyncTask<Object, Void, String> {

    private byte[] bytes;
    private Rect rect;
    private Activity context;

    private Bitmap mBitmap;

    @Override
    protected String doInBackground(Object... params) {
        try {
            context = (Activity) params[0];
            bytes = (byte[]) params[1];
            rect = (Rect) params[2];
            int width = (int) params[3];
            int height = (int) params[4];

            int[] convertYUV420_nv21toRGB8888 = convertYUV420_NV21toRGB8888(bytes, width, height);

            Bitmap bitmap = Bitmap.createBitmap(convertYUV420_nv21toRGB8888, width, height, Bitmap.Config.ARGB_8888);
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            mBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            return TessEngine.getInstance(context).detectText(mBitmap, rect);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result == null || context == null)
            return;

        TextView resultText = (TextView) ((Activity) context).findViewById(R.id.resultText);
        resultText.setText(result);

        super.onPostExecute(result);
    }

    /**
     * Converts YUV420 NV21 to RGB8888
     *
     * @param data byte array on YUV420 NV21 format.
     * @param width pixels width
     * @param height pixels height
     * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
     */
    public static int[] convertYUV420_NV21toRGB8888(byte [] data, int width, int height) {
        int p;
        int size = width*height;
        int[] pixels = new int[size];
        for(int i = 0; i < size; i++) {
            p = data[i] & 0xFF;
            pixels[i] = 0xff000000 | p<<16 | p<<8 | p;
        }

        return pixels;
    }
}
