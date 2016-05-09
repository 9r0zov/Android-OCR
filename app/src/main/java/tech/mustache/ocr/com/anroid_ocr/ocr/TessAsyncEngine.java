package tech.mustache.ocr.com.anroid_ocr.ocr;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

import tech.mustache.ocr.com.anroid_ocr.R;

public class TessAsyncEngine extends AsyncTask<Object, Void, String> {

    private byte[] bytes;
    private Rect rect;
    private Activity context;

    @Override
    protected String doInBackground(Object... params) {
        try {
            context = (Activity) params[0];
            bytes = (byte[]) params[1];
            rect = (Rect) params[2];
            int width = (int) params[3];
            int height = (int) params[4];

            Bitmap bitmap = null;
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                YuvImage yuv = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
                Rect mFrameSize = new Rect(0, 0, width, height);
                yuv.compressToJpeg(mFrameSize, 80, out);

                if (out != null) {
                    byte[] bitmapBytes = out.toByteArray();
                    bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return TessEngine.getInstance(context).detectText(bitmap, rect);
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
}
