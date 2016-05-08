package tech.mustache.ocr.com.anroid_ocr.util;

import android.util.Size;

import java.util.Comparator;

/**
 * Created by cooper on 08.05.2016.
 */
public class SizeComparator implements Comparator<Size> {
    @Override
    public int compare(Size lhs, Size rhs) {
        return Long.signum((long) lhs.getWidth() * lhs.getHeight() /
                (long) rhs.getWidth() * rhs.getHeight());
    }
}
