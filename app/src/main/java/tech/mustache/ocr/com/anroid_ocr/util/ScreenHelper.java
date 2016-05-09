package tech.mustache.ocr.com.anroid_ocr.util;

import android.content.Context;
import android.graphics.Point;
import android.util.Size;
import android.view.Display;
import android.view.TextureView;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by cooper on 08.05.2016.
 */
public class ScreenHelper {

    public static Size chooseOptimalSize(Size[] choices, int width, int height) {
        List<Size> bigEnough = new ArrayList<>();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * height / width
                    && option.getWidth() >= width
                    && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new SizeComparator());
        }
        // TODO change
        return choices[0];
    }

    public static Point minFocusViewSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return size;
    }

}
