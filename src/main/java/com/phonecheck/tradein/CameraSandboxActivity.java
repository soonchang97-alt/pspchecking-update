package com.phonecheck.tradein;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CameraSandboxActivity extends Activity {
    public static final String EXTRA_RESULT = "camera_result";
    public static final String EXTRA_FACING = "camera_facing";
    public static final String FACING_BACK = "back";
    public static final String FACING_FRONT = "front";
    public static final String RESULT_VISIBLE = "visible";
    public static final String RESULT_FAILED = "failed";

    private static final int TEST_TIMEOUT_MS = 6000;
    private static final int MIN_GOOD_FRAMES = 3;
    private static final int MIN_AVERAGE_LUMA = 24;
    private static final int MIN_LUMA_RANGE = 18;

    private final Handler uiHandler = new Handler();
    private String requestedFacing = FACING_BACK;
    private boolean finished = false;
    private int goodFrameCount = 0;
    private int badFrameCount = 0;
    private TextView statusView;
    private HandlerThread cameraThread;
    private Camera activeCamera;
    private SurfaceTexture previewTexture;
    private int previewLumaBytes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> finishWith(RESULT_FAILED, "Camera test crashed"));
        requestedFacing = getIntent().getStringExtra(EXTRA_FACING);
        if (!FACING_FRONT.equals(requestedFacing)) requestedFacing = FACING_BACK;
        buildLayout();
        uiHandler.postDelayed(this::startHardwareTest, 400);
    }

    @Override
    protected void onDestroy() {
        closeCamera();
        uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(243, 247, 250));

        TextView title = new TextView(this);
        title.setText(FACING_FRONT.equals(requestedFacing)
                ? "Front camera hardware test / \u524d\u955c\u5934\u786c\u4ef6\u6d4b\u8bd5"
                : "Back camera hardware test / \u540e\u955c\u5934\u786c\u4ef6\u6d4b\u8bd5");
        title.setTextSize(22);
        title.setTextColor(Color.rgb(28, 38, 47));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        statusView = new TextView(this);
        statusView.setText("Testing camera hardware...\n\nThis page will try to open the camera and receive one preview frame. If it fails or times out, it will close and record abnormal.\n\n\u6b63\u5728\u6d4b\u8bd5\u76f8\u673a\u786c\u4ef6...\n\n\u6b64\u9875\u4f1a\u5c1d\u8bd5\u6253\u5f00\u76f8\u673a\u5e76\u6536\u5230\u4e00\u5e27\u9884\u89c8\u753b\u9762\u3002\u5982\u679c\u5931\u8d25\u6216\u8d85\u65f6\uff0c\u4f1a\u5173\u95ed\u5e76\u8bb0\u5f55\u5f02\u5e38\u3002");
        statusView.setTextSize(15);
        statusView.setTextColor(Color.rgb(84, 96, 108));
        statusView.setPadding(0, 18, 0, 18);
        root.addView(statusView, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button fail = new Button(this);
        fail.setText("Stop and mark abnormal / \u505c\u6b62\u5e76\u6807\u8bb0\u5f02\u5e38");
        fail.setAllCaps(false);
        fail.setOnClickListener(v -> finishWith(RESULT_FAILED, "User stopped camera test"));
        root.addView(fail, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);
    }

    private void startHardwareTest() {
        if (finished) return;
        if (android.os.Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            finishWith(RESULT_FAILED, "Camera permission missing");
            return;
        }
        uiHandler.postDelayed(() -> finishWith(RESULT_FAILED, "Camera hardware timeout"), TEST_TIMEOUT_MS);
        cameraThread = new HandlerThread("camera-hardware-test");
        cameraThread.start();
        new Handler(cameraThread.getLooper()).post(this::openCameraAndWaitForFrame);
    }

    private void openCameraAndWaitForFrame() {
        try {
            int cameraId = findCameraId();
            if (cameraId < 0) {
                finishWith(RESULT_FAILED, "Camera id not found");
                return;
            }
            previewTexture = new SurfaceTexture(12);
            activeCamera = Camera.open(cameraId);
            Camera.Parameters parameters = activeCamera.getParameters();
            Camera.Size size = smallestPreviewSize(parameters);
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height);
                previewLumaBytes = size.width * size.height;
            }
            activeCamera.setParameters(parameters);
            activeCamera.setPreviewTexture(previewTexture);
            activeCamera.setPreviewCallback((data, camera) -> handlePreviewFrame(data));
            activeCamera.startPreview();
            runOnUiThread(() -> statusView.setText("Camera opened. Waiting for preview frame...\n\n\u76f8\u673a\u5df2\u6253\u5f00\uff0c\u6b63\u5728\u7b49\u5f85\u9884\u89c8\u753b\u9762..."));
        } catch (Throwable t) {
            finishWith(RESULT_FAILED, "Camera open failed: " + t.getClass().getSimpleName());
        }
    }

    private int findCameraId() {
        try {
            int count = Camera.getNumberOfCameras();
            int wanted = FACING_FRONT.equals(requestedFacing)
                    ? Camera.CameraInfo.CAMERA_FACING_FRONT
                    : Camera.CameraInfo.CAMERA_FACING_BACK;
            Camera.CameraInfo info = new Camera.CameraInfo();
            for (int i = 0; i < count; i++) {
                Camera.getCameraInfo(i, info);
                if (info.facing == wanted) return i;
            }
            return count > 0 ? 0 : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private Camera.Size smallestPreviewSize(Camera.Parameters parameters) {
        try {
            Camera.Size best = null;
            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                if (best == null || size.width * size.height < best.width * best.height) best = size;
            }
            return best;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void handlePreviewFrame(byte[] data) {
        if (finished) return;
        if (isUsableFrame(data)) {
            goodFrameCount++;
            runOnUiThread(() -> statusView.setText("Camera frame quality check: " + goodFrameCount + "/" + MIN_GOOD_FRAMES
                    + "\n\n\u76f8\u673a\u753b\u9762\u68c0\u67e5\uff1a" + goodFrameCount + "/" + MIN_GOOD_FRAMES));
            if (goodFrameCount >= MIN_GOOD_FRAMES) {
                finishWith(RESULT_VISIBLE, "Usable camera frames received");
            }
        } else {
            badFrameCount++;
            if (badFrameCount >= 6) {
                finishWith(RESULT_FAILED, "Camera frames are black or abnormal");
            }
        }
    }

    private boolean isUsableFrame(byte[] data) {
        if (data == null || data.length < 64) return false;
        int min = 255;
        int max = 0;
        long sum = 0;
        int samples = 0;
        int limit = previewLumaBytes > 0 ? Math.min(previewLumaBytes, data.length) : Math.max(1, data.length * 2 / 3);
        int step = Math.max(1, limit / 160);
        for (int i = 0; i < limit; i += step) {
            int y = data[i] & 0xff;
            if (y < min) min = y;
            if (y > max) max = y;
            sum += y;
            samples++;
        }
        if (samples == 0) return false;
        int avg = (int) (sum / samples);
        return avg >= MIN_AVERAGE_LUMA && (max - min) >= MIN_LUMA_RANGE;
    }

    private synchronized boolean markFinished() {
        if (finished) return false;
        finished = true;
        return true;
    }

    private void finishWith(String result, String note) {
        if (!markFinished()) return;
        uiHandler.removeCallbacksAndMessages(null);
        runOnUiThread(() -> statusView.setText(note + "\n\nReturning to checker...\n\u6b63\u5728\u56de\u5230\u68c0\u6d4b App..."));
        closeCamera();
        Intent data = new Intent();
        data.putExtra(EXTRA_RESULT, result);
        data.putExtra(EXTRA_FACING, requestedFacing);
        setResult(Activity.RESULT_OK, data);
        finish();
        uiHandler.postDelayed(() -> Process.killProcess(Process.myPid()), 500);
    }

    private void closeCamera() {
        try {
            if (activeCamera != null) {
                activeCamera.setPreviewCallback(null);
                activeCamera.stopPreview();
                activeCamera.release();
            }
        } catch (Throwable ignored) {
        }
        activeCamera = null;
        previewLumaBytes = 0;
        try {
            if (previewTexture != null) previewTexture.release();
        } catch (Throwable ignored) {
        }
        previewTexture = null;
        try {
            if (cameraThread != null) cameraThread.quitSafely();
        } catch (Throwable ignored) {
        }
        cameraThread = null;
    }
}
