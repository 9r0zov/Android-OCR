package tech.mustache.ocr.com.anroid_ocr.ui.focus;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.v7.view.ContextThemeWrapper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import tech.mustache.ocr.com.anroid_ocr.R;
import tech.mustache.ocr.com.anroid_ocr.util.ScreenHelper;

/**
 * Created by cooper on 09.05.2016.
 */
public class FocusView extends View {

    private final Context context;

    private final int MIN_BOX_HEIGHT;
    private final int MIN_BOX_WIDTH;

    private final int MASK_COLOR;
    private final int FRAME_COLOR;
    private final int CORNERS_COLOR;

    private final int FRAME_COLOR_ON_DRAG;
    private final int CORNERS_COLOR_ON_DRAG;

    private final Paint mPaint;
    private final Point mDrawAreaSize;

    private Rect mFocusView;
    private OnTouchListener mOnTouchListener;

    private boolean mInMove;

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        this.mDrawAreaSize = ScreenHelper.minFocusViewSize(context);

        this.MIN_BOX_HEIGHT = mDrawAreaSize.y / 10;
        this.MIN_BOX_WIDTH = mDrawAreaSize.x / 3;

        this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        Resources resources = getResources();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Resources.Theme theme = new ContextThemeWrapper(context, R.style.AppTheme).getTheme();
            this.FRAME_COLOR = resources.getColor(R.color.focusViewBorder, theme);
            this.MASK_COLOR = resources.getColor(R.color.focusViewMask, theme);
            this.CORNERS_COLOR = resources.getColor(R.color.focusViewCorner, theme);
            this.FRAME_COLOR_ON_DRAG = resources.getColor(R.color.focusViewBorderOnDrag, theme);
            this.CORNERS_COLOR_ON_DRAG = resources.getColor(R.color.focusViewCornerOnDrag, theme);
        } else{
            this.FRAME_COLOR = resources.getColor(R.color.focusViewBorder);
            this.MASK_COLOR = resources.getColor(R.color.focusViewMask);
            this.CORNERS_COLOR = resources.getColor(R.color.focusViewCorner);
            this.FRAME_COLOR_ON_DRAG = resources.getColor(R.color.focusViewBorderOnDrag);
            this.CORNERS_COLOR_ON_DRAG = resources.getColor(R.color.focusViewCornerOnDrag);
        }

        this.setOnTouchListener(getOnTouchListener());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect frame = getFocusViewRect();

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        mPaint.setColor(MASK_COLOR);
        canvas.drawRect(0, 0, width, frame.top, mPaint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, mPaint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, mPaint);
        canvas.drawRect(0, frame.bottom + 1, width, height, mPaint);

        mPaint.setAlpha(0);
        mPaint.setStyle(Paint.Style.FILL);
        if (mInMove) {
            mPaint.setColor(FRAME_COLOR_ON_DRAG);
        } else {
            mPaint.setColor(FRAME_COLOR);
        }
        canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, mPaint);
        canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, mPaint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, mPaint);
        canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, mPaint);

        if (mInMove) {
            mPaint.setColor(CORNERS_COLOR_ON_DRAG);
        } else {
            mPaint.setColor(CORNERS_COLOR);
        }
        int cornerSize = 30;
        int cornerPadding = 5;
        canvas.drawCircle(frame.left - cornerPadding, frame.top - cornerPadding, cornerSize, mPaint);
        canvas.drawCircle(frame.right + cornerPadding, frame.top - cornerPadding, cornerSize, mPaint);
        canvas.drawCircle(frame.left - cornerPadding, frame.bottom + cornerPadding, cornerSize, mPaint);
        canvas.drawCircle(frame.right + cornerPadding, frame.bottom + cornerPadding, cornerSize, mPaint);
    }

    private OnTouchListener getOnTouchListener() {
        if (mOnTouchListener == null) {
            mOnTouchListener = getNewOnTouchListener();
        }
        return mOnTouchListener;
    }

    private OnTouchListener getNewOnTouchListener() {
        return new OnTouchListener() {

            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Rect rect = getFocusViewRect();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;
                        mInMove = false;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;
                        if (mInMove) {
                            mInMove = false;
                            v.invalidate();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        mInMove = true;
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();
                        try {
                            final int BUFFER = 75;
                            final int BIG_BUFFER = 90;
                            if (lastX >= 0) {
                                if (((currentX >= rect.left - BIG_BUFFER
                                        && currentX <= rect.left + BIG_BUFFER)
                                        || (lastX >= rect.left - BIG_BUFFER
                                        && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER
                                        && currentY >= rect.top - BIG_BUFFER)
                                        || (lastY <= rect.top + BIG_BUFFER
                                        && lastY >= rect.top - BIG_BUFFER))) {
                                    updateBoxRect(2 * (lastX - currentX),
                                            2 * (lastY - currentY));
                                } else if (((currentX >= rect.right - BIG_BUFFER
                                        && currentX <= rect.right + BIG_BUFFER)
                                        || (lastX >= rect.right - BIG_BUFFER
                                        && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER
                                        && currentY >= rect.top - BIG_BUFFER)
                                        || (lastY <= rect.top + BIG_BUFFER
                                        && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top right corner: adjust both top and right sides
                                    updateBoxRect(2 * (currentX - lastX),
                                            2 * (lastY - currentY));
                                } else if (((currentX >= rect.left - BIG_BUFFER
                                        && currentX <= rect.left + BIG_BUFFER)
                                        || (lastX >= rect.left - BIG_BUFFER
                                        && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER
                                        && currentY >= rect.bottom - BIG_BUFFER)
                                        || (lastY <= rect.bottom + BIG_BUFFER
                                        && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom left corner: adjust both bottom and left sides
                                    updateBoxRect(2 * (lastX - currentX),
                                            2 * (currentY - lastY));
                                } else if (((currentX >= rect.right - BIG_BUFFER
                                        && currentX <= rect.right + BIG_BUFFER)
                                        || (lastX >= rect.right - BIG_BUFFER
                                        && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER
                                        && currentY >= rect.bottom - BIG_BUFFER)
                                        || (lastY <= rect.bottom + BIG_BUFFER
                                        && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom right corner: adjust both bottom and right sides
                                    updateBoxRect(2 * (currentX - lastX),
                                            2 * (currentY - lastY));
                                } else if (((currentX >= rect.left - BUFFER
                                        && currentX <= rect.left + BUFFER)
                                        || (lastX >= rect.left - BUFFER
                                        && lastX <= rect.left + BUFFER))
                                        && ((currentY <= rect.bottom
                                        && currentY >= rect.top)
                                        || (lastY <= rect.bottom
                                        && lastY >= rect.top))) {
                                    // Adjusting left side: event falls within BUFFER pixels of
                                    // left side, and between top and bottom side limits
                                    updateBoxRect(2 * (lastX - currentX), 0);
                                } else if (((currentX >= rect.right - BUFFER
                                        && currentX <= rect.right + BUFFER)
                                        || (lastX >= rect.right - BUFFER
                                        && lastX <= rect.right + BUFFER))
                                        && ((currentY <= rect.bottom
                                        && currentY >= rect.top)
                                        || (lastY <= rect.bottom
                                        && lastY >= rect.top))) {
                                    // Adjusting right side: event falls within BUFFER pixels of
                                    // right side, and between top and bottom side limits
                                    updateBoxRect(2 * (currentX - lastX), 0);
                                } else if (((currentY <= rect.top + BUFFER
                                        && currentY >= rect.top - BUFFER)
                                        || (lastY <= rect.top + BUFFER
                                        && lastY >= rect.top - BUFFER))
                                        && ((currentX <= rect.right
                                        && currentX >= rect.left)
                                        || (lastX <= rect.right
                                        && lastX >= rect.left))) {
                                    // Adjusting top side: event falls within BUFFER pixels of
                                    // top side, and between left and right side limits
                                    updateBoxRect(0, 2 * (lastY - currentY));
                                } else if (((currentY <= rect.bottom + BUFFER
                                        && currentY >= rect.bottom - BUFFER)
                                        || (lastY <= rect.bottom + BUFFER
                                        && lastY >= rect.bottom - BUFFER))
                                        && ((currentX <= rect.right
                                        && currentX >= rect.left)
                                        || (lastX <= rect.right
                                        && lastX >= rect.left))) {
                                    updateBoxRect(0, 2 * (currentY - lastY));
                                }
                            }
                        } catch (NullPointerException e) {
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                }
                return false;
            }
        };
    }

    private Rect getFocusViewRect() {
        if (mFocusView == null) {
            int width = mDrawAreaSize.x * 6 / 8;
            int height = mDrawAreaSize.y / 9;

            width = width == 0
                    ? MIN_BOX_WIDTH
                    : width < MIN_BOX_WIDTH ? MIN_BOX_WIDTH : width;

            height = height == 0
                    ? MIN_BOX_HEIGHT
                    : height < MIN_BOX_HEIGHT ? MIN_BOX_HEIGHT : height;

            int left = (mDrawAreaSize.x - width) / 2;
            int top = (mDrawAreaSize.y - height) / 2;
            int right = left + width;
            int bottom = top + height;

            mFocusView = new Rect(left, top, right, bottom);
        }
        return mFocusView;
    }

    private void updateBoxRect(int dW, int dH) {
        int newWidth = (mFocusView.width() + dW > mDrawAreaSize.x - 4 || mFocusView.width() + dW < MIN_BOX_WIDTH)
                ? 0
                : mFocusView.width() + dW;

        int newHeight = (mFocusView.height() + dH > mDrawAreaSize.y - 4 || mFocusView.height() + dH < MIN_BOX_HEIGHT)
                ? 0
                : mFocusView.height() + dH;

        int leftOffset = (mDrawAreaSize.x - newWidth) / 2;
        int topOffset = (mDrawAreaSize.y - newHeight) / 2;

        if (newWidth < MIN_BOX_WIDTH || newHeight < MIN_BOX_HEIGHT)
            return;

        mFocusView = new Rect(leftOffset, topOffset, leftOffset + newWidth, topOffset + newHeight);
    }

    public Rect getmFocusView() {
        return mFocusView;
    }

    public void setmFocusView(Rect mFocusView) {
        this.mFocusView = mFocusView;
    }
}