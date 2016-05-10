package tech.mustache.ocr.com.anroid_ocr.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.googlecode.tesseract.android.TessBaseAPI;

import tech.mustache.ocr.com.anroid_ocr.util.TessDataManager;

public class TessEngine {

    private static TessEngine tessEngine;

    private TessBaseAPI tessBaseAPI;
    private String path;

    private TessEngine(Context context){
        TessDataManager.initTessTrainedData(context);
        this.tessBaseAPI = new TessBaseAPI();
        this.path = TessDataManager.getTesseractFolder();

        tessBaseAPI.init(path, "eng");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "1234567890@#$%^&()_+=|\\{}[]:\";<>/~`");
        tessBaseAPI.setPageSegMode(TessBaseAPI.OEM_DEFAULT);
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
    }

    public static void destroy(Context context) {
        getInstance(context).destroyEngine();
    }

    public void destroyEngine() {
        tessBaseAPI.end();
        tessBaseAPI = null;
        tessEngine = null;
        System.gc();
    }

    public void stopEngine() {
        tessBaseAPI.stop();
    }

    public static synchronized TessEngine getInstance(Context context) {
        if (tessEngine == null) {
            tessEngine = new TessEngine(context);
        }
        return tessEngine;
    }

    public String detectText(Bitmap bitmap, Rect rect) {
        try {
            tessBaseAPI.setImage(bitmap);
            tessBaseAPI.setRectangle(rect);
            return tessBaseAPI.getUTF8Text();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
