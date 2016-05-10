package tech.mustache.ocr.com.anroid_ocr.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import tech.mustache.ocr.com.anroid_ocr.R;
import tech.mustache.ocr.com.anroid_ocr.ocr.TessAsyncEngine;
import tech.mustache.ocr.com.anroid_ocr.ocr.TessEngine;
import tech.mustache.ocr.com.anroid_ocr.ui.focus.FocusView;
import tech.mustache.ocr.com.anroid_ocr.util.ScreenHelper;

/**
 * Created by cooper on 08.05.2016.
 */
public class CameraActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CAMERA_CODE = 0;

    private ImageButton mRecordBtn;
    private boolean mIsRecording = false;

    private TextureView mCameraView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
    };

    private Size mPreviewSize;
    private CaptureRequest.Builder mCaptureRequestBuilder;

    private String mCameraId;

    private HandlerThread mBackGroundHandlerThread;
    private Handler mBackgroundHandler;

    private CameraCaptureSession mPreviewCaptureSession;
    private CameraCaptureSession mRecordSession;

    private ImageReader mImageReader;
    private Activity activity = this;
    private Rect mFrameSize;
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image mImage = reader.acquireNextImage()) {
                if (frame % 30 == 0) {
                    ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes, 0, bytes.length);

                    new TessAsyncEngine().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                            activity,
                            bytes,
                            mFocusView.getmFocusView(),
                            mImageReader.getWidth(),
                            mImageReader.getHeight());
                }
            }
        }
    };

    private FocusView mFocusView;
    private TextView mResultText;
    private static long frame = 0l;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);

        mCameraView = (TextureView) findViewById(R.id.cameraTextureView);
        mRecordBtn = (ImageButton) findViewById(R.id.recordBtn);
        mRecordBtn.setOnClickListener(this);
        mFocusView = (FocusView) findViewById(R.id.focus_view);
        mResultText = (TextView) findViewById(R.id.resultText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackGroundThread();
        TessEngine.getInstance(getApplicationContext());
        if (mCameraView.isAvailable()) {
            setupCamera(mCameraView.getWidth(), mCameraView.getHeight());
            connectCamera();
        } else {
            mCameraView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        endRecord();
        closeCamera();
        stopBackgroundThread();
        mIsRecording = false;
        mRecordBtn.setImageResource(R.drawable.ic_rec);
        TessEngine.destroy(getApplicationContext());
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "Camera app cannot work without permissions",
                        Toast.LENGTH_SHORT)
                        .show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.recordBtn:
                if (mIsRecording) {
                    stopRecBtn();
                } else {
                    startRecBtn();
                }
                break;
            default:
                break;
        }
    }

    private void startRecBtn() {
        mIsRecording = true;
        mRecordBtn.setImageResource(R.drawable.ic_stop);
        startRecord();
    }

    private void stopRecBtn() {
        mIsRecording = false;
        mRecordBtn.setImageResource(R.drawable.ic_rec);
        endRecord();
        startPreview();
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                    // TODO change values
                    width = 1920;
                    height = 1080;
                    mPreviewSize = ScreenHelper.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                    // TODO here
                    mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 1);
                    mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
                    mFrameSize = new Rect(0, 0, mImageReader.getWidth(), mImageReader.getHeight());
                    mCameraId = cameraId;
                    return;
                }
            }
        } catch (CameraAccessException e) {
        }
    }

    private void connectCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
                } else {
                    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        Toast.makeText(this,
                                "App requires access to camera",
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
                }
            } else {
                cameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = mCameraView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            mPreviewCaptureSession = session;
                            try {
                                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Toast.makeText(getApplicationContext(),
                                    "Unable to setup camera preview", Toast.LENGTH_SHORT).show();
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
        }
    }

    private void startRecord() {
        SurfaceTexture surfaceTexture = mCameraView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_VIDEO_SNAPSHOT);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 90);
            try {
                mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                        new CameraCaptureSession.CaptureCallback() {
                            @Override
                            public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                                frame = frameNumber;
                                super.onCaptureStarted(session, request, timestamp, frameNumber);
                            }
                        },
                        null);
            } catch (CameraAccessException e) {
            }
        } catch (CameraAccessException e) {
        }
    }

    private void endRecord() {
        if (mRecordSession != null) {
            mRecordSession.close();
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
    }

    private void startBackGroundThread() {
        mBackGroundHandlerThread = new HandlerThread("CameraThread");
        mBackGroundHandlerThread.start();
        mBackgroundHandler = new Handler(mBackGroundHandlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackGroundHandlerThread.quitSafely();
        try {
            mBackGroundHandlerThread.join();
            mBackGroundHandlerThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
        }

    }
}