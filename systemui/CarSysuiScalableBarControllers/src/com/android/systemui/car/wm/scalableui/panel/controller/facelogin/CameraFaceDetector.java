package com.android.systemui.car.wm.scalableui.panel.controller.facelogin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.util.Collections;

/**
 * Handles Camera2 preview and basic facial detection.
 * Replace with MediaPipe Tasks Vision when AAR is available in the AOSP build.
 */
public class CameraFaceDetector {
    private static final String TAG = "CameraFaceDetector";
    private static final long FRAME_INTERVAL_MS = 500; // Run detection every 500ms

    private final Context mContext;
    private final TextureView mTextureView;
    private final FaceMatchCallback mCallback;

    private CameraManager mCameraManager;
    private String mCameraId;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Handler mMainHandler;

    private boolean mIsRunning = false;
    private boolean mFaceDetected = false;

    public interface FaceMatchCallback {
        void onFaceDetected();
    }

    public CameraFaceDetector(Context context, TextureView textureView, FaceMatchCallback callback) {
        mContext = context;
        mTextureView = textureView;
        mCallback = callback;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void start() {
        if (mIsRunning) return;
        mIsRunning = true;
        mFaceDetected = false;

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    stop();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            });
        }
    }

    public void stop() {
        mIsRunning = false;
        closeCamera();
        stopBackgroundThread();
    }

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    mCameraId = cameraId;
                    break;
                }
            }
            if (mCameraId == null) {
                mCameraId = mCameraManager.getCameraIdList()[0]; // Fallback
            }

            mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    mCameraDevice = camera;
                    createCameraPreviewSession();
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
            }, mBackgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            // Set default buffer size to 640x480 for preview
            texture.setDefaultBufferSize(640, 480);
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (mCameraDevice == null) return;
                    mCaptureSession = cameraCaptureSession;
                    try {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        mCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
                        // Start face detection loop
                        scheduleFaceDetection();
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to set repeating request", e);
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "Configuration failed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create preview session", e);
        }
    }

    private void scheduleFaceDetection() {
        if (!mIsRunning || mFaceDetected) return;
        mBackgroundHandler.postDelayed(() -> {
            detectFaceFromTexture();
            scheduleFaceDetection();
        }, FRAME_INTERVAL_MS);
    }

    private void detectFaceFromTexture() {
        if (!mIsRunning || mFaceDetected) return;
        
        Bitmap bitmap = mTextureView.getBitmap(320, 240); // Downscale for faster detection
        if (bitmap == null) return;

        // FaceDetector requires RGB_565 bitmap and even width/height
        Bitmap rgb565 = bitmap.copy(Bitmap.Config.RGB_565, true);
        if (rgb565 == null) return;

        FaceDetector detector = new FaceDetector(rgb565.getWidth(), rgb565.getHeight(), 1);
        FaceDetector.Face[] faces = new FaceDetector.Face[1];
        int count = detector.findFaces(rgb565, faces);

        if (count > 0 && faces[0] != null && faces[0].confidence() >= FaceDetector.Face.CONFIDENCE_THRESHOLD) {
            Log.d(TAG, "Face detected! Confidence: " + faces[0].confidence());
            mFaceDetected = true;
            mMainHandler.post(() -> mCallback.onFaceDetected());
        }

        rgb565.recycle();
        bitmap.recycle();
    }

    private void closeCamera() {
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while stopping background thread", e);
            }
        }
    }
}
