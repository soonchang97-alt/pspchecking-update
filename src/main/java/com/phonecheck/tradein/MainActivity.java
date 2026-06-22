package com.phonecheck.tradein;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.hardware.biometrics.BiometricPrompt;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.graphics.pdf.PdfDocument;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.biometrics.BiometricManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.fingerprint.FingerprintManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.net.ConnectivityManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.provider.MediaStore;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Range;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;

public class MainActivity extends Activity implements SensorEventListener {
    private static final int GREEN = Color.rgb(22, 132, 91);
    private static final int RED = Color.rgb(191, 52, 52);
    private static final int AMBER = Color.rgb(154, 107, 16);
    private static final int INK = Color.rgb(23, 32, 42);
    private static final int MUTED = Color.rgb(97, 112, 128);
    private static final int BG = Color.rgb(243, 247, 250);
    private static final int SURFACE = Color.WHITE;
    private static final int SOFT_GREEN = Color.rgb(232, 247, 239);
    private static final int SOFT_AMBER = Color.rgb(255, 246, 220);
    private static final int SOFT_RED = Color.rgb(253, 235, 235);
    private static final String APP_VERSION_LABEL = "V93 / 9.3-rear-lens-manual-check";
    private static final int APP_VERSION_CODE = 93;
    private static final String PREFS = "phone_check_developer";
    private static final String PREF_UPDATE_URL = "update_url";
    private static final String PREF_UPLOAD_URL = "upload_url";
    private static final String PREF_AUTO_UPLOAD = "auto_upload";
    private static final String DEFAULT_UPDATE_URL = "https://pspchecking.netlify.app/version.json";
    private static final String DEFAULT_UPLOAD_URL = "https://script.google.com/macros/s/AKfycbzb7DXtLn-OIVyts82xfPXaua7s4-dS5eYlefBrtDdKkfdsqBVSAk75Y0aIQOMoZKQe/exec";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Result> results = new HashMap<>();
    private final Map<String, String[]> text = new HashMap<>();

    private String lang = "en";
    private LinearLayout root;
    private Spinner ramSpinner;
    private Spinner romSpinner;
    private Spinner overallConditionSpinner;
    private Spinner screenScratchSpinner;
    private Spinner backCoverSpinner;
    private Spinner cameraOpenSpinner;
    private Spinner ultraWideSpinner;
    private Spinner zoomSpinner;
    private Spinner cameraSpotSpinner;
    private Spinner cameraFogSpinner;
    private Spinner cameraFocusSpinner;
    private int basePrice = 1200;
    private int deduction = 0;
    private SensorManager sensorManager;
    private TextView sensorStatusView;
    private boolean volumeUpPressed = false;
    private boolean volumeDownPressed = false;
    private TextView volumeUpStatusView;
    private TextView volumeDownStatusView;
    private TextView volumeSummaryView;
    private boolean volumeContinueShown = false;
    private TextView micStatusView;
    private TextView speakerStatusView;
    private TextView cameraHardwareStatusView;
    private boolean backCameraChecked = false;
    private boolean frontCameraChecked = false;
    private boolean backCameraPassed = false;
    private boolean frontCameraPassed = false;
    private String pendingCameraFacing = "";
    private Runnable cameraWatchdogRunnable;
    private android.hardware.Camera activeLegacyCamera;
    private TextView wifiStatusView;
    private TextView bluetoothStatusView;
    private TextView locationStatusView;
    private TextView nfcStatusView;
    private boolean nfcTagRead = false;
    private TextView simStatusView;
    private TextView biometricStatusView;
    private boolean biometricPromptPassed = false;
    private String biometricPassNote = "";
    private String biometricFailNote = "";
    private boolean bluetoothContinueShown = false;
    private boolean pendingWifiAutoScan = false;
    private boolean pendingBluetoothAutoScan = false;
    private boolean pendingLocationAutoCheck = false;
    private int wifiAutoScanAttempts = 0;
    private int bluetoothAutoScanAttempts = 0;
    private int locationAutoCheckAttempts = 0;
    private boolean locationContinueShown = false;
    private boolean motionLeftDone = false;
    private boolean motionRightDone = false;
    private boolean motionUpDone = false;
    private boolean motionDownDone = false;
    private TextView proximityStatusView;
    private boolean proximityPassed = false;
    private float proximityMaxRange = 0f;
    private float lightBaselineLux = -1f;
    private float proximityBaselineFar = -1f;
    private long proximityCalibrateUntilMs = 0L;
    private int proximityNearCount = 0;
    private int lightDarkCount = 0;
    private boolean touchTestActive = false;
    private TouchPad activeTouchPad;
    private volatile boolean micSampling = false;
    private int currentTestStep = 0;
    private boolean fullScreenTestActive = false;
    private int fullScreenTestColor = Color.BLACK;
    private int developerTapCount = 0;
    private boolean developerUnlocked = false;
    private TextView developerStatusView;
    private long lastAutoUploadMs = 0L;
    private boolean uploadInProgress = false;
    private boolean internalHardwareMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        installCrashLogger();
        seedText();
        seedDeveloperText();
        resetSessionState();
        requestBasicPermissions();
        showHome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
        closeCameraPreview();
        disableNfcForegroundDispatch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pendingWifiAutoScan && currentTestStep == 1 && wifiStatusView != null) {
            wifiAutoScanAttempts = 0;
            handler.postDelayed(() -> runWifiAutoScanAttempt(), 900);
        }
        if (pendingBluetoothAutoScan && currentTestStep == 2 && bluetoothStatusView != null) {
            bluetoothAutoScanAttempts = 0;
            handler.postDelayed(() -> runBluetoothAutoScanAttempt(), 900);
        }
        if (pendingLocationAutoCheck && currentTestStep == 3 && locationStatusView != null) {
            locationAutoCheckAttempts = 0;
            handler.postDelayed(() -> runLocationAutoCheckAttempt(), 900);
        }
        if (currentTestStep == 6 && nfcStatusView != null) {
            enableNfcForegroundDispatch();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (isLauncherIntent(intent)) {
            resetSessionState();
            showHome();
            return;
        }
        handleNfcIntent(intent);
    }

    private boolean isLauncherIntent(Intent intent) {
        return intent != null
                && Intent.ACTION_MAIN.equals(intent.getAction())
                && intent.hasCategory(Intent.CATEGORY_LAUNCHER);
    }

    private void resetSessionState() {
        results.clear();
        micSampling = false;
        disableNfcForegroundDispatch();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        ramSpinner = null;
        romSpinner = null;
        overallConditionSpinner = null;
        screenScratchSpinner = null;
        backCoverSpinner = null;
        cameraOpenSpinner = null;
        ultraWideSpinner = null;
        zoomSpinner = null;
        cameraSpotSpinner = null;
        cameraFogSpinner = null;
        cameraFocusSpinner = null;
        backCameraChecked = false;
        frontCameraChecked = false;
        backCameraPassed = false;
        frontCameraPassed = false;
        pendingCameraFacing = "";
        basePrice = 1200;
        deduction = 0;
        sensorStatusView = null;
        volumeUpPressed = false;
        volumeDownPressed = false;
        volumeUpStatusView = null;
        volumeDownStatusView = null;
        volumeSummaryView = null;
        volumeContinueShown = false;
        micStatusView = null;
        wifiStatusView = null;
        bluetoothStatusView = null;
        locationStatusView = null;
        nfcStatusView = null;
        nfcTagRead = false;
        simStatusView = null;
        biometricStatusView = null;
        biometricPromptPassed = false;
        biometricPassNote = "";
        biometricFailNote = "";
        bluetoothContinueShown = false;
        pendingWifiAutoScan = false;
        pendingBluetoothAutoScan = false;
        pendingLocationAutoCheck = false;
        wifiAutoScanAttempts = 0;
        bluetoothAutoScanAttempts = 0;
        locationAutoCheckAttempts = 0;
        locationContinueShown = false;
        motionLeftDone = false;
        motionRightDone = false;
        motionUpDone = false;
        motionDownDone = false;
        proximityStatusView = null;
        proximityPassed = false;
        proximityMaxRange = 0f;
        lightBaselineLux = -1f;
        proximityBaselineFar = -1f;
        proximityCalibrateUntilMs = 0L;
        proximityNearCount = 0;
        lightDarkCount = 0;
        touchTestActive = false;
        activeTouchPad = null;
        currentTestStep = 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (touchTestActive && (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            finishTouchTest();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            volumeUpPressed = true;
            updateButtonResult();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            volumeDownPressed = true;
            updateButtonResult();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            handleProximityChanged(event);
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            handleLightChanged(event);
            return;
        }
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        if (currentTestStep != 4 || sensorStatusView == null) return;
        float x = event.values[0];
        float y = event.values[1];
        if (x > 5.0f) motionLeftDone = true;
        if (x < -5.0f) motionRightDone = true;
        if (y > 5.0f) motionDownDone = true;
        if (y < -5.0f) motionUpDone = true;
        updateMotionSensorStatus(x, y);
        if (motionLeftDone && motionRightDone && motionUpDone && motionDownDone) {
            put("sensors", Status.PASS, t("sensorPassDetailed"));
            if (sensorManager != null) sensorManager.unregisterListener(this);
            nextTestStep();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == 11 || requestCode == 14) && currentTestStep == 2 && bluetoothStatusView != null && pendingBluetoothAutoScan) {
            handler.postDelayed(() -> runBluetoothAutoScanAttempt(), 700);
        }
        if ((requestCode == 11 || requestCode == 14) && currentTestStep == 2 && bluetoothStatusView != null && !pendingBluetoothAutoScan) {
            handler.postDelayed(() -> runBluetoothFunctionTest(), 700);
        }
        if ((requestCode == 13 || requestCode == 15) && currentTestStep == 1 && wifiStatusView != null) {
            if (pendingWifiAutoScan) {
                handler.postDelayed(() -> runWifiAutoScanAttempt(), 700);
            } else {
                handler.postDelayed(() -> runWifiScanTest(), 700);
            }
        }
        if (requestCode == 12 && currentTestStep == 3 && locationStatusView != null) {
            if (pendingLocationAutoCheck) {
                handler.postDelayed(() -> runLocationAutoCheckAttempt(), 700);
            } else {
                handler.postDelayed(() -> runLocationFunctionTest(), 700);
            }
        }
        if (requestCode == 16 && currentTestStep == 10 && micStatusView != null) {
            handler.postDelayed(() -> startMicrophoneSample(), 500);
        }
        if (requestCode == 17 && currentTestStep == 11 && speakerStatusView != null) {
            handler.postDelayed(() -> runSpeakerTest(), 500);
        }
        if (requestCode == 18 && currentTestStep == 12 && cameraHardwareStatusView != null) {
            handler.postDelayed(() -> showCameraDetailTest(), 500);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 19 && currentTestStep == 12) {
            if (cameraWatchdogRunnable != null) {
                handler.removeCallbacks(cameraWatchdogRunnable);
                cameraWatchdogRunnable = null;
            }
            String facing = pendingCameraFacing;
            if (facing == null || facing.length() == 0) return;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String result = data.getStringExtra(CameraSandboxActivity.EXTRA_RESULT);
                String returnedFacing = data.getStringExtra(CameraSandboxActivity.EXTRA_FACING);
                if (returnedFacing != null && returnedFacing.length() > 0) facing = returnedFacing;
                if (CameraSandboxActivity.RESULT_VISIBLE.equals(result)) {
                    setCameraSideResult(facing, true);
                } else {
                    setCameraSideResult(facing, false);
                }
            } else {
                setCameraSideResult(facing, false);
            }
            pendingCameraFacing = "";
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && fullScreenTestActive) {
            applyFullScreenFlags();
        }
    }

    private void requestBasicPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_PHONE_STATE
            }, 10);
        }
    }

    private void baseScreen(String title) {
        touchTestActive = false;
        activeTouchPad = null;
        closeCameraPreview();
        exitFullScreenTestMode();
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(24));
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(10), dp(12), dp(10));
        top.setBackgroundResource(getResources().getIdentifier("card_bg", "drawable", getPackageName()));
        top.setElevation(dp(1));
        LinearLayout.LayoutParams topParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        topParams.setMargins(0, 0, 0, dp(14));
        top.setLayoutParams(topParams);

        TextView h1 = new TextView(this);
        h1.setText(title);
        h1.setTextColor(INK);
        h1.setTextSize(21);
        h1.setTypeface(null, 1);
        top.addView(h1, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button home = new Button(this);
        home.setText(t("homeButton"));
        home.setTextSize(12);
        home.setAllCaps(false);
        home.setTextColor(INK);
        home.setBackgroundResource(getResources().getIdentifier("secondary_button", "drawable", getPackageName()));
        home.setOnClickListener(v -> showHome());
        top.addView(home);

        if (internalHardwareMode
                && !title.equals(t("internalHardwareTest"))
                && !title.equals(t("developerConsole"))) {
            Button internalBack = new Button(this);
            internalBack.setText(t("backInternalShort"));
            internalBack.setTextSize(12);
            internalBack.setAllCaps(false);
            internalBack.setTextColor(INK);
            internalBack.setBackgroundResource(getResources().getIdentifier("secondary_button", "drawable", getPackageName()));
            internalBack.setOnClickListener(v -> showInternalHardwareTestPanel());
            top.addView(internalBack);
        }

        top.addView(langButton("EN", "en"));
        top.addView(langButton("BM", "ms"));
        top.addView(langButton("华", "zh"));
        root.addView(top);
        setContentView(scroll);
    }

    private Button langButton(String label, String code) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setTextColor(code.equals(lang) ? Color.WHITE : INK);
        button.setBackgroundColor(code.equals(lang) ? GREEN : SURFACE);
        button.setOnClickListener(v -> {
            lang = code;
            showHome();
        });
        return button;
    }

    private void showHome() {
        internalHardwareMode = false;
        baseScreen(t("app"));
        LinearLayout card = card();
        TextView versionRow = row(t("version"), APP_VERSION_LABEL);
        versionRow.setOnClickListener(v -> unlockDeveloperTap());
        card.addView(versionRow);
        card.addView(title(t("homeTitle")));
        card.addView(body(t("homeBody")));
        Button start = primary(t("start"));
        start.setOnClickListener(v -> showDevice());
        card.addView(start);
            Button developer = secondary(t("developerConsole"));
        developer.setOnClickListener(v -> showDeveloperConsole());
        card.addView(developer);
        root.addView(card);

        LinearLayout info = card();
        info.addView(title(t("autoTitle")));
        info.addView(body(t("autoBody")));
        root.addView(info);
    }

    private void unlockDeveloperTap() {
        developerTapCount++;
        if (developerTapCount >= 5) {
            developerUnlocked = true;
            developerTapCount = 0;
            Toast.makeText(this, t("developerUnlocked"), Toast.LENGTH_SHORT).show();
            showHome();
        } else {
            Toast.makeText(this, "Developer " + developerTapCount + "/5", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDevice() {
        baseScreen(t("device"));
        LinearLayout card = card();
        card.addView(row(t("brand"), clean(Build.MANUFACTURER)));
        card.addView(row(t("model"), clean(Build.MODEL)));
        card.addView(row("Android", Build.VERSION.RELEASE));
        card.addView(row(t("ramDetected"), estimateRamGb() + " GB"));
        card.addView(row(t("storageDetected"), estimateRomGb() + " GB"));

        card.addView(label(t("confirmRam")));
        ramSpinner = spinner(new String[]{"4 GB", "6 GB", "8 GB", "12 GB", "16 GB"});
        selectSpinnerValue(ramSpinner, estimateRamGb() + " GB");
        card.addView(ramSpinner);

        card.addView(label(t("confirmRom")));
        romSpinner = spinner(new String[]{"64 GB", "128 GB", "256 GB", "512 GB", "1024 GB"});
        selectSpinnerValue(romSpinner, estimateRomGb() + " GB");
        card.addView(romSpinner);
        root.addView(card);

        Button next = primary(t("continue"));
        next.setOnClickListener(v -> {
            currentTestStep = 0;
            showTests();
        });
        root.addView(next);
    }

    private void showTests() {
        showTestStep();
    }

    private void showTestStep() {
        if (currentTestStep == 0) {
            showAutoTestStep();
            return;
        }
        if (currentTestStep == 1) {
            showWifiTest();
            return;
        }
        if (currentTestStep == 2) {
            showBluetoothTest();
            return;
        }
        if (currentTestStep == 3) {
            showLocationTest();
            return;
        }
        if (currentTestStep == 4) {
            showMotionSensorTest();
            return;
        }
        if (currentTestStep == 5) {
            showProximitySensorTest();
            return;
        }
        if (currentTestStep == 6) {
            showNfcTest();
            return;
        }
        if (currentTestStep == 7) {
            showSimSlotTest();
            return;
        }
        if (currentTestStep == 8) {
            showTelcoLockTest();
            return;
        }
        if (currentTestStep == 9) {
            showBiometricTest();
            return;
        }
        if (currentTestStep == 10) {
            showMicrophoneTest();
            return;
        }
        if (currentTestStep == 11) {
            showSpeakerTest();
            return;
        }
        if (currentTestStep == 12) {
            showCameraDetailTest();
            return;
        }
        if (currentTestStep == 13) {
            showTouchTest();
            return;
        }
        if (currentTestStep == 14) {
            showScreenTest(0);
            return;
        }
        if (currentTestStep == 15) {
            showButtonTest();
            return;
        }
        if (currentTestStep == 16) {
            showConditionForm();
            return;
        }
        matchPrice();
        calculateDeduction();
        showEstimate();
    }

    private void nextTestStep() {
        currentTestStep++;
        showTestStep();
    }

    private void nextTestStepIfStillOn(int expectedStep) {
        if (currentTestStep == expectedStep) {
            nextTestStep();
        }
    }

    private void showAutoTestStep() {
        baseScreen(t("tests"));
        LinearLayout card = card();
        card.addView(title(t("stepAutoTitle")));
        card.addView(body(t("stepAutoBody")));
        card.addView(testLine("camera", t("camera")));
        card.addView(testLine("flash", t("flash")));
        card.addView(testLine("vibration", t("vibration")));
        card.addView(testLine("wifi", t("wifi")));
        card.addView(testLine("bluetooth", t("bluetooth")));
        card.addView(testLine("gps", t("gps")));
        card.addView(testLine("battery", t("battery")));
        card.addView(testLine("sensors", t("sensors")));
        card.addView(testLine("proximity", t("proximity")));
        card.addView(testLine("nfc", t("nfc")));
        card.addView(testLine("simSlot", t("simSlot")));
        card.addView(testLine("telcoLock", t("telcoLock")));
        card.addView(testLine("biometric", t("biometric")));
        card.addView(testLine("touch", t("touch")));
        card.addView(testLine("screen", t("screen")));
        card.addView(testLine("buttons", t("buttons")));
        root.addView(card);

        Button auto = primary(t("runAuto"));
        auto.setOnClickListener(v -> runAutoTests());
        root.addView(auto);
    }

    private void runAutoTests() {
        put("camera", cameraStatus(), cameraNote());
        put("flash", flashStatus(), flashNote());
        put("wifi", Status.REVIEW, t("wifiNeedsConnectTest"));
        put("bluetooth", Status.REVIEW, t("btNeedsFunctionTest"));
        put("gps", Status.REVIEW, t("gpsNeedsFunctionTest"));
        put("battery", batteryOk() ? Status.PASS : Status.REVIEW, batteryStatus());
        put("vibration", runVibrationTest() ? Status.PASS : Status.REVIEW, t("vibrationNote"));
        put("speaker", Status.REVIEW, t("speakerNeedsManual"));
        put("microphone", Status.REVIEW, t("micNeedsManual"));
        put("sensors", Status.REVIEW, t("sensorNeedsDirectionTest"));
        put("proximity", Status.REVIEW, t("proximityWaiting"));
        put("nfc", nfcStatus(), nfcNote());
        put("simSlot", simSlotStatus(), simSlotNote());
        put("telcoLock", telcoLockStatus(), telcoLockNote());
        put("biometric", biometricStatus(), biometricNote());
        nextTestStep();
    }

    private void showWifiTest() {
        baseScreen(t("wifi"));
        LinearLayout card = card();
        card.addView(title(t("wifiTestTitle")));
        card.addView(body(t("wifiTestGuide")));
        wifiStatusView = body(wifiDiagnosticNote());
        card.addView(wifiStatusView);
        root.addView(card);

        Button openWifi = primary(t("openWifiSettings"));
        openWifi.setOnClickListener(v -> openWifiPanel());
        root.addView(openWifi);

        Button check = secondary(t("checkWifiNow"));
        check.setOnClickListener(v -> runWifiScanTest());
        root.addView(check);

        Button fail = secondary(t("markWifiFail"));
        fail.setOnClickListener(v -> {
            put("wifi", Status.FAIL, t("wifiUserFail"));
            nextTestStep();
        });
        root.addView(fail);
    }

    private void openWifiPanel() {
        pendingWifiAutoScan = true;
        wifiAutoScanAttempts = 0;
        if (wifiStatusView != null) wifiStatusView.setText(t("wifiReturnAuto"));
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= 29) {
                intent = new Intent(Settings.Panel.ACTION_WIFI);
            } else {
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            }
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }
        handler.postDelayed(() -> runWifiAutoScanAttempt(), 1800);
    }

    private void runWifiScanTest() {
        if (!hasWifiScanPermission()) {
            pendingWifiAutoScan = false;
            requestWifiScanPermission();
            if (wifiStatusView != null) wifiStatusView.setText(t("wifiPermissionNeeded"));
            return;
        }
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        boolean scanStarted = false;
        if (manager != null) {
            try {
                scanStarted = manager.startScan();
            } catch (Exception ignored) {
            }
        }
        if (wifiStatusView != null) wifiStatusView.setText(t("wifiChecking"));
        final boolean finalScanStarted = scanStarted;
        handler.postDelayed(() -> {
            Status status = wifiScanStatus(finalScanStarted);
            String note = wifiDiagnosticNote(finalScanStarted);
            put("wifi", status, note);
            if (wifiStatusView != null) wifiStatusView.setText(note);
            if (status == Status.PASS) {
                nextTestStepIfStillOn(1);
            } else {
                Toast.makeText(this, t("wifiNotPassedYet"), Toast.LENGTH_LONG).show();
            }
        }, 1300);
    }

    private void runWifiAutoScanAttempt() {
        if (!pendingWifiAutoScan || currentTestStep != 1 || wifiStatusView == null) return;
        wifiAutoScanAttempts++;
        wifiStatusView.setText(t("wifiAutoTrying") + " " + wifiAutoScanAttempts + "/8");
        if (!hasWifiScanPermission()) {
            requestWifiScanPermission();
            return;
        }
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        boolean scanStarted = false;
        if (manager != null) {
            try {
                scanStarted = manager.startScan();
            } catch (Exception ignored) {
            }
        }
        final boolean finalScanStarted = scanStarted;
        handler.postDelayed(() -> {
            Status status = wifiScanStatus(finalScanStarted);
            String note = wifiDiagnosticNote(finalScanStarted);
            put("wifi", status, note);
            if (wifiStatusView != null) wifiStatusView.setText(note);
            if (status == Status.PASS) {
                pendingWifiAutoScan = false;
                nextTestStepIfStillOn(1);
                return;
            }
            if (wifiAutoScanAttempts < 8) {
                handler.postDelayed(() -> runWifiAutoScanAttempt(), 1400);
            } else {
                pendingWifiAutoScan = false;
                Toast.makeText(this, t("wifiNotPassedYet"), Toast.LENGTH_LONG).show();
            }
        }, 1100);
    }

    private void showBluetoothTest() {
        baseScreen(t("bluetooth"));
        pendingBluetoothAutoScan = false;
        bluetoothContinueShown = false;
        LinearLayout card = card();
        card.addView(title(t("btTestTitle")));
        card.addView(body(t("btTestGuide")));
        bluetoothStatusView = body(bluetoothDiagnosticNote(false));
        card.addView(bluetoothStatusView);
        root.addView(card);

        Button openBt = primary(t("openBtSettings"));
        openBt.setOnClickListener(v -> openBluetoothSettings());
        root.addView(openBt);

        Button check = secondary(t("checkBtNow"));
        check.setOnClickListener(v -> runBluetoothFunctionTest());
        root.addView(check);

        Button fail = secondary(t("markBtFail"));
        fail.setOnClickListener(v -> {
            put("bluetooth", Status.FAIL, t("btUserFail"));
            nextTestStep();
        });
        root.addView(fail);
    }

    private void openBluetoothSettings() {
        pendingBluetoothAutoScan = true;
        bluetoothAutoScanAttempts = 0;
        if (bluetoothStatusView != null) bluetoothStatusView.setText(t("btReturnAuto"));
        try {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
        handler.postDelayed(() -> runBluetoothAutoScanAttempt(), 1800);
    }

    private void runBluetoothFunctionTest() {
        if (bluetoothStatusView != null) bluetoothStatusView.setText(t("btChecking"));
        boolean scanStarted = startBluetoothDiscovery();
        final boolean finalScanStarted = scanStarted;
        handler.postDelayed(() -> {
            Status status = bluetoothFunctionStatus(finalScanStarted);
            String note = bluetoothDiagnosticNote(finalScanStarted);
            put("bluetooth", status, note);
            if (bluetoothStatusView != null) bluetoothStatusView.setText(note);
            if (status == Status.PASS) {
                pendingBluetoothAutoScan = false;
                showBluetoothContinue();
            } else {
                Toast.makeText(this, t("btNotPassedYet"), Toast.LENGTH_LONG).show();
            }
        }, 1600);
    }

    private void runBluetoothAutoScanAttempt() {
        if (!pendingBluetoothAutoScan || currentTestStep != 2 || bluetoothStatusView == null) return;
        bluetoothAutoScanAttempts++;
        bluetoothStatusView.setText(t("btAutoTrying") + " " + bluetoothAutoScanAttempts + "/8");
        boolean scanStarted = startBluetoothDiscovery();
        final boolean finalScanStarted = scanStarted;
        handler.postDelayed(() -> {
            Status status = bluetoothFunctionStatus(finalScanStarted);
            String note = bluetoothDiagnosticNote(finalScanStarted);
            put("bluetooth", status, note);
            if (bluetoothStatusView != null) bluetoothStatusView.setText(note);
            if (status == Status.PASS) {
                pendingBluetoothAutoScan = false;
                showBluetoothContinue();
                return;
            }
            if (bluetoothAutoScanAttempts < 8) {
                handler.postDelayed(() -> runBluetoothAutoScanAttempt(), 1200);
            } else {
                pendingBluetoothAutoScan = false;
                Toast.makeText(this, t("btNotPassedYet"), Toast.LENGTH_LONG).show();
            }
        }, 900);
    }

    private boolean startBluetoothDiscovery() {
        try {
            if (!hasBluetoothScanPermission()) {
                requestBluetoothScanPermission();
                return false;
            }
            if (Build.VERSION.SDK_INT >= 31
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                }, 14);
                return false;
            }
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                adapter.cancelDiscovery();
                return adapter.startDiscovery();
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void showBluetoothContinue() {
        if (currentTestStep == 2 && root != null && !bluetoothContinueShown) {
            bluetoothContinueShown = true;
            Button next = primary(t("bluetoothContinue"));
            next.setOnClickListener(v -> nextTestStepIfStillOn(2));
            root.addView(next);
        }
    }

    private void showLocationTest() {
        baseScreen(t("gps"));
        locationContinueShown = false;
        pendingLocationAutoCheck = false;
        LinearLayout card = card();
        card.addView(title(t("gpsTestTitle")));
        card.addView(body(t("gpsTestGuide")));
        locationStatusView = body(locationDiagnosticNote());
        card.addView(locationStatusView);
        root.addView(card);

        Button openLocation = primary(t("openLocationSettings"));
        openLocation.setOnClickListener(v -> openLocationSettings());
        root.addView(openLocation);

        Button check = secondary(t("checkLocationNow"));
        check.setOnClickListener(v -> runLocationFunctionTest());
        root.addView(check);

        Button fail = secondary(t("markGpsFail"));
        fail.setOnClickListener(v -> {
            put("gps", Status.FAIL, t("gpsUserFail"));
            nextTestStep();
        });
        root.addView(fail);
    }

    private void openLocationSettings() {
        pendingLocationAutoCheck = true;
        locationAutoCheckAttempts = 0;
        if (locationStatusView != null) locationStatusView.setText(t("gpsReturnAuto"));
        try {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
        handler.postDelayed(() -> runLocationAutoCheckAttempt(), 1800);
    }

    private void runLocationFunctionTest() {
        if (Build.VERSION.SDK_INT >= 23 && !hasLocationPermission()) {
            put("gps", Status.REVIEW, t("gpsPermission"));
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 12);
            if (locationStatusView != null) locationStatusView.setText(t("gpsPermission"));
            return;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null || !isAnyLocationProviderEnabled(manager)) {
            put("gps", Status.REVIEW, t("gpsOpenRequired"));
            if (locationStatusView != null) locationStatusView.setText(t("gpsOpenRequired"));
            Toast.makeText(this, t("gpsNotPassedYet"), Toast.LENGTH_LONG).show();
            return;
        }
        if (locationStatusView != null) locationStatusView.setText(t("gpsChecking"));
        final boolean[] completed = new boolean[]{false};
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (completed[0]) return;
                completed[0] = true;
                try {
                    manager.removeUpdates(this);
                } catch (Exception ignored) {
                }
                saveLocationPass(location, locationProviderPassText(location));
            }
        };
        try {
            boolean requested = false;
            if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
                requested = true;
            }
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
                requested = true;
            }
            if (!requested) {
                put("gps", Status.REVIEW, t("gpsOpenRequired"));
                if (locationStatusView != null) locationStatusView.setText(t("gpsOpenRequired"));
                return;
            }
            handler.postDelayed(() -> {
                if (completed[0]) return;
                completed[0] = true;
                try {
                    manager.removeUpdates(listener);
                } catch (Exception ignored) {
                }
                put("gps", Status.REVIEW, t("gpsTimeoutReview"));
                if (locationStatusView != null) locationStatusView.setText(t("gpsTimeoutReview"));
                Toast.makeText(this, t("gpsNotPassedYet"), Toast.LENGTH_LONG).show();
            }, 35000);
        } catch (Exception e) {
            put("gps", Status.REVIEW, t("gpsRequestFailed"));
            if (locationStatusView != null) locationStatusView.setText(t("gpsRequestFailed"));
        }
    }

    private void runLocationAutoCheckAttempt() {
        if (!pendingLocationAutoCheck || currentTestStep != 3 || locationStatusView == null) return;
        locationAutoCheckAttempts++;
        locationStatusView.setText(t("gpsAutoTrying") + " " + locationAutoCheckAttempts + "/3");
        pendingLocationAutoCheck = false;
        runLocationFunctionTest();
    }

    private void showMotionSensorTest() {
        baseScreen(t("sensors"));
        motionLeftDone = false;
        motionRightDone = false;
        motionUpDone = false;
        motionDownDone = false;

        LinearLayout card = card();
        card.addView(title(t("sensorDirectionTitle")));
        card.addView(body(t("sensorDirectionGuide")));
        sensorStatusView = body(t("sensorDirectionWaiting"));
        card.addView(sensorStatusView);
        root.addView(card);

        Button fail = secondary(t("markSensorFail"));
        fail.setOnClickListener(v -> {
            if (sensorManager != null) sensorManager.unregisterListener(this);
            put("sensors", Status.FAIL, t("sensorUserFail"));
            nextTestStep();
        });
        root.addView(fail);

        startSensorTest();
    }

    private void updateMotionSensorStatus(float x, float y) {
        String note = t("sensorLive") + ": X " + String.format(Locale.US, "%.1f", x)
                + " / Y " + String.format(Locale.US, "%.1f", y) + "\n"
                + t("sensorLeft") + ": " + checkMark(motionLeftDone) + "\n"
                + t("sensorRight") + ": " + checkMark(motionRightDone) + "\n"
                + t("sensorUp") + ": " + checkMark(motionUpDone) + "\n"
                + t("sensorDown") + ": " + checkMark(motionDownDone);
        sensorStatusView.setText(note);
    }

    private String checkMark(boolean done) {
        return done ? t("pass") : t("pending");
    }

    private void showProximitySensorTest() {
        baseScreen(t("proximity"));
        proximityPassed = false;
        proximityMaxRange = 0f;
        lightBaselineLux = -1f;
        proximityBaselineFar = -1f;
        proximityNearCount = 0;
        lightDarkCount = 0;
        proximityCalibrateUntilMs = System.currentTimeMillis() + 1800L;

        LinearLayout card = card();
        card.addView(title(t("proximityTitle")));
        card.addView(body(t("proximityGuide")));
        proximityStatusView = body(t("proximityCalibrating"));
        card.addView(proximityStatusView);
        root.addView(card);

        Button fail = secondary(t("markProximityFail"));
        fail.setOnClickListener(v -> {
            if (sensorManager != null) sensorManager.unregisterListener(this);
            put("proximity", Status.FAIL, t("proximityUserFail"));
            nextTestStep();
        });
        root.addView(fail);

        startProximitySensorTest();
    }

    private void startProximitySensorTest() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            put("proximity", Status.REVIEW, t("proximityMissing"));
            if (proximityStatusView != null) proximityStatusView.setText(t("proximityMissing"));
            return;
        }
        Sensor proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        Sensor light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (proximity == null && light == null) {
            put("proximity", Status.REVIEW, t("proximityMissing"));
            if (proximityStatusView != null) proximityStatusView.setText(t("proximityMissing"));
            return;
        }
        if (proximity != null) {
            proximityMaxRange = proximity.getMaximumRange();
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (light != null) {
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL);
        }
        put("proximity", Status.REVIEW, t("proximityWaiting"));
    }

    private void handleProximityChanged(SensorEvent event) {
        if (currentTestStep != 5 || proximityStatusView == null || proximityPassed) return;
        float value = event.values[0];
        if (System.currentTimeMillis() < proximityCalibrateUntilMs) {
            proximityBaselineFar = Math.max(proximityBaselineFar, value);
            proximityStatusView.setText(t("proximityCalibrating") + "\n"
                    + t("proximityValue") + ": " + String.format(Locale.US, "%.1f", value));
            return;
        }
        proximityStatusView.setText(t("proximityValue") + ": " + String.format(Locale.US, "%.1f", value)
                + " / " + t("lightStart") + " " + String.format(Locale.US, "%.1f", proximityBaselineFar) + "\n"
                + t("proximityCoverTop"));
        float drop = proximityBaselineFar - value;
        float threshold = Math.max(1.0f, proximityBaselineFar * 0.45f);
        if (proximityBaselineFar > 1.0f && drop >= threshold) {
            proximityNearCount++;
        } else {
            proximityNearCount = 0;
        }
        if (proximityNearCount >= 2) {
            passProximityTest(t("proximityPass"));
        }
    }

    private void handleLightChanged(SensorEvent event) {
        if (currentTestStep != 5 || proximityStatusView == null || proximityPassed) return;
        float lux = event.values[0];
        if (System.currentTimeMillis() < proximityCalibrateUntilMs) {
            lightBaselineLux = Math.max(lightBaselineLux, lux);
            proximityStatusView.setText(t("proximityCalibrating") + "\n"
                    + t("lightValue") + ": " + String.format(Locale.US, "%.1f", lux));
            return;
        }
        if (lightBaselineLux < 0.1f) {
            lightBaselineLux = Math.max(lux, 0.1f);
        }
        proximityStatusView.setText(t("lightValue") + ": " + String.format(Locale.US, "%.1f", lux)
                + " / " + t("lightStart") + " " + String.format(Locale.US, "%.1f", lightBaselineLux) + "\n"
                + t("proximityCoverTop"));
        if (lightBaselineLux >= 5f && lux <= lightBaselineLux * 0.35f) {
            lightDarkCount++;
        } else {
            lightDarkCount = 0;
        }
        if (lightDarkCount >= 2) {
            passProximityTest(t("lightSensorPass"));
        }
    }

    private void passProximityTest(String note) {
        if (proximityPassed) return;
        proximityPassed = true;
        runVibrationTest();
        put("proximity", Status.PASS, note);
        proximityStatusView.setText(note + "\n" + t("proximityVibrated"));
        if (sensorManager != null) sensorManager.unregisterListener(this);
        Button next = primary(t("proximityContinue"));
        next.setOnClickListener(v -> nextTestStepIfStillOn(5));
        root.addView(next);
    }

    private void showNfcTest() {
        baseScreen(t("nfc"));
        nfcTagRead = false;
        LinearLayout card = card();
        card.addView(title(t("nfcTestTitle")));
        card.addView(body(t("nfcTestGuide")));
        nfcStatusView = body(nfcNote());
        card.addView(nfcStatusView);
        root.addView(card);

        Button check = primary(t("checkNfcNow"));
        check.setOnClickListener(v -> {
            put("nfc", nfcStatus(), nfcNote());
            if (nfcStatusView != null) nfcStatusView.setText(nfcNote());
            enableNfcForegroundDispatch();
        });
        root.addView(check);

        Button open = secondary(t("openNfcSettings"));
        open.setOnClickListener(v -> openNfcSettings());
        root.addView(open);

        Button fail = secondary(t("markNfcFail"));
        fail.setOnClickListener(v -> {
            put("nfc", Status.FAIL, t("nfcUserFail"));
            disableNfcForegroundDispatch();
            nextTestStep();
        });
        root.addView(fail);

        Button next = primary(t("continue"));
        next.setOnClickListener(v -> {
            put("nfc", nfcStatus(), nfcNote());
            disableNfcForegroundDispatch();
            nextTestStep();
        });
        root.addView(next);
        enableNfcForegroundDispatch();
    }

    private void openNfcSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            } catch (Exception ignored) {
                Toast.makeText(this, t("nfcSettingsFailed"), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Status nfcStatus() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) return Status.FAIL;
        if (!adapter.isEnabled()) return Status.REVIEW;
        return nfcTagRead ? Status.PASS : Status.REVIEW;
    }

    private String nfcNote() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null) return t("nfcMissing");
        if (nfcTagRead) return t("nfcTagPass");
        if (adapter.isEnabled()) return t("nfcWaitingCard");
        return t("nfcDisabledReview");
    }

    private void enableNfcForegroundDispatch() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (adapter == null || !adapter.isEnabled()) return;
        try {
            Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);
            IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
            IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
            adapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{tag, tech, ndef}, null);
        } catch (Exception ignored) {
        }
    }

    private void disableNfcForegroundDispatch() {
        try {
            NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
            if (adapter != null) adapter.disableForegroundDispatch(this);
        } catch (Exception ignored) {
        }
    }

    private void handleNfcIntent(Intent intent) {
        if (intent == null || currentTestStep != 6 || nfcStatusView == null) return;
        String action = intent.getAction();
        if (!NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                && !NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            return;
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        nfcTagRead = true;
        String detail = t("nfcTagPass");
        if (tag != null) {
            detail += "\n" + t("nfcTagId") + ": " + bytesToHex(tag.getId());
            String[] techList = tag.getTechList();
            if (techList != null && techList.length > 0) {
                detail += "\n" + t("nfcTagTech") + ": " + techList.length;
            }
        }
        put("nfc", Status.PASS, detail);
        nfcStatusView.setText(detail);
        runVibrationTest();
        Toast.makeText(this, t("nfcTagPass"), Toast.LENGTH_LONG).show();
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "-";
        StringBuilder out = new StringBuilder();
        for (byte value : bytes) {
            out.append(String.format(Locale.US, "%02X", value));
        }
        return out.toString();
    }

    private void showSimSlotTest() {
        baseScreen(t("simSlot"));
        LinearLayout card = card();
        card.addView(title(t("simSlotTestTitle")));
        card.addView(body(t("simSlotTestGuide")));
        simStatusView = body(simSlotNote());
        card.addView(simStatusView);
        root.addView(card);

        Button check = primary(t("checkSimNow"));
        check.setOnClickListener(v -> {
            put("simSlot", simSlotStatus(), simSlotNote());
            if (simStatusView != null) simStatusView.setText(simSlotNote());
        });
        root.addView(check);

        Button fail = secondary(t("markSimFail"));
        fail.setOnClickListener(v -> {
            put("simSlot", Status.FAIL, t("simUserFail"));
            nextTestStep();
        });
        root.addView(fail);

        Button next = primary(t("continue"));
        next.setOnClickListener(v -> {
            put("simSlot", simSlotStatus(), simSlotNote());
            nextTestStep();
        });
        root.addView(next);
    }

    private Status simSlotStatus() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return Status.FAIL;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return Status.REVIEW;
        }
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (manager == null) return Status.REVIEW;
        int slots = detectedSimSlotCount();
        if (slots <= 0) return Status.REVIEW;
        boolean inserted = false;
        boolean ready = false;
        boolean ioError = false;
        for (int i = 0; i < slots; i++) {
            int state = simState(manager, i);
            if (state == TelephonyManager.SIM_STATE_READY) ready = true;
            if (state == TelephonyManager.SIM_STATE_CARD_IO_ERROR) ioError = true;
            if (state != TelephonyManager.SIM_STATE_ABSENT && state != TelephonyManager.SIM_STATE_UNKNOWN) inserted = true;
        }
        if (ioError) return Status.FAIL;
        if (ready && hasMobileService(manager)) return Status.PASS;
        return inserted || ready ? Status.REVIEW : Status.REVIEW;
    }

    private String simSlotNote() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return t("simNoTelephony");
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return t("simPermissionNeeded");
        }
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (manager == null) return t("simUnknown");
        int slots = detectedSimSlotCount();
        StringBuilder out = new StringBuilder();
        out.append(t("simSlotDetected")).append(": ").append(slots > 0 ? slots : t("simUnknown"));
        boolean anyReady = false;
        boolean anyInserted = false;
        for (int i = 0; i < Math.max(1, slots); i++) {
            int state = simState(manager, i);
            if (state == TelephonyManager.SIM_STATE_READY) anyReady = true;
            if (state != TelephonyManager.SIM_STATE_ABSENT && state != TelephonyManager.SIM_STATE_UNKNOWN) anyInserted = true;
            out.append("\n").append(t("simSlot")).append(" ").append(i + 1).append(": ").append(simStateText(state));
        }
        out.append("\n").append(mobileNetworkDetail(manager));
        if (anyReady && hasMobileService(manager)) {
            out.append("\n").append(t("simReadyNetworkPass"));
        } else if (anyReady || anyInserted) {
            out.append("\n").append(t("simReadyNoNetworkReview"));
        } else {
            out.append("\n").append(t("simNeedsInsertedReady"));
        }
        return out.toString();
    }

    private int detectedSimSlotCount() {
        try {
            TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (manager == null) return 0;
            if (Build.VERSION.SDK_INT >= 30) return manager.getActiveModemCount();
            return manager.getPhoneCount();
        } catch (Exception e) {
            return 0;
        }
    }

    private int simState(TelephonyManager manager, int slotIndex) {
        try {
            return Build.VERSION.SDK_INT >= 26 ? manager.getSimState(slotIndex) : manager.getSimState();
        } catch (Exception e) {
            return TelephonyManager.SIM_STATE_UNKNOWN;
        }
    }

    private String simStateText(int state) {
        if (state == TelephonyManager.SIM_STATE_READY) return t("simReady");
        if (state == TelephonyManager.SIM_STATE_ABSENT) return t("simAbsent");
        if (state == TelephonyManager.SIM_STATE_PIN_REQUIRED) return t("simPin");
        if (state == TelephonyManager.SIM_STATE_PUK_REQUIRED) return t("simPuk");
        if (state == TelephonyManager.SIM_STATE_NETWORK_LOCKED) return t("simLocked");
        if (state == TelephonyManager.SIM_STATE_CARD_IO_ERROR) return t("simIoError");
        if (state == TelephonyManager.SIM_STATE_CARD_RESTRICTED) return t("simRestricted");
        return t("simUnknown");
    }

    private boolean hasMobileService(TelephonyManager manager) {
        ServiceState serviceState = mobileServiceState(manager);
        if (serviceState != null && serviceState.getState() == ServiceState.STATE_IN_SERVICE) return true;
        return isActiveCellularNetwork();
    }

    private ServiceState mobileServiceState(TelephonyManager manager) {
        try {
            return manager == null ? null : manager.getServiceState();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isActiveCellularNetwork() {
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (manager == null) return false;
            NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
            return capabilities != null
                    && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    private String mobileNetworkDetail(TelephonyManager manager) {
        ServiceState serviceState = mobileServiceState(manager);
        StringBuilder out = new StringBuilder();
        out.append(t("mobileServiceState")).append(": ").append(serviceStateText(serviceState)).append("\n");
        out.append(t("mobileDataState")).append(": ").append(dataStateText(manager)).append("\n");
        out.append(t("activeCellularNetwork")).append(": ").append(isActiveCellularNetwork() ? t("yes") : t("no"));
        return out.toString();
    }

    private String serviceStateText(ServiceState state) {
        if (state == null) return t("simUnknown");
        int value = state.getState();
        if (value == ServiceState.STATE_IN_SERVICE) return t("serviceIn");
        if (value == ServiceState.STATE_OUT_OF_SERVICE) return t("serviceOut");
        if (value == ServiceState.STATE_EMERGENCY_ONLY) return t("serviceEmergency");
        if (value == ServiceState.STATE_POWER_OFF) return t("servicePowerOff");
        return t("simUnknown");
    }

    private String dataStateText(TelephonyManager manager) {
        try {
            int value = manager == null ? TelephonyManager.DATA_UNKNOWN : manager.getDataState();
            if (value == TelephonyManager.DATA_CONNECTED) return t("dataConnected");
            if (value == TelephonyManager.DATA_CONNECTING) return t("dataConnecting");
            if (value == TelephonyManager.DATA_DISCONNECTED) return t("dataDisconnected");
            if (value == TelephonyManager.DATA_SUSPENDED) return t("dataSuspended");
            return t("simUnknown");
        } catch (Exception e) {
            return t("simUnknown");
        }
    }

    private void showTelcoLockTest() {
        baseScreen(t("telcoLock"));
        LinearLayout card = card();
        card.addView(title(t("telcoLockTitle")));
        card.addView(body(t("telcoLockGuide")));
        TextView status = body(telcoLockNote());
        card.addView(status);
        root.addView(card);

        Button check = primary(t("checkTelcoLockNow"));
        check.setOnClickListener(v -> {
            put("telcoLock", telcoLockStatus(), telcoLockNote());
            status.setText(telcoLockNote());
        });
        root.addView(check);

        Button locked = secondary(t("markTelcoLockFail"));
        locked.setOnClickListener(v -> {
            put("telcoLock", Status.FAIL, t("telcoUserFail"));
            nextTestStep();
        });
        root.addView(locked);

        Button next = primary(t("continue"));
        next.setOnClickListener(v -> {
            put("telcoLock", telcoLockStatus(), telcoLockNote());
            nextTestStep();
        });
        root.addView(next);
    }

    private Status telcoLockStatus() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return Status.FAIL;
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return Status.REVIEW;
        }
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (manager == null) return Status.REVIEW;
        int slots = Math.max(1, detectedSimSlotCount());
        boolean ready = false;
        for (int i = 0; i < slots; i++) {
            int state = simState(manager, i);
            if (state == TelephonyManager.SIM_STATE_NETWORK_LOCKED) return Status.FAIL;
            if (state == TelephonyManager.SIM_STATE_READY) ready = true;
        }
        if (!ready) return Status.REVIEW;
        return hasMobileService(manager) ? Status.PASS : Status.REVIEW;
    }

    private String telcoLockNote() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return t("simNoTelephony");
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return t("simPermissionNeeded");
        }
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (manager == null) return t("simUnknown");
        int slots = Math.max(1, detectedSimSlotCount());
        StringBuilder out = new StringBuilder();
        boolean ready = false;
        boolean locked = false;
        for (int i = 0; i < slots; i++) {
            int state = simState(manager, i);
            if (state == TelephonyManager.SIM_STATE_READY) ready = true;
            if (state == TelephonyManager.SIM_STATE_NETWORK_LOCKED) locked = true;
            out.append(t("simSlot")).append(" ").append(i + 1).append(": ").append(simStateText(state)).append("\n");
        }
        String simOperator = safeText(manager.getSimOperatorName());
        String networkOperator = safeText(manager.getNetworkOperatorName());
        out.append(t("simOperator")).append(": ").append(simOperator.length() > 0 ? simOperator : "-").append("\n");
        out.append(t("networkOperator")).append(": ").append(networkOperator.length() > 0 ? networkOperator : "-").append("\n");
        out.append(mobileNetworkDetail(manager)).append("\n");
        if (locked) {
            out.append(t("telcoLockDetected"));
        } else if (ready && hasMobileService(manager)) {
            out.append(t("telcoNoLockForInsertedSim"));
        } else if (ready) {
            out.append(t("telcoSimReadyNoNetwork"));
        } else {
            out.append(t("telcoNeedsOtherSim"));
        }
        return out.toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void showBiometricTest() {
        baseScreen(t("biometric"));
        biometricPromptPassed = false;
        biometricPassNote = "";
        biometricFailNote = "";
        LinearLayout card = card();
        card.addView(title(t("biometricTestTitle")));
        card.addView(body(t("biometricTestGuide")));
        biometricStatusView = body(biometricNote());
        card.addView(biometricStatusView);
        root.addView(card);

        int authStatus = biometricAuthStatus();
        if (!hasFingerprintHardware()) {
            biometricFailNote = t("fingerprintMissing");
            put("biometric", Status.FAIL, biometricNote());
            biometricStatusView.setText(biometricNote());
        } else if (authStatus == 0) {
            Button auth = primary(t("startBiometricPrompt"));
            auth.setOnClickListener(v -> startBiometricPrompt());
            root.addView(auth);
        } else {
            biometricFailNote = authStatus == 11 ? t("fingerprintNoneEnrolled") : t("fingerprintUnavailable");
            put("biometric", Status.FAIL, biometricNote());
            biometricStatusView.setText(biometricNote());
        }

        Button open = secondary(t("openFingerprintSettings"));
        open.setOnClickListener(v -> openFingerprintSettings());
        root.addView(open);

        Button refresh = secondary(t("refreshFingerprintStatus"));
        refresh.setOnClickListener(v -> showBiometricTest());
        root.addView(refresh);

        Button fail = secondary(t("markBiometricFail"));
        fail.setOnClickListener(v -> {
            put("biometric", Status.FAIL, t("biometricUserFail"));
            biometricFailNote = t("biometricUserFail");
            nextTestStep();
        });
        root.addView(fail);

        Button next = primary(t("continue"));
        next.setOnClickListener(v -> {
            put("biometric", biometricStatus(), biometricNote());
            nextTestStep();
        });
        root.addView(next);
    }

    private void openBiometricSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startActivity(new Intent(Settings.ACTION_BIOMETRIC_ENROLL));
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
        } catch (Exception e) {
            biometricFailNote = t("biometricSettingsFailed");
            put("biometric", Status.FAIL, t("biometricSettingsFailed"));
            if (biometricStatusView != null) biometricStatusView.setText(biometricNote());
            Toast.makeText(this, t("biometricSettingsFailed"), Toast.LENGTH_LONG).show();
        }
    }

    private Status biometricStatus() {
        boolean fingerprint = hasFingerprintHardware();
        if (!fingerprint) return Status.FAIL;
        if (biometricPromptPassed) return Status.PASS;
        if (biometricFailNote.length() > 0) return Status.FAIL;
        int auth = biometricAuthStatus();
        return auth == 0 ? Status.REVIEW : Status.FAIL;
    }

    private String biometricNote() {
        boolean fingerprint = hasFingerprintHardware();
        StringBuilder out = new StringBuilder();
        out.append(t("fingerprint")).append(": ").append(fingerprint ? t("detected") : t("notDetected"));
        if (!fingerprint) {
            out.append("\n").append(t("fingerprintMissing"));
            return out.toString();
        }
        if (biometricPromptPassed) {
            out.append("\n").append(biometricPassNote.length() > 0 ? biometricPassNote : t("biometricPromptPass"));
            return out.toString();
        }
        if (biometricFailNote.length() > 0) {
            out.append("\n").append(biometricFailNote);
            return out.toString();
        }
        int auth = biometricAuthStatus();
        if (auth == 0) {
            out.append("\n").append(t("fingerprintReadyNeedPrompt"));
        } else if (auth == 11) {
            out.append("\n").append(t("fingerprintNoneEnrolled"));
        } else if (auth == 12 || auth == 1) {
            out.append("\n").append(t("fingerprintUnavailable"));
        } else {
            out.append("\n").append(t("fingerprintReview"));
        }
        return out.toString();
    }

    private void startBiometricPrompt() {
        if (Build.VERSION.SDK_INT < 28) {
            biometricFailNote = t("biometricPromptUnsupported");
            put("biometric", Status.FAIL, t("biometricPromptUnsupported"));
            if (biometricStatusView != null) biometricStatusView.setText(biometricNote());
            Toast.makeText(this, t("biometricPromptUnsupported"), Toast.LENGTH_LONG).show();
            return;
        }
        if (biometricAuthStatus() != 0) {
            biometricFailNote = t("biometricNeedEnroll");
            put("biometric", Status.FAIL, biometricNote());
            if (biometricStatusView != null) biometricStatusView.setText(biometricNote());
            Toast.makeText(this, t("biometricNeedEnroll"), Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Executor executor = command -> handler.post(command);
            BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                    .setTitle(t("biometricPromptTitle"))
                    .setSubtitle(t("biometricPromptSubtitle"))
                    .setNegativeButton(t("cancel"), executor, (dialog, which) -> {
                    })
                    .build();
            prompt.authenticate(new CancellationSignal(), executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    passBiometricCheck(t("biometricPromptPass"));
                }

                @Override
                public void onAuthenticationFailed() {
                    if (biometricStatusView != null) biometricStatusView.setText(t("biometricPromptFailed"));
                }

                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    biometricFailNote = t("biometricPromptError") + ": " + errString;
                    put("biometric", Status.FAIL, biometricFailNote);
                    if (biometricStatusView != null) {
                        biometricStatusView.setText(biometricNote());
                    }
                }
            });
        } catch (Exception e) {
            biometricFailNote = t("biometricPromptError");
            put("biometric", Status.FAIL, t("biometricPromptError"));
            if (biometricStatusView != null) biometricStatusView.setText(biometricNote());
            Toast.makeText(this, t("biometricPromptError"), Toast.LENGTH_LONG).show();
        }
    }

    private void passBiometricCheck(String note) {
        biometricPromptPassed = true;
        biometricPassNote = note;
        biometricFailNote = "";
        put("biometric", Status.PASS, biometricNote());
        if (biometricStatusView != null) biometricStatusView.setText(biometricNote());
        runVibrationTest();
        Toast.makeText(this, note, Toast.LENGTH_LONG).show();
    }

    private void openFingerprintSettings() {
        try {
            if (Build.VERSION.SDK_INT >= 28) {
                startActivity(new Intent(Settings.ACTION_FINGERPRINT_ENROLL));
                return;
            }
        } catch (Exception ignored) {
        }
        openBiometricSettings();
    }

    private boolean hasFingerprintHardware() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
    }

    private boolean hasFaceHardware() {
        return Build.VERSION.SDK_INT >= 29 && getPackageManager().hasSystemFeature(PackageManager.FEATURE_FACE);
    }

    private int biometricAuthStatus() {
        if (Build.VERSION.SDK_INT < 23) return 12;
        if (Build.VERSION.SDK_INT < 29) {
            try {
                FingerprintManager manager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
                if (manager == null || !manager.isHardwareDetected()) return 12;
                return manager.hasEnrolledFingerprints() ? 0 : 11;
            } catch (Exception e) {
                return 12;
            }
        }
        try {
            BiometricManager manager = (BiometricManager) getSystemService(BIOMETRIC_SERVICE);
            if (manager == null) return 12;
            if (Build.VERSION.SDK_INT >= 30) {
                return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK);
            }
            return manager.canAuthenticate();
        } catch (Exception e) {
            return 12;
        }
    }

    private void enterFullScreenTestMode(int backgroundColor) {
        fullScreenTestActive = true;
        fullScreenTestColor = backgroundColor;
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
        );
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(attrs);
        }
        getWindow().getDecorView().setBackgroundColor(backgroundColor);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
            if (fullScreenTestActive) handler.postDelayed(() -> applyFullScreenFlags(), 250);
        });
        applyFullScreenFlags();
    }

    private void applyFullScreenFlags() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LOW_PROFILE
        );
    }

    private void exitFullScreenTestMode() {
        fullScreenTestActive = false;
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        );
        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            getWindow().setAttributes(attrs);
        }
        getWindow().getDecorView().setBackgroundColor(BG);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(SURFACE);
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    private void showTouchTest() {
        enterFullScreenTestMode(Color.WHITE);
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(Color.WHITE);
        screen.setPadding(0, 0, 0, 0);
        screen.setFitsSystemWindows(false);

        TouchPad pad = new TouchPad(this);
        pad.setFitsSystemWindows(false);
        activeTouchPad = pad;
        touchTestActive = true;
        screen.addView(pad, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(screen);
        handler.postDelayed(() -> applyFullScreenFlags(), 120);
    }

    private void finishTouchTest() {
        if (activeTouchPad == null) return;
        int percent = activeTouchPad.coveragePercent();
        touchTestActive = false;
        put("touch", percent >= 65 ? Status.PASS : Status.REVIEW, t("coverage") + " " + percent + "%");
        nextTestStep();
    }

    private void showMicrophoneTest() {
        baseScreen(t("microphone"));
        LinearLayout card = card();
        card.addView(title(t("micTestTitle")));
        card.addView(body(t("micTestGuide")));
        micStatusView = body(t("micReady"));
        card.addView(micStatusView);
        root.addView(card);

        Button start = primary(t("startMic"));
        start.setOnClickListener(v -> startMicrophoneSample());
        root.addView(start);

        Button pass = secondary(t("micPlaybackClear"));
        pass.setOnClickListener(v -> {
            stopMicSampling();
            put("microphone", Status.PASS, t("micUserPass"));
            nextTestStep();
        });
        root.addView(pass);

        Button fail = secondary(t("markMicFail"));
        fail.setOnClickListener(v -> {
            stopMicSampling();
            put("microphone", Status.FAIL, t("micUserFail"));
            nextTestStep();
        });
        root.addView(fail);
    }

    private void showSpeakerTest() {
        baseScreen(t("speaker"));
        LinearLayout card = card();
        card.addView(title(t("speakerTestTitle")));
        card.addView(body(t("speakerTestGuide")));
        speakerStatusView = body(t("speakerReady"));
        card.addView(speakerStatusView);
        root.addView(card);

        Button play = primary(t("playSpeaker"));
        play.setOnClickListener(v -> {
            runSpeakerTest();
        });
        root.addView(play);

        Button pass = secondary(t("speakerGood"));
        pass.setOnClickListener(v -> {
            put("speaker", Status.PASS, t("speakerUserPass"));
            nextTestStep();
        });
        root.addView(pass);

        Button fail = secondary(t("speakerBad"));
        fail.setOnClickListener(v -> {
            put("speaker", Status.FAIL, t("speakerUserFail"));
            nextTestStep();
        });
        root.addView(fail);
    }

    private void showCameraDetailTest() {
        baseScreen(t("cameraDetailTitle"));
        LinearLayout diagnostics = card();
        diagnostics.addView(title(t("cameraAutoInfo")));
        diagnostics.addView(body(cameraDiagnostics()));
        cameraHardwareStatusView = body(t("cameraHardwareReady"));
        diagnostics.addView(cameraHardwareStatusView);
        root.addView(diagnostics);

        Button backTest = primary(t("testBackCamera"));
        backTest.setOnClickListener(v -> openSafeCameraPreview(CameraSandboxActivity.FACING_BACK));
        root.addView(backTest);

        Button frontTest = primary(t("testFrontCamera"));
        frontTest.setOnClickListener(v -> openSafeCameraPreview(CameraSandboxActivity.FACING_FRONT));
        root.addView(frontTest);

        LinearLayout card = card();
        card.addView(title(t("cameraManualCheck")));
        card.addView(body(t("cameraManualGuide")));

        card.addView(label(t("cameraOpenCondition")));
        cameraOpenSpinner = spinner(new String[]{
                t("cameraNotTested"),
                t("cameraOpensOk"),
                t("cameraCannotOpen")
        });
        card.addView(cameraOpenSpinner);

        card.addView(label(t("ultraWide05")));
        ultraWideSpinner = spinner(new String[]{
                t("cameraNotTested"),
                t("cameraOk"),
                t("cameraNotAvailable"),
                t("cameraProblem")
        });
        card.addView(ultraWideSpinner);

        card.addView(label(t("zoomInOut")));
        zoomSpinner = spinner(new String[]{
                t("cameraNotTested"),
                t("cameraOk"),
                t("cameraProblem")
        });
        card.addView(zoomSpinner);

        card.addView(label(t("cameraBlackSpots")));
        cameraSpotSpinner = spinner(new String[]{
                t("noneSeen"),
                t("minorSeen"),
                t("obviousSeen")
        });
        card.addView(cameraSpotSpinner);

        card.addView(label(t("cameraFogWatermark")));
        cameraFogSpinner = spinner(new String[]{
                t("noneSeen"),
                t("minorSeen"),
                t("obviousSeen")
        });
        card.addView(cameraFogSpinner);

        card.addView(label(t("cameraFocusBlur")));
        cameraFocusSpinner = spinner(new String[]{
                t("cameraOk"),
                t("minorSeen"),
                t("obviousSeen")
        });
        card.addView(cameraFocusSpinner);
        root.addView(card);

        Button save = primary(t("saveCameraDetail"));
        save.setOnClickListener(v -> {
            int penalty = cameraDetailDeduction();
            Result existing = results.get("camera");
            Status status = penalty >= 180 ? Status.FAIL : Status.REVIEW;
            if ((backCameraChecked && !backCameraPassed) || (frontCameraChecked && !frontCameraPassed)) status = Status.FAIL;
            if (manualRearCameraProblem()) status = Status.FAIL;
            if (existing != null && existing.status == Status.FAIL) status = Status.FAIL;
            put("camera", status, cameraDetailSummary());
            nextTestStep();
        });
        root.addView(save);
    }

    private void openCameraApp() {
        put("camera", Status.REVIEW, t("cameraSafeMode"));
        showCameraDetailTest();
    }

    private void openSafeCameraPreview(String facing) {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 18);
            return;
        }
        try {
            clearCameraSideBeforeRetest(facing);
            Intent intent = new Intent(this, CameraSandboxActivity.class);
            intent.putExtra(CameraSandboxActivity.EXTRA_FACING, facing);
            pendingCameraFacing = facing;
            cameraWatchdogRunnable = () -> {
                if (pendingCameraFacing == null || pendingCameraFacing.length() == 0) return;
                String timedOutFacing = pendingCameraFacing;
                pendingCameraFacing = "";
                appendDebugLog("camera_watchdog_timeout", timedOutFacing);
                setCameraSideResult(timedOutFacing, false);
                Intent back = new Intent(this, MainActivity.class);
                back.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(back);
                handler.postDelayed(() -> {
                    if (currentTestStep == 12) showCameraDetailTest();
                }, 350);
            };
            handler.postDelayed(cameraWatchdogRunnable, 8000);
            startActivityForResult(intent, 19);
        } catch (Exception e) {
            if (cameraWatchdogRunnable != null) {
                handler.removeCallbacks(cameraWatchdogRunnable);
                cameraWatchdogRunnable = null;
            }
            setCameraSideResult(facing, false);
            pendingCameraFacing = "";
        }
    }

    private void clearCameraSideBeforeRetest(String facing) {
        if (CameraSandboxActivity.FACING_FRONT.equals(facing)) {
            frontCameraChecked = true;
            frontCameraPassed = false;
        } else {
            backCameraChecked = true;
            backCameraPassed = false;
        }
        put("camera", cameraSideStatus(), cameraSideSummary());
        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(cameraSideSummary());
    }

    private void showCameraPreviewTest() {
        put("camera", Status.REVIEW, t("cameraSafeMode"));
        showCameraDetailTest();
    }

    private void setCameraSideResult(String facing, boolean passed) {
        if (CameraSandboxActivity.FACING_FRONT.equals(facing)) {
            frontCameraChecked = true;
            frontCameraPassed = passed;
        } else {
            if (passed) {
                if (cameraOpenSpinner != null) cameraOpenSpinner.setSelection(1);
                String note = t("cameraRearAutoNeedsManual") + "\n" + cameraSideSummary();
                put("camera", Status.REVIEW, note);
                if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(note);
                return;
            } else {
                backCameraChecked = true;
                backCameraPassed = false;
            }
        }
        String note = cameraSideSummary();
        Status status = cameraSideStatus();
        put("camera", status, note);
        if (cameraOpenSpinner != null) {
            if (status == Status.FAIL) {
                cameraOpenSpinner.setSelection(2);
            } else if (backCameraChecked || frontCameraChecked) {
                cameraOpenSpinner.setSelection(1);
            }
        }
        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(note);
    }

    private Status cameraSideStatus() {
        if ((backCameraChecked && !backCameraPassed) || (frontCameraChecked && !frontCameraPassed)) return Status.FAIL;
        if (backCameraChecked && frontCameraChecked && backCameraPassed && frontCameraPassed) return Status.REVIEW;
        return Status.REVIEW;
    }

    private String cameraSideSummary() {
        String back = backCameraChecked ? (backCameraPassed ? t("pass") : t("fail")) : t("notTested");
        String front = frontCameraChecked ? (frontCameraPassed ? t("pass") : t("fail")) : t("notTested");
        return t("backCamera") + ": " + back + "\n" + t("frontCamera") + ": " + front;
    }

    private void openCameraOnPreview(SurfaceTexture texture, TextView statusView) {
        try {
            closeCameraPreview();
            int cameraId = 0;
            int count = android.hardware.Camera.getNumberOfCameras();
            if (count <= 0) {
                put("camera", Status.FAIL, t("cameraMissing"));
                statusView.setText(t("cameraMissing"));
                return;
            }
            android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
            for (int i = 0; i < count; i++) {
                android.hardware.Camera.getCameraInfo(i, info);
                if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraId = i;
                    break;
                }
            }
            texture.setDefaultBufferSize(1280, 720);
            activeLegacyCamera = android.hardware.Camera.open(cameraId);
            if (activeLegacyCamera == null) throw new RuntimeException("Camera open returned null");
            activeLegacyCamera.setPreviewTexture(texture);
            activeLegacyCamera.startPreview();
            put("camera", Status.REVIEW, t("cameraPreviewSessionOpened"));
            statusView.setText(t("cameraPreviewCheckNow"));
        } catch (Exception e) {
            put("camera", Status.FAIL, t("cameraHardwareFailed"));
            statusView.setText(t("cameraHardwareFailed"));
            closeCameraPreview();
        }
    }

    private void closeCameraPreview() {
        try {
            if (activeLegacyCamera != null) {
                activeLegacyCamera.stopPreview();
                activeLegacyCamera.setPreviewCallback(null);
                activeLegacyCamera.release();
            }
        } catch (Exception ignored) {
        }
        activeLegacyCamera = null;
    }

    private void runCameraHardwareOpenTest() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraPermissionNeeded"));
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 18);
            return;
        }
        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraHardwareTesting"));
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (manager == null || manager.getCameraIdList().length == 0) {
                put("camera", Status.FAIL, t("cameraMissing"));
                if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraMissing"));
                return;
            }
            String targetId = manager.getCameraIdList()[0];
            for (String id : manager.getCameraIdList()) {
                Integer facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    targetId = id;
                    break;
                }
            }
            final String cameraIdToOpen = targetId;
            final boolean[] finished = new boolean[]{false};
            final CameraDevice[] cameraRef = new CameraDevice[]{null};
            final CameraCaptureSession[] sessionRef = new CameraCaptureSession[]{null};
            final SurfaceTexture[] textureRef = new SurfaceTexture[]{null};
            final Surface[] surfaceRef = new Surface[]{null};
            final Runnable cleanup = () -> {
                try {
                    if (sessionRef[0] != null) sessionRef[0].close();
                } catch (Exception ignored) {
                }
                try {
                    if (cameraRef[0] != null) cameraRef[0].close();
                } catch (Exception ignored) {
                }
                try {
                    if (surfaceRef[0] != null) surfaceRef[0].release();
                } catch (Exception ignored) {
                }
                try {
                    if (textureRef[0] != null) textureRef[0].release();
                } catch (Exception ignored) {
                }
            };
            manager.openCamera(cameraIdToOpen, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraRef[0] = camera;
                    try {
                        textureRef[0] = new SurfaceTexture(10);
                        textureRef[0].setDefaultBufferSize(640, 480);
                        surfaceRef[0] = new Surface(textureRef[0]);
                        CaptureRequest.Builder request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        request.addTarget(surfaceRef[0]);
                        camera.createCaptureSession(Collections.singletonList(surfaceRef[0]), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                sessionRef[0] = session;
                                try {
                                    session.setRepeatingRequest(request.build(), null, handler);
                                    finished[0] = true;
                                    handler.postDelayed(cleanup, 400);
                                    handler.post(() -> {
                                        put("camera", Status.REVIEW, t("cameraPreviewSessionOpened"));
                                        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraPreviewSessionOpened"));
                                    });
                                } catch (Exception e) {
                                    finished[0] = true;
                                    cleanup.run();
                                    handler.post(() -> {
                                        put("camera", Status.FAIL, t("cameraPreviewSessionFailed"));
                                        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraPreviewSessionFailed"));
                                    });
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                                finished[0] = true;
                                sessionRef[0] = session;
                                cleanup.run();
                                handler.post(() -> {
                                    put("camera", Status.FAIL, t("cameraPreviewSessionFailed"));
                                    if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraPreviewSessionFailed"));
                                });
                            }
                        }, handler);
                    } catch (Exception e) {
                        finished[0] = true;
                        cleanup.run();
                        handler.post(() -> {
                            put("camera", Status.FAIL, t("cameraPreviewSessionFailed"));
                            if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraPreviewSessionFailed"));
                        });
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    finished[0] = true;
                    cameraRef[0] = camera;
                    cleanup.run();
                    handler.post(() -> {
                        put("camera", Status.FAIL, t("cameraHardwareDisconnected"));
                        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraHardwareDisconnected"));
                    });
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    finished[0] = true;
                    cameraRef[0] = camera;
                    cleanup.run();
                    handler.post(() -> {
                        put("camera", Status.FAIL, t("cameraHardwareFailed"));
                        if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraHardwareFailed"));
                    });
                }
            }, handler);
            handler.postDelayed(() -> {
                if (!finished[0]) {
                    finished[0] = true;
                    cleanup.run();
                    put("camera", Status.FAIL, t("cameraHardwareTimeout"));
                    if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraHardwareTimeout"));
                }
            }, 5000);
        } catch (Exception e) {
            put("camera", Status.FAIL, t("cameraHardwareFailed"));
            if (cameraHardwareStatusView != null) cameraHardwareStatusView.setText(t("cameraHardwareFailed"));
        }
    }

    private void showScreenTest(int index) {
        int[] colors = new int[]{Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE};
        String[] names = new String[]{t("white"), t("black"), t("red"), t("green"), t("blue")};
        enterFullScreenTestMode(colors[index]);
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(0, 0, 0, 0);
        screen.setBackgroundColor(colors[index]);
        screen.setFitsSystemWindows(false);

        TextView instruction = new TextView(this);
        instruction.setText(names[index] + "\n" + t("screenTapNext"));
        instruction.setTextColor(index == 1 ? Color.WHITE : Color.BLACK);
        instruction.setTextSize(22);
        instruction.setGravity(Gravity.CENTER);
        instruction.setAlpha(0.62f);
        instruction.setFitsSystemWindows(false);
        screen.addView(instruction, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        screen.setOnClickListener(v -> {
            if (index == colors.length - 1) {
                showScreenConfirm();
            } else {
                showScreenTest(index + 1);
            }
        });
        setContentView(screen);
        handler.postDelayed(() -> applyFullScreenFlags(), 120);
    }

    private void showScreenConfirm() {
        baseScreen(t("screen"));
        LinearLayout card = card();
        card.addView(title(t("screenConfirmTitle")));
        card.addView(body(t("screenGuide")));
        root.addView(card);

        Button pass = primary(t("screenLooksGood"));
        pass.setOnClickListener(v -> {
            put("screen", Status.PASS, t("screenPass"));
            nextTestStep();
        });
        root.addView(pass);

        Button fail = secondary(t("screenProblem"));
        fail.setOnClickListener(v -> {
            put("screen", Status.FAIL, t("screenFail"));
            nextTestStep();
        });
        root.addView(fail);
    }

    private void showButtonTest() {
        volumeUpPressed = false;
        volumeDownPressed = false;
        volumeContinueShown = false;
        baseScreen(t("buttons"));
        LinearLayout card = card();
        card.addView(title(t("volumeTestTitle")));
        card.addView(body(t("buttonGuide")));
        volumeSummaryView = body(t("volumeWaiting"));
        card.addView(volumeSummaryView);
        root.addView(card);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        volumeUpStatusView = volumeStatusCard(t("volumeUp"), false);
        volumeDownStatusView = volumeStatusCard(t("volumeDown"), false);
        grid.addView(volumeUpStatusView);
        grid.addView(volumeDownStatusView);
        root.addView(grid);

        Button fail = secondary(t("markButtonsFail"));
        fail.setOnClickListener(v -> {
            put("buttons", Status.FAIL, t("buttonsUserFail"));
            nextTestStep();
        });
        root.addView(fail);
        updateButtonResult();
    }

    private void showConditionForm() {
        baseScreen(t("conditionTitle"));
        LinearLayout card = card();
        card.addView(title(t("conditionTitle")));
        card.addView(body(t("conditionGuide")));

        card.addView(label(t("overallCondition")));
        overallConditionSpinner = spinner(new String[]{
                t("conditionExcellent"),
                t("conditionGood"),
                t("conditionFair"),
                t("conditionPoor")
        });
        card.addView(overallConditionSpinner);

        card.addView(label(t("screenScratch")));
        screenScratchSpinner = spinner(new String[]{
                t("scratchNone"),
                t("scratchLight"),
                t("scratchVisible"),
                t("scratchHeavy")
        });
        card.addView(screenScratchSpinner);

        card.addView(label(t("backCoverCondition")));
        backCoverSpinner = spinner(new String[]{
                t("backGood"),
                t("backLightWear"),
                t("backDents"),
                t("backCracked")
        });
        card.addView(backCoverSpinner);
        root.addView(card);

        Button save = primary(t("saveCondition"));
        save.setOnClickListener(v -> nextTestStep());
        root.addView(save);
    }

    private void updateButtonResult() {
        if (root == null) return;
        if (volumeUpStatusView != null) updateVolumeStatusCard(volumeUpStatusView, t("volumeUp"), volumeUpPressed);
        if (volumeDownStatusView != null) updateVolumeStatusCard(volumeDownStatusView, t("volumeDown"), volumeDownPressed);
        if (volumeSummaryView != null) {
            volumeSummaryView.setText(volumeUpPressed && volumeDownPressed ? t("volumeBothDetected") : t("volumeWaiting"));
        }
        if (volumeUpPressed && volumeDownPressed) {
            put("buttons", Status.PASS, t("buttonsPass"));
            if (!volumeContinueShown) {
                volumeContinueShown = true;
                Button next = primary(t("buttonsContinue"));
                next.setOnClickListener(v -> nextTestStep());
                root.addView(next);
            }
        }
    }

    private TextView volumeStatusCard(String label, boolean done) {
        TextView view = new TextView(this);
        view.setTextSize(22);
        view.setTypeface(null, 1);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(16), dp(22), dp(16), dp(22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        view.setLayoutParams(params);
        updateVolumeStatusCard(view, label, done);
        return view;
    }

    private void updateVolumeStatusCard(TextView view, String label, boolean done) {
        view.setText(label + "\n" + (done ? t("pass") : t("pending")));
        view.setTextColor(done ? GREEN : AMBER);
        view.setBackgroundColor(done ? SOFT_GREEN : SOFT_AMBER);
    }

    private void showEstimate() {
        baseScreen(t("estimate"));
        LinearLayout card = card();
        int finalPrice = Math.max(0, basePrice - deduction);
        card.addView(title(t("estimateTitle")));
        card.addView(body(t("matched") + " " + selected(ramSpinner) + " / " + selected(romSpinner)));
        TextView price = new TextView(this);
        price.setText("MYR " + finalPrice);
        price.setTextColor(GREEN);
        price.setTextSize(38);
        price.setTypeface(null, 1);
        price.setPadding(0, dp(8), 0, dp(8));
        card.addView(price);
        card.addView(row(t("base"), "MYR " + basePrice));
        card.addView(row(t("deduction"), "MYR " + deduction));
        card.addView(row(t("updated"), "2026-06-06"));
        root.addView(card);

        LinearLayout condition = card();
        condition.addView(title(t("conditionSummary")));
        condition.addView(row(t("overallCondition"), conditionValue(overallConditionSpinner, t("conditionNotSet"))));
        condition.addView(row(t("screenScratch"), conditionValue(screenScratchSpinner, t("conditionNotSet"))));
        condition.addView(row(t("backCoverCondition"), conditionValue(backCoverSpinner, t("conditionNotSet"))));
        condition.addView(row(t("conditionDeduction"), "MYR " + conditionDeduction()));
        root.addView(condition);

        LinearLayout camera = card();
        camera.addView(title(t("cameraDetailSummary")));
        camera.addView(row(t("backCamera"), backCameraChecked ? (backCameraPassed ? t("pass") : t("fail")) : t("notTested")));
        camera.addView(row(t("frontCamera"), frontCameraChecked ? (frontCameraPassed ? t("pass") : t("fail")) : t("notTested")));
        camera.addView(row(t("cameraOpenCondition"), conditionValue(cameraOpenSpinner, t("conditionNotSet"))));
        camera.addView(row(t("ultraWide05"), conditionValue(ultraWideSpinner, t("conditionNotSet"))));
        camera.addView(row(t("zoomInOut"), conditionValue(zoomSpinner, t("conditionNotSet"))));
        camera.addView(row(t("cameraBlackSpots"), conditionValue(cameraSpotSpinner, t("conditionNotSet"))));
        camera.addView(row(t("cameraFogWatermark"), conditionValue(cameraFogSpinner, t("conditionNotSet"))));
        camera.addView(row(t("cameraFocusBlur"), conditionValue(cameraFocusSpinner, t("conditionNotSet"))));
        camera.addView(row(t("cameraDetailDeduction"), "MYR " + cameraDetailDeduction()));
        root.addView(camera);

        LinearLayout summary = card();
        summary.addView(title(t("testSummary")));
        for (String key : new String[]{"camera", "flash", "microphone", "speaker", "vibration", "wifi", "bluetooth", "gps", "battery", "sensors", "proximity", "nfc", "simSlot", "telcoLock", "biometric", "touch", "screen", "buttons"}) {
            Result result = result(key);
            summary.addView(row(labelFor(key), result.status.label(this) + " - " + result.note));
        }
        root.addView(summary);

        TextView disclaimer = body(t("disclaimer"));
        disclaimer.setTextColor(Color.rgb(104, 72, 7));
        disclaimer.setBackgroundColor(Color.rgb(255, 243, 215));
        disclaimer.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(disclaimer);

        Button pdf = primary(t("savePdf"));
        pdf.setOnClickListener(v -> savePdfReport());
        root.addView(pdf);

        Button submit = primary(t("submit"));
        submit.setOnClickListener(v -> showSubmit());
        root.addView(submit);
    }

    private void savePdfReport() {
        try {
            File file = createPdfReport("", "", "");
            Toast.makeText(this, t("pdfSaved") + ": " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, t("pdfFailed"), Toast.LENGTH_LONG).show();
        }
    }

    private File createPdfReport(String name, String phone, String notes) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(20, 20, 20));
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int y = 42;

        paint.setTextSize(20);
        paint.setFakeBoldText(true);
        canvas.drawText("Phone Check Report", 40, y, paint);
        paint.setTextSize(12);
        paint.setFakeBoldText(false);
        y += 28;

        y = pdfLine(canvas, paint, y, t("version") + ": " + APP_VERSION_LABEL);
        if (!blankToDash(name).equals("-")) y = pdfLine(canvas, paint, y, t("name") + ": " + blankToDash(name));
        if (!blankToDash(phone).equals("-")) y = pdfLine(canvas, paint, y, t("phone") + ": " + blankToDash(phone));
        y = pdfLine(canvas, paint, y, t("brand") + ": " + clean(Build.MANUFACTURER));
        y = pdfLine(canvas, paint, y, t("model") + ": " + clean(Build.MODEL));
        y = pdfLine(canvas, paint, y, "Android: " + Build.VERSION.RELEASE);
        y = pdfLine(canvas, paint, y, t("ramDetected") + ": " + estimateRamGb() + " GB");
        y = pdfLine(canvas, paint, y, t("storageDetected") + ": " + estimateRomGb() + " GB");
        y = pdfLine(canvas, paint, y, t("matched") + " " + selected(ramSpinner) + " / " + selected(romSpinner));
        y = pdfLine(canvas, paint, y, t("base") + ": MYR " + basePrice);
        y = pdfLine(canvas, paint, y, t("deduction") + ": MYR " + deduction);
        y = pdfLine(canvas, paint, y, t("estimateTitle") + ": MYR " + Math.max(0, basePrice - deduction));
        y += 10;

        paint.setFakeBoldText(true);
        y = pdfLine(canvas, paint, y, t("conditionSummary"));
        paint.setFakeBoldText(false);
        y = pdfLine(canvas, paint, y, t("overallCondition") + ": " + conditionValue(overallConditionSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("screenScratch") + ": " + conditionValue(screenScratchSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("backCoverCondition") + ": " + conditionValue(backCoverSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("conditionDeduction") + ": MYR " + conditionDeduction());
        y += 10;

        paint.setFakeBoldText(true);
        y = pdfLine(canvas, paint, y, t("cameraDetailSummary"));
        paint.setFakeBoldText(false);
        y = pdfLine(canvas, paint, y, t("backCamera") + ": " + (backCameraChecked ? (backCameraPassed ? t("pass") : t("fail")) : t("notTested")));
        y = pdfLine(canvas, paint, y, t("frontCamera") + ": " + (frontCameraChecked ? (frontCameraPassed ? t("pass") : t("fail")) : t("notTested")));
        y = pdfLine(canvas, paint, y, t("cameraOpenCondition") + ": " + conditionValue(cameraOpenSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("ultraWide05") + ": " + conditionValue(ultraWideSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("zoomInOut") + ": " + conditionValue(zoomSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("cameraBlackSpots") + ": " + conditionValue(cameraSpotSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("cameraFogWatermark") + ": " + conditionValue(cameraFogSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("cameraFocusBlur") + ": " + conditionValue(cameraFocusSpinner, t("conditionNotSet")));
        y = pdfLine(canvas, paint, y, t("cameraDetailDeduction") + ": MYR " + cameraDetailDeduction());
        y += 10;

        paint.setFakeBoldText(true);
        y = pdfLine(canvas, paint, y, t("testSummary"));
        paint.setFakeBoldText(false);
        for (String key : new String[]{"camera", "flash", "microphone", "speaker", "vibration", "wifi", "bluetooth", "gps", "battery", "sensors", "proximity", "nfc", "simSlot", "telcoLock", "biometric", "touch", "screen", "buttons"}) {
            Result result = result(key);
            y = pdfLine(canvas, paint, y, labelFor(key) + ": " + result.status.label(this) + " - " + result.note);
        }
        if (!blankToDash(notes).equals("-")) {
            y += 10;
            y = pdfLine(canvas, paint, y, t("notes") + ": " + blankToDash(notes));
        }
        y += 10;
        y = pdfLine(canvas, paint, y, t("disclaimer"));

        document.finishPage(page);
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
        File file = new File(dir, "phone-check-report-" + stamp + ".pdf");
        FileOutputStream out = new FileOutputStream(file);
        document.writeTo(out);
        out.close();
        document.close();
        return file;
    }

    private int pdfLine(Canvas canvas, Paint paint, int y, String text) {
        if (text == null) text = "";
        int maxChars = 82;
        while (text.length() > maxChars) {
            canvas.drawText(text.substring(0, maxChars), 40, y, paint);
            text = text.substring(maxChars);
            y += 17;
        }
        canvas.drawText(text, 40, y, paint);
        return y + 17;
    }

    private void showSubmit() {
        baseScreen(t("submitTitle"));
        LinearLayout card = card();
        card.addView(body(t("submitBody")));
        card.addView(label(t("name")));
        EditText nameInput = input("Ali Tan");
        card.addView(nameInput);
        card.addView(label(t("phone")));
        EditText phoneInput = input("+60");
        card.addView(phoneInput);
        card.addView(label(t("notes")));
        EditText notesInput = input(t("notesHint"));
        card.addView(notesInput);
        root.addView(card);

        Button send = primary(t("sendWhatsapp"));
        send.setOnClickListener(v -> openWhatsAppReview(
                nameInput.getText().toString(),
                phoneInput.getText().toString(),
                notesInput.getText().toString()
        ));
        root.addView(send);
    }

    private void openWhatsAppReview(String name, String phone, String notes) {
        File report;
        try {
            report = createPdfReport(name, phone, notes);
        } catch (Exception e) {
            Toast.makeText(this, t("pdfFailed"), Toast.LENGTH_LONG).show();
            return;
        }
        Uri reportUri = Uri.parse("content://" + getPackageName() + ".pdfprovider/" + report.getName());
        String message = t("whatsappPdfCaption");
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_TEXT, message);
        send.putExtra(Intent.EXTRA_STREAM, reportUri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setPackage("com.whatsapp");
        try {
            startActivity(send);
            return;
        } catch (Exception ignored) {
        }

        Intent business = new Intent(Intent.ACTION_SEND);
        business.setType("application/pdf");
        business.putExtra(Intent.EXTRA_TEXT, message);
        business.putExtra(Intent.EXTRA_STREAM, reportUri);
        business.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        business.setPackage("com.whatsapp.w4b");
        try {
            startActivity(business);
            return;
        } catch (Exception ignored) {
        }

        try {
            Intent chooser = Intent.createChooser(send, t("sendWhatsapp"));
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, t("whatsappMissing"), Toast.LENGTH_LONG).show();
        }
    }

    private void showDeveloperConsole() {
        internalHardwareMode = false;
        baseScreen(t("developerConsole"));
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinearLayout config = card();
        config.addView(title(t("onlineTestSettings")));
        config.addView(body(t("onlineTestSettingsBody")));
        config.addView(label(t("netlifyVersionUrl")));
        EditText updateUrl = input(DEFAULT_UPDATE_URL);
        updateUrl.setText(onlineSetting(PREF_UPDATE_URL, DEFAULT_UPDATE_URL));
        config.addView(updateUrl);
        config.addView(label(t("appsScriptUploadUrl")));
        EditText uploadUrl = input(DEFAULT_UPLOAD_URL);
        uploadUrl.setText(onlineSetting(PREF_UPLOAD_URL, DEFAULT_UPLOAD_URL));
        config.addView(uploadUrl);
        Button save = primary(t("saveOnlineSettings"));
        save.setOnClickListener(v -> {
            prefs.edit()
                    .putString(PREF_UPDATE_URL, updateUrl.getText().toString().trim())
                    .putString(PREF_UPLOAD_URL, uploadUrl.getText().toString().trim())
                    .apply();
            appendDebugLog("developer_settings_saved", "updateUrl=" + updateUrl.getText().toString().trim().length()
                    + ", uploadUrl=" + uploadUrl.getText().toString().trim().length());
            Toast.makeText(this, t("saved"), Toast.LENGTH_SHORT).show();
        });
        config.addView(save);
        root.addView(config);

        LinearLayout device = card();
        device.addView(title(t("currentTestDevice")));
        device.addView(row("App", APP_VERSION_LABEL));
        device.addView(row(t("brand"), clean(Build.MANUFACTURER)));
        device.addView(row(t("model"), clean(Build.MODEL)));
        device.addView(row("Android", Build.VERSION.RELEASE));
        device.addView(row(t("logFile"), getDebugLogFile().getAbsolutePath()));
        root.addView(device);

        LinearLayout actions = card();
        actions.addView(title(t("actions")));
        developerStatusView = body(t("ready"));
        actions.addView(developerStatusView);
        Button upload = primary(t("uploadCompatibilityReport"));
        upload.setOnClickListener(v -> uploadCompatibilityReport());
        actions.addView(upload);
        Button update = primary(t("checkApkUpdate"));
        update.setOnClickListener(v -> checkOnlineUpdate());
        actions.addView(update);
        Button openUpdateUrl = secondary(t("openUpdateUrl"));
        openUpdateUrl.setOnClickListener(v -> openExternalUrl(DEFAULT_UPDATE_URL));
        actions.addView(openUpdateUrl);
        Button autoUpload = secondary(autoUploadEnabled() ? t("autoUploadOn") : t("autoUploadOff"));
        autoUpload.setOnClickListener(v -> {
            boolean next = !autoUploadEnabled();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(PREF_AUTO_UPLOAD, next).apply();
            appendDebugLog("auto_upload_toggle", String.valueOf(next));
            Toast.makeText(this, next ? t("autoUploadEnabled") : t("autoUploadDisabled"), Toast.LENGTH_LONG).show();
            showDeveloperConsole();
        });
        actions.addView(autoUpload);
        Button internal = primary(t("internalHardwarePanel"));
        internal.setOnClickListener(v -> showInternalHardwareTestPanel());
        actions.addView(internal);
        Button share = secondary(t("shareLocalDebugLog"));
        share.setOnClickListener(v -> shareDebugLog());
        actions.addView(share);
        Button clear = secondary(t("clearLocalDebugLog"));
        clear.setOnClickListener(v -> {
            boolean deleted = getDebugLogFile().delete();
            developerStatusView.setText(deleted ? t("localLogCleared") : t("noLogToClear"));
        });
        actions.addView(clear);
        root.addView(actions);

        LinearLayout preview = card();
        preview.addView(title(t("recentLogPreview")));
        preview.addView(body(readRecentDebugLog()));
        root.addView(preview);
    }

    private void showInternalHardwareTestPanel() {
        internalHardwareMode = true;
        baseScreen(t("internalHardwareTest"));
        appendDebugLog("internal_panel_opened", "manual compatibility test");

        LinearLayout intro = card();
        intro.addView(title(t("fastCompatibilityTesting")));
        intro.addView(body(t("fastCompatibilityBody")));
        intro.addView(row(t("deviceName"), clean(Build.MANUFACTURER) + " " + clean(Build.MODEL)));
        intro.addView(row("Android", Build.VERSION.RELEASE + " / SDK " + Build.VERSION.SDK_INT));
        intro.addView(row("App", APP_VERSION_LABEL));
        root.addView(intro);

        LinearLayout quick = card();
        quick.addView(title(t("jumpToTest")));
        addInternalJumpButton(quick, 1, t("wifi"));
        addInternalJumpButton(quick, 2, t("bluetooth"));
        addInternalJumpButton(quick, 3, t("gps"));
        addInternalJumpButton(quick, 4, t("sensors"));
        addInternalJumpButton(quick, 5, t("proximity"));
        addInternalJumpButton(quick, 6, t("nfc"));
        addInternalJumpButton(quick, 9, t("biometric"));
        addInternalJumpButton(quick, 10, t("microphone"));
        addInternalJumpButton(quick, 11, t("speaker"));
        addInternalJumpButton(quick, 12, t("camera"));
        addInternalJumpButton(quick, 13, t("touch"));
        addInternalJumpButton(quick, 14, t("screen"));
        addInternalJumpButton(quick, 15, t("buttons"));
        root.addView(quick);

        LinearLayout noteCard = card();
        noteCard.addView(title(t("recordIssue")));
        noteCard.addView(body(t("recordIssueBody")));
        EditText issue = input(t("recordIssueHint"));
        issue.setSingleLine(false);
        noteCard.addView(issue);
        Button saveIssue = primary(t("saveIssueToLog"));
        saveIssue.setOnClickListener(v -> {
            String text = issue.getText().toString().trim();
            if (text.length() == 0) text = t("noIssueTextEntered");
            appendDebugLog("manual_issue", text);
            Toast.makeText(this, t("issueSavedToLog"), Toast.LENGTH_SHORT).show();
            showInternalHardwareTestPanel();
        });
        noteCard.addView(saveIssue);
        root.addView(noteCard);

        LinearLayout actions = card();
        actions.addView(title(t("reportActions")));
        Button upload = primary(t("uploadThisPhoneReport"));
        upload.setOnClickListener(v -> uploadCompatibilityReport());
        actions.addView(upload);
        Button share = secondary(t("shareDebugLog"));
        share.setOnClickListener(v -> shareDebugLog());
        actions.addView(share);
        Button back = secondary(t("backToDeveloperConsole"));
        back.setOnClickListener(v -> showDeveloperConsole());
        actions.addView(back);
        root.addView(actions);

        LinearLayout preview = card();
        preview.addView(title(t("recentEvents")));
        preview.addView(body(readRecentDebugLog()));
        root.addView(preview);
    }

    private void addInternalJumpButton(LinearLayout parent, int step, String label) {
        Button button = secondary(label);
        button.setOnClickListener(v -> {
            appendDebugLog("internal_jump", "step=" + step + ", label=" + label);
            internalHardwareMode = true;
            currentTestStep = step;
            showTestStep();
        });
        parent.addView(button);
    }

    private void uploadCompatibilityReport() {
        String endpoint = onlineSetting(PREF_UPLOAD_URL, DEFAULT_UPLOAD_URL);
        if (endpoint.length() == 0) {
            if (developerStatusView != null) developerStatusView.setText("Upload URL missing.");
            return;
        }
        if (!hasInternetConnection()) {
            String message = t("noInternetUpload");
            appendDebugLog("upload_blocked_no_internet", endpoint);
            if (developerStatusView != null) developerStatusView.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }
        if (developerStatusView != null) developerStatusView.setText("Uploading report...");
        appendDebugLog("upload_start", endpoint);
        uploadCompatibilityReportAsync(endpoint, true);
    }

    private void uploadCompatibilityReportAsync(String endpoint, boolean showUi) {
        if (uploadInProgress) {
            appendDebugLog("upload_skipped", "upload already in progress");
            return;
        }
        uploadInProgress = true;
        new Thread(() -> {
            try {
                JSONObject payload = buildCompatibilityPayload();
                byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                OutputStream out = connection.getOutputStream();
                out.write(body);
                out.close();
                int code = connection.getResponseCode();
                appendDebugLog("upload_result", "http=" + code);
                runOnUiThread(() -> {
                    uploadInProgress = false;
                    if (showUi && developerStatusView != null) developerStatusView.setText("Upload finished. HTTP " + code);
                });
            } catch (Exception e) {
                appendDebugLog("upload_failed", e.toString());
                runOnUiThread(() -> {
                    uploadInProgress = false;
                    if (showUi && developerStatusView != null) developerStatusView.setText("Upload failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private boolean autoUploadEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PREF_AUTO_UPLOAD, false);
    }

    private void maybeAutoUploadReport(String reason) {
        if (!autoUploadEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastAutoUploadMs < 3500) {
            appendDebugLog("auto_upload_debounced", reason);
            return;
        }
        String endpoint = onlineSetting(PREF_UPLOAD_URL, DEFAULT_UPLOAD_URL);
        if (endpoint.length() == 0) {
            appendDebugLog("auto_upload_missing_url", reason);
            return;
        }
        if (!hasInternetConnection()) {
            appendDebugLog("auto_upload_no_internet", reason);
            return;
        }
        lastAutoUploadMs = now;
        appendDebugLog("auto_upload_start", reason);
        uploadCompatibilityReportAsync(endpoint, false);
    }

    private boolean hasInternetConnection() {
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (manager == null) return false;
            if (Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
                return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
            return manager.getActiveNetworkInfo() != null && manager.getActiveNetworkInfo().isConnected();
        } catch (Exception e) {
            appendDebugLog("network_check_failed", e.toString());
            return false;
        }
    }

    private JSONObject buildCompatibilityPayload() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("sessionId", debugSessionId());
        payload.put("createdAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
        payload.put("appVersionCode", APP_VERSION_CODE);
        payload.put("appVersion", APP_VERSION_LABEL);
        payload.put("brand", clean(Build.MANUFACTURER));
        payload.put("model", clean(Build.MODEL));
        payload.put("android", Build.VERSION.RELEASE);
        payload.put("sdk", Build.VERSION.SDK_INT);
        payload.put("currentStep", currentTestStep);
        payload.put("internalTestMode", developerUnlocked);
        JSONArray resultArray = new JSONArray();
        for (String key : new String[]{"camera", "flash", "microphone", "speaker", "vibration", "wifi", "bluetooth", "gps", "battery", "sensors", "proximity", "nfc", "simSlot", "telcoLock", "biometric", "touch", "screen", "buttons"}) {
            Result result = result(key);
            JSONObject item = new JSONObject();
            item.put("key", key);
            item.put("label", labelFor(key));
            item.put("status", result.status.name());
            item.put("note", result.note);
            resultArray.put(item);
        }
        payload.put("results", resultArray);
        payload.put("log", readDebugLog());
        return payload;
    }

    private void checkOnlineUpdate() {
        String endpoint = DEFAULT_UPDATE_URL;
        if (endpoint.length() == 0) {
            if (developerStatusView != null) developerStatusView.setText("version.json URL missing.");
            Toast.makeText(this, "version.json URL missing", Toast.LENGTH_LONG).show();
            return;
        }
        if (developerStatusView != null) developerStatusView.setText("Checking update...");
        Toast.makeText(this, t("checkingUpdate"), Toast.LENGTH_SHORT).show();
        appendDebugLog("update_check_start", endpoint);
        new Thread(() -> {
            try {
                String json = httpGet(endpoint);
                JSONObject remote = new JSONObject(json);
                int latest = remote.optInt("latestVersionCode", 0);
                String versionName = remote.optString("versionName", "");
                String apkUrl = remote.optString("apkUrl", "");
                String notes = remote.optString("notes", "");
                appendDebugLog("update_check_result", "latest=" + latest + ", name=" + versionName);
                runOnUiThread(() -> {
                    if (latest > APP_VERSION_CODE && apkUrl.length() > 0) {
                        if (developerStatusView != null) {
                            developerStatusView.setText("New version found: " + versionName + "\n" + notes + "\nDownloading APK...");
                        }
                        Toast.makeText(this, t("newVersionDownloading") + " " + versionName, Toast.LENGTH_LONG).show();
                        downloadAndInstallApk(apkUrl, versionName);
                    } else if (developerStatusView != null) {
                        developerStatusView.setText("No newer APK found.\nLatest: " + versionName);
                        Toast.makeText(this, t("noNewerApk"), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                appendDebugLog("update_check_failed", e.toString());
                runOnUiThread(() -> {
                    String message = "Update check failed: " + e.getMessage();
                    if (developerStatusView != null) developerStatusView.setText(message + "\nURL: " + DEFAULT_UPDATE_URL);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void downloadAndInstallApk(String apkUrl, String versionName) {
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(apkUrl).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);
                InputStream in = connection.getInputStream();
                File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                if (dir == null) dir = getFilesDir();
                if (!dir.exists()) dir.mkdirs();
                File apk = new File(dir, "phone-check-update-" + versionName.replaceAll("[^A-Za-z0-9._-]", "-") + ".apk");
                FileOutputStream out = new FileOutputStream(apk);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.close();
                in.close();
                appendDebugLog("apk_downloaded", apk.getAbsolutePath() + ", bytes=" + apk.length());
                runOnUiThread(() -> {
                    if (developerStatusView != null) developerStatusView.setText("APK downloaded. Opening installer...");
                    Toast.makeText(this, t("apkDownloadedOpening"), Toast.LENGTH_LONG).show();
                    installDownloadedApk(apk);
                });
            } catch (Exception e) {
                appendDebugLog("apk_download_failed", e.toString());
                runOnUiThread(() -> {
                    String message = "APK download failed: " + e.getMessage();
                    if (developerStatusView != null) developerStatusView.setText(message + "\nURL: " + apkUrl);
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    openExternalUrl(apkUrl);
                });
            }
        }).start();
    }

    private void openExternalUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            appendDebugLog("open_url_failed", url + " | " + e.toString());
            Toast.makeText(this, "Cannot open URL", Toast.LENGTH_LONG).show();
        }
    }

    private void installDownloadedApk(File apk) {
        try {
            if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
                appendDebugLog("apk_install_permission_needed", "request unknown app install permission");
                if (developerStatusView != null) {
                    developerStatusView.setText("Please allow Install unknown apps for this app, then tap Check APK update again.");
                }
                Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                settings.setData(Uri.parse("package:" + getPackageName()));
                startActivity(settings);
                return;
            }
            Uri apkUri = Uri.parse("content://" + getPackageName() + ".pdfprovider/" + apk.getName());
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(install);
        } catch (Exception e) {
            appendDebugLog("apk_install_open_failed", e.toString());
            Toast.makeText(this, "Cannot open APK installer", Toast.LENGTH_LONG).show();
        }
    }

    private String httpGet(String endpoint) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(12000);
        InputStream stream = connection.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        stream.close();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private String onlineSetting(String key, String fallback) {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, "").trim();
        return value.length() == 0 ? fallback : value;
    }

    private void shareDebugLog() {
        try {
            File file = getDebugLogFile();
            if (!file.exists()) appendDebugLog("share_log", "created empty log before share");
            Uri uri = Uri.parse("content://" + getPackageName() + ".pdfprovider/" + file.getName());
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, "Share debug log"));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share log", Toast.LENGTH_LONG).show();
        }
    }

    private String buildWhatsAppMessage(String name, String phone, String notes) {
        int finalPrice = Math.max(0, basePrice - deduction);
        StringBuilder out = new StringBuilder();
        out.append(t("whatsappTitle")).append("\n");
        out.append(t("name")).append(": ").append(blankToDash(name)).append("\n");
        out.append(t("phone")).append(": ").append(blankToDash(phone)).append("\n");
        out.append(t("brand")).append(": ").append(clean(Build.MANUFACTURER)).append("\n");
        out.append(t("model")).append(": ").append(clean(Build.MODEL)).append("\n");
        out.append("Android: ").append(Build.VERSION.RELEASE).append("\n");
        out.append(t("matched")).append(" ").append(selected(ramSpinner)).append(" / ").append(selected(romSpinner)).append("\n");
        out.append(t("estimateTitle")).append(": MYR ").append(finalPrice).append("\n");
        out.append(t("deduction")).append(": MYR ").append(deduction).append("\n\n");
        out.append(t("conditionSummary")).append("\n");
        out.append("- ").append(t("overallCondition")).append(": ").append(conditionValue(overallConditionSpinner, t("conditionNotSet"))).append("\n");
        out.append("- ").append(t("screenScratch")).append(": ").append(conditionValue(screenScratchSpinner, t("conditionNotSet"))).append("\n");
        out.append("- ").append(t("backCoverCondition")).append(": ").append(conditionValue(backCoverSpinner, t("conditionNotSet"))).append("\n\n");
        out.append(t("testSummary")).append("\n");
        for (String key : new String[]{"camera", "flash", "microphone", "speaker", "vibration", "wifi", "bluetooth", "gps", "battery", "sensors", "proximity", "nfc", "simSlot", "telcoLock", "biometric", "touch", "screen", "buttons"}) {
            Result result = result(key);
            out.append("- ").append(labelFor(key)).append(": ").append(result.status.label(this)).append(" - ").append(result.note).append("\n");
        }
        if (notes != null && notes.trim().length() > 0) {
            out.append("\n").append(t("notes")).append(": ").append(notes.trim()).append("\n");
        }
        out.append("\n").append(t("disclaimer"));
        return out.toString();
    }

    private String blankToDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private void startMicrophoneSample() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            put("microphone", Status.REVIEW, t("micPermission"));
            if (micStatusView != null) micStatusView.setText(t("micPermission"));
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 16);
            return;
        }
        stopMicSampling();
        micSampling = true;
        File file = new File(getCacheDir(), "mic-test-" + System.currentTimeMillis() + ".m4a");
        final MediaRecorder recorder = new MediaRecorder();
        final int[] maxAmp = new int[]{0};
        final int[] samples = new int[]{0};
        final int[] activeSamples = new int[]{0};
        final long started = System.currentTimeMillis();
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioSamplingRate(44100);
            recorder.setOutputFile(file.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            if (micStatusView != null) micStatusView.setText(t("micListening"));
            Runnable poll = new Runnable() {
                @Override
                public void run() {
                    if (!micSampling) return;
                    try {
                        int amp = recorder.getMaxAmplitude();
                        if (amp > maxAmp[0]) maxAmp[0] = amp;
                        samples[0]++;
                        if (amp > 900) activeSamples[0]++;
                    } catch (Exception ignored) {
                    }
                    int secondsLeft = Math.max(0, 5 - (int) ((System.currentTimeMillis() - started) / 1000));
                    if (micStatusView != null) {
                        micStatusView.setText(t("micListening") + "\n" + t("micAmplitude") + " " + maxAmp[0] + "\n" + t("secondsLeft") + " " + secondsLeft);
                    }
                    handler.postDelayed(this, 250);
                }
            };
            handler.post(poll);
            handler.postDelayed(() -> {
                try {
                    recorder.stop();
                } catch (Exception ignored) {
                }
                try {
                    recorder.release();
                } catch (Exception ignored) {
                }
                micSampling = false;
                int activePercent = samples[0] > 0 ? activeSamples[0] * 100 / samples[0] : 0;
                Status status = maxAmp[0] > 1200 && activePercent >= 10 ? Status.REVIEW : Status.FAIL;
                String note = t("micRecordedSummary") + "\n" + t("micAmplitude") + " " + maxAmp[0] + "\n" + t("micActiveSound") + " " + activePercent + "%";
                put("microphone", status, note);
                if (micStatusView != null) micStatusView.setText(note + "\n" + t("micPlaybackNow"));
                playRecordedMicFile(file);
            }, 5200);
        } catch (Exception e) {
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            put("microphone", Status.REVIEW, t("micFailed"));
            micSampling = false;
            if (micStatusView != null) micStatusView.setText(t("micFailed"));
        }
    }

    private void playRecordedMicFile(File file) {
        if (file == null || !file.exists() || file.length() == 0) {
            if (micStatusView != null) micStatusView.setText(t("micPlaybackFailed"));
            return;
        }
        try {
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(true);
            }
            MediaPlayer player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(mp -> {
                try {
                    mp.release();
                } catch (Exception ignored) {
                }
            });
            player.setOnErrorListener((mp, what, extra) -> {
                try {
                    mp.release();
                } catch (Exception ignored) {
                }
                if (micStatusView != null) micStatusView.setText(t("micPlaybackFailed"));
                return true;
            });
            player.prepare();
            player.start();
        } catch (Exception e) {
            if (micStatusView != null) micStatusView.setText(t("micPlaybackFailed"));
        }
    }

    private void stopMicSampling() {
        micSampling = false;
    }

    private void playPcmSample(byte[] audio, int sampleRate) {
        if (audio == null || audio.length == 0) return;
        new Thread(() -> {
            AudioTrack track = null;
            try {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                    audioManager.setSpeakerphoneOn(true);
                }
                int min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, audio.length), AudioTrack.MODE_STATIC);
                track.write(audio, 0, audio.length);
                track.play();
                long durationMs = Math.max(700, audio.length / 2L * 1000L / sampleRate);
                Thread.sleep(durationMs + 300);
                track.stop();
            } catch (Exception ignored) {
            } finally {
                if (track != null) {
                    try {
                        track.release();
                    } catch (Exception ignored) {
                    }
                }
            }
        }).start();
    }

    private void runSpeakerTest() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (speakerStatusView != null) speakerStatusView.setText(t("speakerMicPermission"));
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 17);
            return;
        }
        if (speakerStatusView != null) speakerStatusView.setText(t("speakerTesting"));
        new Thread(() -> {
            AudioTrack track = null;
            AudioRecord record = null;
            try {
                int sampleRate = 44100;
                int seconds = 5;
                int samples = sampleRate * seconds;
                byte[] data = new byte[samples * 2];
                int[] freqs = new int[]{220, 440, 880, 1200};
                for (int i = 0; i < samples; i++) {
                    int section = Math.min(freqs.length - 1, i / Math.max(1, (sampleRate * seconds / freqs.length)));
                    double angle = 2.0 * Math.PI * freqs[section] * i / sampleRate;
                    short value = (short) (Math.sin(angle) * 22000);
                    data[i * 2] = (byte) (value & 0xff);
                    data[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
                }
                int min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                track = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, data.length), AudioTrack.MODE_STREAM);
                int recBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (recBuffer < 2048) recBuffer = 2048;
                record = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recBuffer);
                short[] mic = new short[recBuffer / 2];
                final AudioTrack playbackTrack = track;
                Thread playback = new Thread(() -> {
                    try {
                        playbackTrack.play();
                        playbackTrack.write(data, 0, data.length);
                        playbackTrack.stop();
                    } catch (Exception ignored) {
                    }
                });
                record.startRecording();
                playback.start();
                long end = System.currentTimeMillis() + 5500;
                int maxLevel = 0;
                int chunks = 0;
                int activeChunks = 0;
                while (System.currentTimeMillis() < end) {
                    int read = record.read(mic, 0, mic.length);
                    int chunkMax = 0;
                    for (int i = 0; i < read; i++) {
                        int level = Math.abs(mic[i]);
                        if (level > maxLevel) maxLevel = level;
                        if (level > chunkMax) chunkMax = level;
                    }
                    if (read > 0) {
                        chunks++;
                        if (chunkMax > 900) activeChunks++;
                    }
                    final int live = maxLevel;
                    handler.post(() -> {
                        if (speakerStatusView != null) speakerStatusView.setText(t("speakerTesting") + "\n" + t("speakerMicLevel") + " " + live);
                    });
                }
                try {
                    playback.join(1000);
                } catch (Exception ignored) {
                }
                record.stop();
                int activePercent = chunks > 0 ? activeChunks * 100 / chunks : 0;
                int finalMaxLevel = maxLevel;
                Status status = finalMaxLevel > 1200 && activePercent >= 15 ? Status.REVIEW : Status.FAIL;
                String note = status == Status.FAIL
                        ? t("speakerNoOutputDetected") + "\n" + t("speakerMicLevel") + " " + finalMaxLevel + "\n" + t("micActiveSound") + " " + activePercent + "%"
                        : t("speakerOutputDetected") + "\n" + t("speakerMicLevel") + " " + finalMaxLevel + "\n" + t("micActiveSound") + " " + activePercent + "%\n" + t("speakerManualQuality");
                handler.post(() -> {
                    put("speaker", status, note);
                    if (speakerStatusView != null) speakerStatusView.setText(note);
                });
            } catch (Exception ignored) {
                handler.post(() -> {
                    put("speaker", Status.REVIEW, t("speakerFailed"));
                    if (speakerStatusView != null) speakerStatusView.setText(t("speakerFailed"));
                });
            } finally {
                if (record != null) {
                    try {
                        record.release();
                    } catch (Exception ignored) {
                    }
                }
                if (track != null) {
                    try {
                        track.release();
                    } catch (Exception ignored) {
                    }
                }
            }
        }).start();
    }

    private void startSensorTest() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) {
            put("sensors", Status.REVIEW, t("sensorMissing"));
            return;
        }
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            put("sensors", Status.REVIEW, t("sensorMissing"));
            return;
        }
        put("sensors", Status.REVIEW, t("sensorDirectionWaiting"));
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private Status cameraStatus() {
        return Status.REVIEW;
    }

    private String cameraNote() {
        return t("cameraSafeMode");
    }

    private String cameraDiagnostics() {
        return t("cameraSafeMode");
    }

    private Status flashStatus() {
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (manager == null) return Status.REVIEW;
            for (String id : manager.getCameraIdList()) {
                Boolean flash = manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) return Status.PASS;
            }
            return Status.REVIEW;
        } catch (Exception e) {
            return Status.REVIEW;
        }
    }

    private String flashNote() {
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (manager == null) return t("flashReview");
            for (String id : manager.getCameraIdList()) {
                Boolean flash = manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (Boolean.TRUE.equals(flash)) {
                    manager.setTorchMode(id, true);
                    handler.postDelayed(() -> {
                        try {
                            manager.setTorchMode(id, false);
                        } catch (Exception ignored) {
                        }
                    }, 600);
                    return t("flashBlink");
                }
            }
        } catch (Exception ignored) {
        }
        return t("flashReview");
    }

    private boolean runVibrationTest() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator == null || !vibrator.hasVibrator()) return false;
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 250, 120, 450}, -1));
            } else {
                vibrator.vibrate(new long[]{0, 250, 120, 450}, -1);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean wifiOk() {
        try {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) return false;
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null && Build.VERSION.SDK_INT >= 23) {
                NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            return manager != null && manager.isWifiEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private String wifiStatus() {
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) return t("wifiMissing");
        if (manager != null && !manager.isWifiEnabled()) return t("wifiOff");
        return wifiOk() ? t("wifiConnected") : t("wifiNoNetwork");
    }

    private Status wifiScanStatus(boolean scanStarted) {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) return Status.FAIL;
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (manager == null || !manager.isWifiEnabled()) return Status.REVIEW;
        int networks = nearbyWifiCount();
        return scanStarted || networks >= 0 ? Status.PASS : Status.FAIL;
    }

    private boolean hasWifiScanPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestWifiScanPermission() {
        if (Build.VERSION.SDK_INT < 23) return;
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, 15);
            return;
        }
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 15);
    }

    private String wifiDiagnosticNote() {
        return wifiDiagnosticNote(false);
    }

    private String wifiDiagnosticNote(boolean scanStarted) {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) return t("wifiMissing");
        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        boolean enabled = manager != null && manager.isWifiEnabled();
        int networks = nearbyWifiCount();
        String scanText;
        if (networks >= 0) {
            scanText = t("wifiScanFound") + " " + networks;
        } else {
            scanText = t("wifiScanBlocked");
        }
        if (!enabled) return t("wifiOpenRequired") + "\n" + scanText;
        if (scanStarted) return t("wifiScanStartedPass") + "\n" + scanText;
        if (networks >= 0) return t("wifiScanPass") + "\n" + scanText;
        return t("wifiScanFailed") + "\n" + scanText;
    }

    private int nearbyWifiCount() {
        try {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI)) return -1;
            if (Build.VERSION.SDK_INT >= 23 && !hasLocationPermission()) {
                return -1;
            }
            WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (manager == null) return -1;
            List<ScanResult> results = manager.getScanResults();
            return results == null ? 0 : results.size();
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean bluetoothOk() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        } catch (SecurityException e) {
            return false;
        }
    }

    private String bluetoothStatus() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return t("btMissing");
        return bluetoothOk() ? t("btOn") : t("btReview");
    }

    private boolean hasBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothScanPermission() {
        if (Build.VERSION.SDK_INT < 23) return;
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, 14);
            return;
        }
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, 14);
    }

    private Status bluetoothFunctionStatus(boolean scanStarted) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return Status.FAIL;
        try {
            if (!adapter.isEnabled()) return Status.REVIEW;
            return scanStarted ? Status.PASS : Status.FAIL;
        } catch (Exception e) {
            return Status.REVIEW;
        }
    }

    private String bluetoothDiagnosticNote(boolean scanStarted) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return t("btMissing");
        try {
            if (!adapter.isEnabled()) return t("btOpenRequired");
            if (scanStarted) return t("btScanPass");
            return t("btScanFailed");
        } catch (Exception e) {
            return t("btPermissionBlocked");
        }
    }

    private boolean hasConnectedBluetoothProfile(BluetoothAdapter adapter) {
        try {
            return adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED
                    || adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                    || adapter.getProfileConnectionState(BluetoothProfile.HEALTH) == BluetoothProfile.STATE_CONNECTED;
        } catch (Exception e) {
            return false;
        }
    }

    private int pairedBluetoothCount(BluetoothAdapter adapter) {
        try {
            return adapter.getBondedDevices() == null ? 0 : adapter.getBondedDevices().size();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean gpsOk() {
        try {
            int mode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            return mode != Settings.Secure.LOCATION_MODE_OFF;
        } catch (Exception e) {
            return false;
        }
    }

    private String gpsStatus() {
        return gpsOk() ? t("gpsOn") : t("gpsReview");
    }

    private String locationDiagnosticNote() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) return t("gpsReview");
        if (!isAnyLocationProviderEnabled(manager)) return t("gpsOpenRequired");
        boolean gps = false;
        boolean network = false;
        try {
            gps = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }
        return t("gpsReadyToTest") + "\n"
                + "GPS: " + (gps ? t("gpsProviderOn") : t("gpsProviderOff")) + "\n"
                + t("networkLocation") + ": " + (network ? t("gpsProviderOn") : t("gpsProviderOff"));
    }

    private boolean isAnyLocationProviderEnabled(LocationManager manager) {
        try {
            return manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private Location bestLastKnownLocation(LocationManager manager) {
        if (Build.VERSION.SDK_INT >= 23 && !hasLocationPermission()) {
            return null;
        }
        Location best = null;
        try {
            Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (gps != null) best = gps;
            if (network != null && (best == null || network.getTime() > best.getTime())) best = network;
        } catch (Exception ignored) {
        }
        return best;
    }

    private void saveLocationPass(Location location, String prefix) {
        String note = formatLocationNote(prefix, location);
        put("gps", Status.PASS, note);
        if (locationStatusView != null) locationStatusView.setText(note);
        if (currentTestStep == 3 && root != null && !locationContinueShown) {
            locationContinueShown = true;
            Button next = primary(t("locationContinue"));
            next.setOnClickListener(v -> nextTestStepIfStillOn(3));
            root.addView(next);
        }
    }

    private String locationProviderPassText(Location location) {
        if (location != null && LocationManager.GPS_PROVIDER.equals(location.getProvider())) {
            return t("gpsProviderPass");
        }
        if (location != null && LocationManager.NETWORK_PROVIDER.equals(location.getProvider())) {
            return t("gpsNetworkPass");
        }
        return t("gpsLivePass");
    }

    private String formatLocationNote(String prefix, Location location) {
        if (location == null) return prefix;
        return prefix + "\n" + t("gpsAccuracy") + " "
                + String.format(Locale.US, "%.0f", location.getAccuracy()) + "m";
    }

    private boolean batteryOk() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return level >= 15;
    }

    private String batteryStatus() {
        BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return level + "%";
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackgroundResource(getResources().getIdentifier("card_bg", "drawable", getPackageName()));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        return card;
    }

    private TextView title(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(INK);
        view.setTextSize(19);
        view.setTypeface(null, 1);
        view.setPadding(0, 0, 0, dp(10));
        return view;
    }

    private TextView body(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(MUTED);
        view.setTextSize(14);
        view.setLineSpacing(dp(2), 1.12f);
        view.setPadding(0, 0, 0, dp(14));
        return view;
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(INK);
        view.setTextSize(13);
        view.setTypeface(null, 1);
        view.setPadding(0, dp(10), 0, dp(4));
        return view;
    }

    private TextView row(String left, String right) {
        TextView view = new TextView(this);
        view.setText(left + "\n" + right);
        view.setTextColor(INK);
        view.setTextSize(15);
        view.setLineSpacing(dp(2), 1.05f);
        view.setPadding(0, dp(8), 0, dp(10));
        return view;
    }

    private TextView testLine(String key, String name) {
        Result result = result(key);
        TextView view = row(statusIcon(result.status) + " " + name, result.status.label(this) + " - " + result.note);
        view.setTextColor(result.status.color());
        view.setBackgroundColor(statusBackground(result.status));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        return view;
    }

    private String statusIcon(Status status) {
        if (status == Status.PASS) return "OK";
        if (status == Status.FAIL) return "!";
        if (status == Status.REVIEW) return "?";
        return "-";
    }

    private int statusBackground(Status status) {
        if (status == Status.PASS) return SOFT_GREEN;
        if (status == Status.FAIL) return SOFT_RED;
        if (status == Status.REVIEW) return SOFT_AMBER;
        return SURFACE;
    }

    private Button primary(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(null, 1);
        button.setBackgroundResource(getResources().getIdentifier("primary_button", "drawable", getPackageName()));
        button.setMinHeight(dp(50));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        button.setLayoutParams(params);
        return button;
    }

    private Button secondary(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(INK);
        button.setTextSize(15);
        button.setBackgroundResource(getResources().getIdentifier("secondary_button", "drawable", getPackageName()));
        button.setMinHeight(dp(48));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        button.setLayoutParams(params);
        return button;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        return input;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        spinner.setPadding(0, dp(4), 0, dp(8));
        return spinner;
    }

    private void selectSpinnerValue(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (value.equals(spinner.getItemAtPosition(i).toString())) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String selected(Spinner spinner) {
        if (spinner == null || spinner.getSelectedItem() == null) return "";
        return spinner.getSelectedItem().toString();
    }

    private int estimateRamGb() {
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        manager.getMemoryInfo(info);
        long gb = Math.round(info.totalMem / 1024.0 / 1024.0 / 1024.0);
        if (gb <= 4) return 4;
        if (gb <= 6) return 6;
        if (gb <= 8) return 8;
        if (gb <= 12) return 12;
        return 16;
    }

    private int estimateRomGb() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        long gb = Math.round(stat.getTotalBytes() / 1024.0 / 1024.0 / 1024.0);
        if (gb <= 80) return 64;
        if (gb <= 160) return 128;
        if (gb <= 320) return 256;
        if (gb <= 700) return 512;
        return 1024;
    }

    private void matchPrice() {
        try {
            String json = readAsset("price-data.json");
            JSONArray items = new JSONObject(json).getJSONArray("items");
            int ram = numberFrom(selected(ramSpinner));
            int rom = numberFrom(selected(romSpinner));
            String brand = clean(Build.MANUFACTURER).toLowerCase(Locale.US);
            String model = clean(Build.MODEL).toLowerCase(Locale.US);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (!item.optBoolean("enabled", false)) continue;
                String rowBrand = item.optString("brand").toLowerCase(Locale.US);
                String rowModel = item.optString("model").toLowerCase(Locale.US);
                if (brand.contains(rowBrand) && model.contains(rowModel)
                        && item.optInt("ramGb") == ram && item.optInt("romGb") == rom) {
                    basePrice = item.optInt("basePriceMyr", basePrice);
                    return;
                }
            }
        } catch (Exception ignored) {
            basePrice = 1200;
        }
    }

    private void calculateDeduction() {
        int fail = 0;
        int review = 0;
        for (Result result : results.values()) {
            if (result.status == Status.FAIL) fail++;
            if (result.status == Status.REVIEW) review++;
        }
        deduction = Math.min(basePrice / 2, fail * 180 + review * 60 + conditionDeduction() + cameraDetailDeduction());
    }

    private int cameraDetailDeduction() {
        int total = 0;
        String open = conditionValue(cameraOpenSpinner, "");
        String ultra = conditionValue(ultraWideSpinner, "");
        String zoom = conditionValue(zoomSpinner, "");
        String spots = conditionValue(cameraSpotSpinner, "");
        String fog = conditionValue(cameraFogSpinner, "");
        String focus = conditionValue(cameraFocusSpinner, "");

        if (backCameraChecked && !backCameraPassed) total += 220;
        if (frontCameraChecked && !frontCameraPassed) total += 160;
        if (open.equals(t("cameraCannotOpen"))) total += 260;
        if (ultra.equals(t("cameraProblem"))) total += 120;
        if (zoom.equals(t("cameraProblem"))) total += 100;
        if (spots.equals(t("minorSeen"))) total += 60;
        if (spots.equals(t("obviousSeen"))) total += 180;
        if (fog.equals(t("minorSeen"))) total += 80;
        if (fog.equals(t("obviousSeen"))) total += 220;
        if (focus.equals(t("minorSeen"))) total += 80;
        if (focus.equals(t("obviousSeen"))) total += 220;
        return total;
    }

    private boolean manualRearCameraProblem() {
        String open = conditionValue(cameraOpenSpinner, "");
        String ultra = conditionValue(ultraWideSpinner, "");
        String zoom = conditionValue(zoomSpinner, "");
        return open.equals(t("cameraCannotOpen"))
                || ultra.equals(t("cameraProblem"))
                || zoom.equals(t("cameraProblem"));
    }

    private String cameraDetailSummary() {
        if (ultraWideSpinner == null) return cameraNote();
        return t("backCamera") + "=" + (backCameraChecked ? (backCameraPassed ? t("pass") : t("fail")) : t("notTested"))
                + ", " + t("frontCamera") + "=" + (frontCameraChecked ? (frontCameraPassed ? t("pass") : t("fail")) : t("notTested"))
                + ", " + t("cameraOpenCondition") + "=" + conditionValue(cameraOpenSpinner, t("conditionNotSet"))
                + ", " + t("ultraWide05") + "=" + conditionValue(ultraWideSpinner, t("conditionNotSet"))
                + ", " + t("zoomInOut") + "=" + conditionValue(zoomSpinner, t("conditionNotSet"))
                + ", " + t("cameraBlackSpots") + "=" + conditionValue(cameraSpotSpinner, t("conditionNotSet"))
                + ", " + t("cameraFogWatermark") + "=" + conditionValue(cameraFogSpinner, t("conditionNotSet"))
                + ", " + t("cameraFocusBlur") + "=" + conditionValue(cameraFocusSpinner, t("conditionNotSet"));
    }

    private int conditionDeduction() {
        int total = 0;
        String overall = conditionValue(overallConditionSpinner, "");
        String screen = conditionValue(screenScratchSpinner, "");
        String back = conditionValue(backCoverSpinner, "");

        if (overall.equals(t("conditionGood"))) total += 40;
        if (overall.equals(t("conditionFair"))) total += 120;
        if (overall.equals(t("conditionPoor"))) total += 260;

        if (screen.equals(t("scratchLight"))) total += 30;
        if (screen.equals(t("scratchVisible"))) total += 90;
        if (screen.equals(t("scratchHeavy"))) total += 180;

        if (back.equals(t("backLightWear"))) total += 30;
        if (back.equals(t("backDents"))) total += 100;
        if (back.equals(t("backCracked"))) total += 220;
        return total;
    }

    private String conditionValue(Spinner spinner, String fallback) {
        if (spinner == null || spinner.getSelectedItem() == null) return fallback;
        return spinner.getSelectedItem().toString();
    }

    private String readAsset(String name) throws Exception {
        InputStream stream = getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private int numberFrom(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String clean(String value) {
        if (value == null || value.trim().isEmpty()) return "Unknown";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void put(String key, Status status, String note) {
        results.put(key, new Result(status, note));
        appendDebugLog("result_" + key, status.name() + " | " + note);
        maybeAutoUploadReport("result_" + key + "_" + status.name());
    }

    private Result result(String key) {
        Result result = results.get(key);
        return result == null ? new Result(Status.PENDING, t("notTested")) : result;
    }

    private String labelFor(String key) {
        if ("touch".equals(key)) return t("touch");
        if ("screen".equals(key)) return t("screen");
        if ("flash".equals(key)) return t("flash");
        if ("gps".equals(key)) return t("gps");
        if ("buttons".equals(key)) return t("buttons");
        return t(key);
    }

    private String t(String key) {
        String[] values = text.get(key);
        int index = "ms".equals(lang) ? 1 : "zh".equals(lang) ? 2 : 0;
        return values == null ? key : values[index];
    }

    private void add(String key, String en, String ms, String zh) {
        text.put(key, new String[]{en, ms, zh});
    }

    private File getDebugLogFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = getFilesDir();
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "phone-check-debug-log.txt");
    }

    private void appendDebugLog(String event, String detail) {
        try {
            FileOutputStream out = new FileOutputStream(getDebugLogFile(), true);
            String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())
                    + " | session=" + debugSessionId()
                    + " | app=" + APP_VERSION_LABEL
                    + " | device=" + clean(Build.MANUFACTURER) + " " + clean(Build.MODEL)
                    + " | android=" + Build.VERSION.RELEASE
                    + " | step=" + currentTestStep
                    + " | event=" + safeLog(event)
                    + " | " + safeLog(detail)
                    + "\n";
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (Exception ignored) {
        }
    }

    private void installCrashLogger() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                appendDebugLog("fatal_crash",
                        "thread=" + (thread == null ? "unknown" : thread.getName())
                                + " | " + throwable.toString()
                                + " | " + stackTraceText(throwable));
            } catch (Throwable ignored) {
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            } else {
                System.exit(2);
            }
        });
    }

    private String stackTraceText(Throwable throwable) {
        if (throwable == null) return "";
        StringBuilder out = new StringBuilder();
        StackTraceElement[] stack = throwable.getStackTrace();
        int limit = Math.min(stack.length, 12);
        for (int i = 0; i < limit; i++) {
            out.append(stack[i].toString()).append(" / ");
        }
        Throwable cause = throwable.getCause();
        if (cause != null) {
            out.append("cause=").append(cause.toString());
        }
        return out.toString();
    }

    private String readDebugLog() {
        try {
            InputStream stream = new java.io.FileInputStream(getDebugLogFile());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            stream.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String readRecentDebugLog() {
        String log = readDebugLog();
        if (log.length() == 0) return "No log yet.";
        int max = 2600;
        return log.length() <= max ? log : log.substring(log.length() - max);
    }

    private String debugSessionId() {
        return clean(Build.MANUFACTURER).replaceAll("[^A-Za-z0-9]", "")
                + "-" + clean(Build.MODEL).replaceAll("[^A-Za-z0-9]", "")
                + "-" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
    }

    private String safeLog(String value) {
        if (value == null) return "";
        return value.replace("\r", " ").replace("\n", " ").trim();
    }

    private void seedText() {
        add("app", "Phone Check V92", "Semak Telefon V92", "手机检测 V92");
        add("homeButton", "Home", "Utama", "主页");
        add("version", "Version", "Versi", "版本");
        add("homeTitle", "Check phone functions before trade-in", "Semak fungsi telefon sebelum trade-in", "回收前检测手机功能");
        add("homeBody", "This version runs real hardware checks where Android allows it: live microphone level, vibration, flash, sensors, top proximity/light sensor, NFC card/tag reading, SIM slot and mobile network service report, telco lock check, fingerprint verification, battery, Wi-Fi scan test, Bluetooth scan test, location fix test, fine touch grid, screen colors and button input.", "Versi ini menjalankan ujian perkakasan sebenar jika Android membenarkan: tahap mikrofon langsung, getaran, lampu flash, sensor, sensor jarak/cahaya atas, bacaan kad/tag NFC, laporan slot SIM dan servis rangkaian mudah alih, semakan telco lock, pengesahan cap jari, bateri, ujian imbasan Wi-Fi, ujian imbasan Bluetooth, ujian lokasi, grid sentuhan halus, warna skrin dan butang.", "这个版本会在 Android 允许的范围内实际检测：实时麦克风音量、震动、闪光灯、传感器、顶部接近/光线感应器、NFC 持卡读卡、SIM 卡槽与移动网络服务报告、Telco lock 检查、指纹验证、电池、Wi-Fi 扫描测试、蓝牙扫描测试、定位取点测试、更细触控格子、屏幕纯色和按键输入。");
        add("autoTitle", "Important limitation", "Had penting", "重要限制");
        add("autoBody", "Some problems cannot be judged 100% automatically, for example screen cracks, camera blur, speaker distortion or hidden water damage. The app marks these for manual review.", "Sesetengah masalah tidak boleh dinilai 100% automatik, seperti skrin retak, kamera kabur, bunyi pecah atau kerosakan air tersembunyi. Aplikasi akan tandakan untuk semakan manual.", "有些问题无法 100% 自动判断，例如屏幕裂痕、相机模糊、扬声器破音、隐藏进水。App 会标记为人工复核。");
        add("start", "Start phone check", "Mula semakan", "开始检测");
        add("device", "Detected device", "Peranti dikesan", "已识别设备");
        add("brand", "Brand", "Jenama", "品牌");
        add("model", "Model", "Model", "型号");
        add("ramDetected", "Detected RAM", "RAM dikesan", "识别 RAM");
        add("storageDetected", "Detected storage", "Storan dikesan", "识别存储");
        add("confirmRam", "Confirm RAM", "Sahkan RAM", "确认 RAM");
        add("confirmRom", "Confirm ROM", "Sahkan ROM", "确认 ROM");
        add("continue", "Continue", "Teruskan", "继续");
        add("tests", "Function tests", "Ujian fungsi", "功能检测");
        add("stepAutoTitle", "Step 1: automatic checks", "Langkah 1: ujian automatik", "第 1 步：自动检测");
        add("stepAutoBody", "This page checks the items Android can read automatically. After it finishes, the app will continue to the next test page.", "Halaman ini menyemak item yang Android boleh baca secara automatik. Selepas selesai, aplikasi akan terus ke halaman ujian seterusnya.", "这一页会检测 Android 能自动读取的项目。完成后会进入下一项测试。");
        add("camera", "Camera modules", "Modul kamera", "摄像头模块");
        add("flash", "Flashlight", "Lampu flash", "闪光灯");
        add("microphone", "Microphone", "Mikrofon", "麦克风");
        add("speaker", "Speaker", "Pembesar suara", "扬声器");
        add("vibration", "Vibration", "Getaran", "震动");
        add("wifi", "Wi-Fi", "Wi-Fi", "Wi-Fi");
        add("bluetooth", "Bluetooth", "Bluetooth", "蓝牙");
        add("gps", "Location / GPS", "Lokasi / GPS", "定位 / GPS");
        add("battery", "Battery", "Bateri", "电池");
        add("sensors", "Motion sensors", "Sensor gerakan", "动作传感器");
        add("proximity", "Top proximity / light sensor", "Sensor jarak / cahaya atas", "顶部接近 / 光线感应器");
        add("nfc", "NFC", "NFC", "NFC");
        add("simSlot", "SIM slot", "Slot SIM", "SIM 卡槽");
        add("telcoLock", "Telco lock / SIM lock", "Telco lock / SIM lock", "Telco lock / SIM 锁");
        add("biometric", "Fingerprint verification", "Pengesahan cap jari", "指纹验证");
        add("touch", "Touchscreen", "Skrin sentuh", "触控屏");
        add("screen", "Screen color test", "Ujian warna skrin", "屏幕纯色测试");
        add("buttons", "Volume buttons", "Butang volume", "音量键");
        add("runAuto", "Run automatic tests", "Jalankan ujian automatik", "运行自动检测");
        add("openTouch", "Test touchscreen", "Uji skrin sentuh", "测试触控屏");
        add("openMic", "Test microphone", "Uji mikrofon", "测试麦克风");
        add("openSpeaker", "Test speaker", "Uji pembesar suara", "测试扬声器");
        add("openCameraDetail", "Detailed camera test", "Ujian kamera terperinci", "详细镜头测试");
        add("openScreen", "Test screen colors", "Uji warna skrin", "测试屏幕颜色");
        add("openButtons", "Test volume buttons", "Uji butang volume", "测试音量键");
        add("openCondition", "Enter phone condition", "Isi keadaan telefon", "填写手机成色");
        add("calculate", "Calculate estimate", "Kira anggaran", "计算估价");
        add("notTested", "Not tested", "Belum diuji", "未检测");
        add("pass", "Pass", "Lulus", "通过");
        add("fail", "Fail", "Gagal", "异常");
        add("review", "Review", "Semak", "复核");
        add("pending", "Pending", "Menunggu", "等待");
        add("front", "Front", "Depan", "前置");
        add("back", "Back", "Belakang", "后置");
        add("cameraDetailTitle", "Detailed camera test", "Ujian kamera terperinci", "详细镜头测试");
        add("cameraAutoInfo", "Auto-detected camera info", "Maklumat kamera dikesan automatik", "自动识别镜头信息");
        add("cameraManualCheck", "Manual camera checks", "Semakan kamera manual", "手动镜头检查");
        add("openCameraApp", "Test camera hardware", "Uji perkakasan kamera", "测试相机硬件");
        add("testCameraHardware", "Test camera hardware", "Uji perkakasan kamera", "测试相机硬件");
        add("cameraManualGuide", "Rear camera auto check only proves that one rear stream can respond. For multi-lens phones, confirm main 1x, 0.5, and zoom manually before saving.", "Semakan automatik kamera belakang hanya membuktikan satu stream belakang boleh bertindak balas. Untuk telefon berbilang lensa, sahkan 1x utama, 0.5 dan zoom secara manual sebelum simpan.", "后镜头自动检测只证明其中一个后摄串流有反应。多镜头手机请手动确认主镜头 1x、0.5 和 zoom 后再保存。");
        add("cameraSafeMode", "Camera frame quality mode: brightness/variation check with timeout failback.", "Mod kualiti frame kamera: semak kecerahan/variasi dengan tamat masa abnormal.", "相机画面质量模式：亮度/变化检查，加超时失败返回。");
        add("openSafeCameraPreview", "Open camera preview test", "Buka ujian pratonton kamera", "打开相机预览测试");
        add("testBackCamera", "Auto rear stream check", "Semak stream belakang automatik", "自动检查后摄串流");
        add("testFrontCamera", "Test front camera", "Uji kamera depan", "测试前镜头");
        add("backCamera", "Back camera", "Kamera belakang", "后镜头");
        add("frontCamera", "Front camera", "Kamera depan", "前镜头");
        add("cameraCannotOpen", "Camera cannot open / black screen", "Kamera tidak boleh buka / skrin hitam", "相机无法打开 / 黑屏");
        add("cameraCannotOpenNote", "Camera marked failed: cannot open, black screen, frozen preview, or camera service unstable.", "Kamera ditanda gagal: tidak boleh buka, skrin hitam, pratonton beku, atau servis kamera tidak stabil.", "相机判定失败：无法打开、黑屏、预览卡住或相机服务异常。");
        add("cameraOpenCondition", "Main rear camera / 1x result", "Keputusan kamera belakang utama / 1x", "主后镜头 / 1x 结果");
        add("cameraOpensOk", "Camera opens normally", "Kamera boleh dibuka", "相机可以打开");
        add("cameraOpenFailed", "Could not open camera hardware", "Tidak dapat buka perkakasan kamera", "无法打开相机硬件");
        add("cameraHardwareReady", "Camera hardware has not been opened yet.", "Perkakasan kamera belum dibuka.", "尚未打开相机硬件测试。");
        add("cameraPermissionNeeded", "Camera permission is needed for hardware test.", "Kebenaran kamera diperlukan untuk ujian perkakasan.", "需要相机权限才能测试硬件。");
        add("cameraHardwareTesting", "Opening camera hardware now...", "Sedang membuka perkakasan kamera...", "正在打开相机硬件...");
        add("cameraHardwareOpened", "Camera hardware opened successfully. Photo quality still needs manual review.", "Perkakasan kamera berjaya dibuka. Kualiti gambar masih perlu semakan manual.", "相机硬件可以打开。照片质量仍需人工复核。");
        add("cameraRearAutoNeedsManual", "Rear stream responded. This is not a full pass: confirm 1x, 0.5, and zoom manually.", "Stream belakang bertindak balas. Ini bukan lulus penuh: sahkan 1x, 0.5 dan zoom secara manual.", "后摄串流有反应，但这不是完整通过：请手动确认 1x、0.5 和 zoom。");
        add("cameraPreviewSessionOpened", "Camera preview session opened successfully. Photo quality still needs manual review.", "Sesi pratonton kamera berjaya dibuka. Kualiti gambar masih perlu semakan manual.", "相机预览通道可以正常建立。照片质量仍需人工复核。");
        add("cameraPreviewSessionFailed", "Camera preview session could not start.", "Sesi pratonton kamera tidak dapat dimulakan.", "相机预览通道无法启动。");
        add("cameraPreviewTitle", "Camera live preview test", "Ujian pratonton langsung kamera", "相机实时预览测试");
        add("cameraPreviewGuide", "The preview below must show a live camera image. If it is black, frozen, or cannot start, mark camera failed.", "Pratonton di bawah mesti menunjukkan imej kamera langsung. Jika hitam, beku, atau tidak dapat mula, tanda kamera gagal.", "下面必须显示实时相机画面。如果黑屏、卡住或无法启动，请标记相机异常。");
        add("cameraPreviewCheckNow", "Preview started. Check whether the image is live and clear enough.", "Pratonton bermula. Semak sama ada imej bergerak dan cukup jelas.", "预览已启动。请确认画面是否实时显示。");
        add("cameraPreviewVisible", "Preview is visible", "Pratonton kelihatan", "预览画面正常");
        add("cameraPreviewFailed", "Preview failed / black screen", "Pratonton gagal / skrin hitam", "预览失败 / 黑屏");
        add("cameraPreviewUserVisible", "User confirmed live camera preview is visible", "Pengguna sahkan pratonton kamera langsung kelihatan", "用户确认相机实时预览正常");
        add("cameraPreviewUserFailed", "User reported camera preview failed or black screen", "Pengguna lapor pratonton kamera gagal atau skrin hitam", "用户报告相机预览失败或黑屏");
        add("cameraFrameAutoPass", "Camera preview confirmed visible by tester.", "Pratonton kamera disahkan kelihatan oleh pemeriksa.", "测试员确认相机预览画面可见。");
        add("cameraFrameAutoFail", "Camera preview failed, black screen, or could not open.", "Pratonton kamera gagal, skrin hitam, atau tidak dapat dibuka.", "相机预览失败、黑屏或无法打开。");
        add("cameraHardwareDisconnected", "Camera disconnected during hardware test.", "Kamera terputus semasa ujian perkakasan.", "相机硬件测试时断开。");
        add("cameraHardwareFailed", "Camera hardware could not be opened.", "Perkakasan kamera tidak dapat dibuka.", "相机硬件无法打开。");
        add("cameraHardwareTimeout", "Camera hardware did not respond within 5 seconds.", "Perkakasan kamera tidak memberi respons dalam 5 saat.", "相机硬件 5 秒内无响应。");
        add("cameraTotal", "Camera modules", "Modul kamera", "镜头模块数量");
        add("cameraUnknown", "Unknown", "Tidak pasti", "未知");
        add("focalLength", "focal length", "jarak fokus", "焦距");
        add("ultraWideLikely", "0.5x / ultra-wide likely", "0.5x / ultra-wide kemungkinan", "可能支持 0.5x / 超广角");
        add("maxDigitalZoom", "Max digital zoom", "Zoom digital maksimum", "最大数码变焦");
        add("zoomRatioRange", "Zoom ratio range", "Julat nisbah zoom", "变焦比例范围");
        add("cameraAutoLimit", "Auto info shows hardware capability only. Photo quality still needs manual review.", "Maklumat automatik hanya menunjukkan kemampuan perkakasan. Kualiti gambar masih perlu semakan manual.", "自动信息只能显示硬件能力，照片质量仍需要人工复核。");
        add("yes", "Yes", "Ya", "是");
        add("no", "No", "Tidak", "否");
        add("notSure", "Not sure", "Tidak pasti", "不确定");
        add("ultraWide05", "0.5x ultra-wide", "0.5x ultra-wide", "0.5x 超广角");
        add("zoomInOut", "Zoom in / zoom out", "Zoom masuk / keluar", "Zoom in / out");
        add("cameraBlackSpots", "Black dots in photo", "Titik hitam dalam gambar", "照片黑点");
        add("cameraFogWatermark", "Fog / watermark / stain", "Kabus / watermark / kesan", "雾化 / 水印 / 污渍");
        add("cameraFocusBlur", "Focus / blur issue", "Masalah fokus / kabur", "对焦 / 模糊问题");
        add("cameraOk", "OK", "OK", "正常");
        add("cameraNotAvailable", "Not available", "Tiada", "没有此功能");
        add("cameraProblem", "Problem found", "Ada masalah", "发现问题");
        add("cameraNotTested", "Not tested", "Belum diuji", "未测试");
        add("noneSeen", "None seen", "Tiada", "没有看到");
        add("minorSeen", "Minor", "Ringan", "轻微");
        add("obviousSeen", "Obvious", "Jelas", "明显");
        add("saveCameraDetail", "Save camera result", "Simpan keputusan kamera", "保存镜头结果");
        add("cameraDetailSummary", "Camera detail summary", "Ringkasan kamera", "镜头详细总结");
        add("cameraDetailDeduction", "Camera detail deduction", "Potongan kamera", "镜头扣减");
        add("cameraMissing", "No camera detected", "Kamera tidak dikesan", "未检测到摄像头");
        add("cameraReview", "Camera needs preview review", "Kamera perlu semakan pratonton", "摄像头需要预览复核");
        add("flashBlink", "Flash blinked once", "Flash berkelip sekali", "闪光灯已闪烁一次");
        add("flashReview", "No usable flash detected", "Flash tidak dikesan", "未检测到可用闪光灯");
        add("micPermission", "Microphone permission not granted", "Kebenaran mikrofon belum diberi", "未授权麦克风权限");
        add("micNeedsManual", "Open microphone test and speak loudly", "Buka ujian mikrofon dan bercakap kuat", "请打开麦克风测试并大声说话");
        add("micTestTitle", "Microphone recording test", "Ujian rakaman mikrofon", "麦克风录音测试");
        add("micTestGuide", "Tap start, then speak near the phone for 5 seconds. The app records the full sample and plays it back so you can hear dropouts.", "Tekan mula, kemudian bercakap dekat telefon selama 5 saat. Aplikasi merakam sampel penuh dan memainkan semula supaya anda boleh dengar jika bunyi putus-putus.", "点击开始后，对着手机说话 5 秒。App 会录下完整声音并自动回放，方便检查是否断音。");
        add("micReady", "Ready to test", "Sedia untuk diuji", "准备测试");
        add("startMic", "Record 5-second microphone sample", "Rakam sampel mikrofon 5 saat", "录制 5 秒麦克风声音");
        add("micListening", "Listening now...", "Sedang mendengar...", "正在录音...");
        add("secondsLeft", "Seconds left", "Saat baki", "剩余秒数");
        add("micAmplitude", "Max amplitude", "Amplitud maksimum", "最大音量");
        add("micRecordedSummary", "Recording completed. Listen to the playback and confirm whether the sound is continuous.", "Rakaman selesai. Dengar main semula dan sahkan sama ada bunyi berterusan.", "录音完成。请听自动回放，确认声音是否连续。");
        add("micActiveSound", "Active sound", "Bunyi aktif", "有效声音");
        add("micLongestQuiet", "Longest quiet gap", "Senyap paling lama", "最长静音段");
        add("micPlaybackNow", "Playing recorded microphone sample now...", "Sedang memainkan semula rakaman mikrofon...", "正在回放刚才录到的麦克风声音...");
        add("micPlaybackFailed", "Recorded microphone playback failed", "Main semula rakaman mikrofon gagal", "麦克风录音回放失败");
        add("micFailed", "Recording failed", "Rakaman gagal", "录音失败");
        add("micPlaybackClear", "Microphone playback is clear", "Main semula mikrofon jelas", "麦克风回放正常");
        add("micUserPass", "User confirmed microphone recording is continuous and clear", "Pengguna sahkan rakaman mikrofon berterusan dan jelas", "用户确认麦克风录音连续且清楚");
        add("markMicFail", "Mark microphone as failed", "Tanda mikrofon gagal", "标记麦克风异常");
        add("micUserFail", "User marked microphone problem", "Pengguna tandakan masalah mikrofon", "用户标记麦克风异常");
        add("speakerNote", "Test tone played. Listen for distortion.", "Bunyi ujian dimainkan. Dengar sama ada pecah.", "已播放测试音，请听是否破音");
        add("speakerNeedsManual", "Open speaker test and confirm sound quality", "Buka ujian pembesar suara dan sahkan kualiti bunyi", "请打开扬声器测试并确认声音质量");
        add("speakerTestTitle", "Speaker sound test", "Ujian bunyi pembesar suara", "扬声器声音测试");
        add("speakerTestGuide", "Tap play, raise media volume if needed. The app plays a 5-second tone and uses the microphone to confirm sound output. Distortion still needs your hearing check.", "Tekan main, naikkan volume media jika perlu. Aplikasi memainkan nada 5 saat dan guna mikrofon untuk sahkan output bunyi. Bunyi pecah masih perlu didengar manual.", "点击播放，必要时调高媒体音量。App 会播放 5 秒测试音，并用麦克风确认是否有声音输出；破音/杂音仍需人工听。");
        add("speakerReady", "Ready to test speaker output.", "Sedia untuk uji output pembesar suara.", "准备测试喇叭输出。");
        add("speakerTesting", "Playing tone and checking speaker output...", "Sedang main nada dan semak output pembesar suara...", "正在播放测试音并检测喇叭是否有输出...");
        add("speakerMicPermission", "Microphone permission is needed to confirm speaker output.", "Kebenaran mikrofon diperlukan untuk sahkan output pembesar suara.", "需要麦克风权限才能确认喇叭是否有声音输出。");
        add("speakerMicLevel", "Detected sound level", "Tahap bunyi dikesan", "检测到的声音数值");
        add("speakerOutputDetected", "Speaker output detected. Please confirm whether the sound is clear.", "Output pembesar suara dikesan. Sila sahkan sama ada bunyi jelas.", "已检测到喇叭有声音输出，请确认声音是否清楚。");
        add("speakerNoOutputDetected", "No clear speaker output detected. Raise media volume and retry, or mark speaker problem.", "Tiada output pembesar suara yang jelas dikesan. Naikkan volume media dan cuba lagi, atau tanda masalah pembesar suara.", "未检测到明显喇叭声音。请调高媒体音量重试，或标记喇叭异常。");
        add("speakerManualQuality", "Listen for crackling, distortion, weak sound or intermittent sound.", "Dengar sama ada ada bunyi berkeretak, pecah, lemah atau putus-putus.", "请继续听是否有杂音、破音、小声或断断续续。");
        add("playSpeaker", "Play test sound", "Main bunyi ujian", "播放测试音");
        add("speakerPlayed", "Test sound played", "Bunyi ujian dimainkan", "已播放测试音");
        add("speakerGood", "Sound is clear", "Bunyi jelas", "声音正常");
        add("speakerBad", "Speaker has problem", "Pembesar suara bermasalah", "扬声器异常");
        add("speakerUserPass", "User confirmed clear sound", "Pengguna sahkan bunyi jelas", "用户确认声音正常");
        add("speakerUserFail", "User reported speaker issue", "Pengguna lapor masalah pembesar suara", "用户报告扬声器异常");
        add("speakerFailed", "Could not play test tone", "Tidak dapat main bunyi ujian", "无法播放测试音");
        add("vibrationNote", "Vibration command sent", "Arahan getaran dihantar", "已触发震动");
        add("wifiConnected", "Connected to Wi-Fi", "Disambung ke Wi-Fi", "已连接 Wi-Fi");
        add("wifiMissing", "No Wi-Fi module reported", "Modul Wi-Fi tidak dikesan", "未检测到 Wi‑Fi 模块");
        add("wifiOff", "Wi-Fi is turned off", "Wi-Fi dimatikan", "Wi‑Fi 已关闭");
        add("wifiNoNetwork", "Wi-Fi is on but current network is not Wi-Fi", "Wi-Fi aktif tetapi rangkaian semasa bukan Wi-Fi", "Wi‑Fi 已开启，但当前网络不是 Wi‑Fi");
        add("wifiReview", "Not connected / disabled", "Tidak bersambung / dimatikan", "未连接或未开启");
        add("wifiNeedsConnectTest", "Open the Wi-Fi test page to verify opening and scanning", "Buka halaman ujian Wi-Fi untuk sahkan buka dan imbasan", "请进入 Wi-Fi 测试页，确认能开启和扫描");
        add("wifiTestTitle", "Wi-Fi opening and scan test", "Ujian buka dan imbasan Wi-Fi", "Wi-Fi 开启与扫描测试");
        add("wifiTestGuide", "Tap Open Wi-Fi settings, turn on Wi-Fi if needed, then return here. GPS/location service does not need to be on. Passing only requires Wi-Fi to be on and the scan request to start; connection is not required.", "Tekan Buka tetapan Wi-Fi, hidupkan Wi-Fi jika perlu, kemudian kembali ke sini. GPS/servis lokasi tidak perlu aktif. Lulus hanya perlukan Wi-Fi aktif dan permintaan imbasan bermula; sambungan tidak diperlukan.", "点击打开 Wi-Fi 设置，需要时开启 Wi-Fi，然后回到这里。不需要开启 GPS/定位服务。通过只需要 Wi-Fi 已开启并能启动扫描请求，不需要连接。");
        add("openWifiSettings", "Open Wi-Fi settings", "Buka tetapan Wi-Fi", "打开 Wi-Fi 设置");
        add("checkWifiNow", "Check Wi-Fi now", "Semak Wi-Fi sekarang", "立即检查 Wi-Fi");
        add("markWifiFail", "Mark Wi-Fi as failed", "Tanda Wi-Fi gagal", "标记 Wi-Fi 异常");
        add("wifiUserFail", "User marked Wi-Fi problem", "Pengguna tandakan masalah Wi-Fi", "用户标记 Wi-Fi 异常");
        add("wifiChecking", "Checking Wi-Fi opening and scan...", "Menyemak buka dan imbasan Wi-Fi...", "正在检查 Wi-Fi 开启和扫描能力...");
        add("wifiReturnAuto", "Return here after turning on Wi-Fi. Auto scan will start.", "Kembali ke sini selepas hidupkan Wi-Fi. Imbasan automatik akan bermula.", "开启 Wi-Fi 后回到这里，会自动开始扫描。");
        add("wifiAutoTrying", "Auto Wi-Fi scan attempt", "Cubaan imbasan Wi-Fi automatik", "正在自动尝试 Wi-Fi 扫描");
        add("wifiPermissionNeeded", "Wi-Fi scan result access may need location permission on some Android versions, but GPS does not need to be turned on.", "Akses hasil imbasan Wi-Fi mungkin perlukan kebenaran lokasi pada sesetengah Android, tetapi GPS tidak perlu dihidupkan.", "部分 Android 版本读取 Wi-Fi 扫描结果可能需要定位权限，但不需要开启 GPS。");
        add("wifiNotPassedYet", "Wi-Fi scan is not confirmed yet. Turn on Wi-Fi, then check again. GPS does not need to be on.", "Imbasan Wi-Fi belum disahkan. Hidupkan Wi-Fi, kemudian semak lagi. GPS tidak perlu aktif.", "Wi-Fi 扫描还未确认。请开启 Wi-Fi 后再检查，不需要开启 GPS。");
        add("wifiConnectPass", "Wi-Fi scan test passed", "Ujian imbasan Wi-Fi lulus", "Wi-Fi 扫描测试通过");
        add("wifiOpenRequired", "Wi-Fi is still off. Open Wi-Fi settings and turn it on.", "Wi-Fi masih tutup. Buka tetapan Wi-Fi dan hidupkan.", "Wi-Fi 仍未开启，请打开 Wi-Fi 设置并开启。");
        add("wifiCanScanNeedConnect", "Wi-Fi scan test passed", "Ujian imbasan Wi-Fi lulus", "Wi-Fi 扫描测试通过");
        add("wifiConnectFail", "Wi-Fi scan did not start and no scan result is available", "Imbasan Wi-Fi tidak bermula dan tiada hasil imbasan", "Wi-Fi 扫描未启动，也没有扫描结果");
        add("wifiScanPass", "Wi-Fi is on and scan result access is available", "Wi-Fi aktif dan akses hasil imbasan tersedia", "Wi-Fi 已开启并可读取扫描结果");
        add("wifiScanStartedPass", "Wi-Fi is on and scan request started. GPS/location service is not required for this pass.", "Wi-Fi aktif dan permintaan imbasan bermula. GPS/servis lokasi tidak diperlukan untuk lulus.", "Wi-Fi 已开启并成功启动扫描请求。此通过不需要开启 GPS/定位服务。");
        add("wifiScanFailed", "Wi-Fi is on, but scan request could not start", "Wi-Fi aktif, tetapi permintaan imbasan tidak dapat bermula", "Wi-Fi 已开启，但无法启动扫描请求");
        add("wifiScanFound", "Nearby Wi-Fi networks found:", "Rangkaian Wi-Fi berhampiran ditemui:", "扫描到附近 Wi-Fi 数量：");
        add("wifiScanBlocked", "Wi-Fi scan result list unavailable. This can happen when Android blocks scan results because location service is off.", "Senarai hasil imbasan Wi-Fi tidak tersedia. Ini boleh berlaku apabila Android menyekat hasil imbasan kerana servis lokasi dimatikan.", "无法读取 Wi-Fi 扫描列表。这可能是 Android 因定位服务关闭而限制扫描结果。");
        add("btOn", "Bluetooth is on", "Bluetooth aktif", "蓝牙已开启");
        add("btMissing", "No Bluetooth adapter", "Tiada adapter Bluetooth", "无蓝牙模块");
        add("btReview", "Bluetooth off or permission blocked", "Bluetooth tutup atau kebenaran disekat", "蓝牙关闭或权限受限");
        add("btNeedsFunctionTest", "Open the Bluetooth test page to verify opening and scanning", "Buka halaman ujian Bluetooth untuk sahkan buka dan imbasan", "请进入蓝牙测试页，确认能开启和扫描");
        add("btTestTitle", "Bluetooth opening and scan test", "Ujian buka dan imbasan Bluetooth", "蓝牙开启与扫描测试");
        add("btTestGuide", "Tap Open Bluetooth settings, turn on Bluetooth, then return here. The app will automatically scan. Passing only requires Bluetooth to be on and able to start scanning; pairing or connection is not required.", "Tekan Buka tetapan Bluetooth, hidupkan Bluetooth, kemudian kembali ke sini. Aplikasi akan mengimbas secara automatik. Lulus hanya perlukan Bluetooth aktif dan boleh mula mengimbas; pasangan atau sambungan tidak diperlukan.", "点击打开蓝牙设置并开启蓝牙，然后回到这里。App 会自动扫描。通过只需要蓝牙已开启并能启动扫描，不需要配对或连接。");
        add("openBtSettings", "Open Bluetooth settings", "Buka tetapan Bluetooth", "打开蓝牙设置");
        add("checkBtNow", "Check Bluetooth now", "Semak Bluetooth sekarang", "立即检查蓝牙");
        add("markBtFail", "Mark Bluetooth as failed", "Tanda Bluetooth gagal", "标记蓝牙异常");
        add("btUserFail", "User marked Bluetooth problem", "Pengguna tandakan masalah Bluetooth", "用户标记蓝牙异常");
        add("btChecking", "Checking Bluetooth opening and scan...", "Menyemak buka dan imbasan Bluetooth...", "正在检查蓝牙开启和扫描能力...");
        add("btReturnAuto", "Return here after turning on Bluetooth. Auto scan will start.", "Kembali ke sini selepas hidupkan Bluetooth. Imbasan automatik akan bermula.", "开启蓝牙后回到这里，会自动开始扫描。");
        add("btAutoTrying", "Auto Bluetooth scan attempt", "Cubaan imbasan Bluetooth automatik", "正在自动尝试蓝牙扫描");
        add("btNotPassedYet", "Bluetooth scan is not confirmed yet. Turn on Bluetooth and check again.", "Imbasan Bluetooth belum disahkan. Hidupkan Bluetooth dan semak lagi.", "蓝牙扫描还未确认。请开启蓝牙后再检查。");
        add("btOpenRequired", "Bluetooth is still off. Open Bluetooth settings and turn it on.", "Bluetooth masih tutup. Buka tetapan Bluetooth dan hidupkan.", "蓝牙仍未开启，请打开蓝牙设置并开启。");
        add("btConnectedPass", "Bluetooth scan test passed", "Ujian imbasan Bluetooth lulus", "蓝牙扫描测试通过");
        add("btScanPass", "Bluetooth can turn on and start scanning", "Bluetooth boleh dihidupkan dan mula mengimbas", "蓝牙可开启并能启动扫描");
        add("bluetoothContinue", "Bluetooth passed - continue", "Bluetooth lulus - teruskan", "蓝牙通过 - 继续");
        add("btPairedReview", "Bluetooth is on, but scanning was not confirmed", "Bluetooth aktif, tetapi imbasan belum disahkan", "蓝牙已开启，但未确认扫描能力");
        add("btScanFailed", "Bluetooth is on, but scan did not start", "Bluetooth aktif, tetapi imbasan tidak bermula", "蓝牙已开启，但扫描未能启动");
        add("btPairedFound", "Paired Bluetooth devices:", "Peranti Bluetooth berpasangan:", "已配对蓝牙设备数量：");
        add("btPermissionBlocked", "Bluetooth permission blocked or restricted by Android", "Kebenaran Bluetooth disekat atau dihadkan Android", "蓝牙权限被 Android 限制或未授权");
        add("gpsOn", "Location service is on", "Servis lokasi aktif", "定位服务已开启");
        add("gpsReview", "Location service is off", "Servis lokasi dimatikan", "定位服务关闭");
        add("gpsNeedsFunctionTest", "Open the location test page to verify actual location fix", "Buka halaman ujian lokasi untuk sahkan bacaan lokasi sebenar", "请进入定位测试页，确认能实际取得位置");
        add("gpsTestTitle", "Location fix test", "Ujian bacaan lokasi", "定位取点测试");
        add("gpsTestGuide", "This test requests live location from both GPS and network providers. Wi-Fi is not required; if Wi-Fi has no internet, the phone can still pass using GPS or mobile/network location.", "Ujian ini meminta lokasi langsung daripada GPS dan provider rangkaian. Wi-Fi tidak wajib; jika Wi-Fi tiada internet, telefon masih boleh lulus melalui GPS atau lokasi mudah alih/rangkaian.", "这个测试会同时请求 GPS 和网络定位。Wi-Fi 不是必须；如果 Wi-Fi 无法联网，手机仍可通过 GPS 或移动/网络定位取得位置并通过。");
        add("openLocationSettings", "Open location settings", "Buka tetapan lokasi", "打开定位设置");
        add("checkLocationNow", "Check location now", "Semak lokasi sekarang", "立即检查定位");
        add("markGpsFail", "Mark location as failed", "Tanda lokasi gagal", "标记定位异常");
        add("gpsUserFail", "User marked location problem", "Pengguna tandakan masalah lokasi", "用户标记定位异常");
        add("gpsPermission", "Location permission not granted", "Kebenaran lokasi belum diberi", "定位权限未授权");
        add("gpsPermissionGranted", "Location permission granted. Tap Check location now to start the test.", "Kebenaran lokasi diberi. Tekan Semak lokasi sekarang untuk mula ujian.", "定位权限已授权。请点击立即检查定位开始测试。");
        add("gpsOpenRequired", "Location service is off. Open location settings and turn it on.", "Servis lokasi ditutup. Buka tetapan lokasi dan hidupkan.", "定位服务关闭，请打开定位设置并开启。");
        add("gpsChecking", "Getting live location. Wi-Fi is not required; GPS or mobile/network location can pass.", "Mendapatkan lokasi langsung. Wi-Fi tidak wajib; GPS atau lokasi mudah alih/rangkaian boleh lulus.", "正在取得实时位置。Wi-Fi 不是必须；GPS 或移动/网络定位都可以通过。");
        add("gpsReturnAuto", "Return here after turning on location. Auto location check will start.", "Kembali ke sini selepas hidupkan lokasi. Semakan lokasi automatik akan bermula.", "开启定位后回到这里，会自动开始定位检测。");
        add("gpsAutoTrying", "Auto location check attempt", "Cubaan semakan lokasi automatik", "正在自动尝试定位检测");
        add("gpsReadyToTest", "Location is on. Tap Check location now.", "Lokasi aktif. Tekan Semak lokasi sekarang.", "定位已开启，请点击立即检查定位。");
        add("gpsLastKnownPass", "Location result available", "Bacaan lokasi tersedia", "已取得位置结果");
        add("gpsLivePass", "Live location result received", "Bacaan lokasi langsung diterima", "已取得实时位置结果");
        add("gpsProviderPass", "Live GPS location received", "Lokasi GPS langsung diterima", "已取得实时 GPS 定位");
        add("gpsNetworkPass", "Live network/mobile location received", "Lokasi rangkaian/mudah alih langsung diterima", "已取得实时网络/移动定位");
        add("locationContinue", "Location passed - continue", "Lokasi lulus - teruskan", "定位通过 - 继续");
        add("gpsTimeoutReview", "No live location result within 35 seconds. Turn on location, allow permission, then try near a window or with mobile data.", "Tiada lokasi langsung dalam 35 saat. Hidupkan lokasi, benarkan kebenaran, kemudian cuba dekat tingkap atau dengan data mudah alih.", "35 秒内没有取得实时位置。请开启定位和权限，然后靠近窗边或使用移动数据再试。");
        add("gpsProviderOn", "On", "Aktif", "已开启");
        add("gpsProviderOff", "Off", "Dimatikan", "未开启");
        add("networkLocation", "Network location", "Lokasi rangkaian", "网络定位");
        add("gpsRequestFailed", "Location request failed", "Permintaan lokasi gagal", "定位请求失败");
        add("gpsNotPassedYet", "Location is not confirmed yet. Turn on location and try again near a window.", "Lokasi belum disahkan. Hidupkan lokasi dan cuba lagi dekat tingkap.", "定位还未确认可用，请开启定位并靠近窗边再试。");
        add("gpsAccuracy", "Accuracy", "Ketepatan", "精度");
        add("sensorMissing", "Accelerometer missing", "Accelerometer tiada", "未检测到加速度传感器");
        add("shakePhone", "Shake phone to complete sensor test", "Goncang telefon untuk lengkapkan ujian", "摇动手机完成传感器测试");
        add("sensorLive", "Movement", "Pergerakan", "动作数值");
        add("sensorPass", "Motion detected", "Gerakan dikesan", "已检测到动作");
        add("sensorNeedsDirectionTest", "Open motion sensor test and tilt the phone in four directions", "Buka ujian sensor gerakan dan condongkan telefon dalam empat arah", "请进入动作传感器测试，并完成上下左右四个方向");
        add("sensorDirectionTitle", "Motion sensor direction test", "Ujian arah sensor gerakan", "动作传感器方向测试");
        add("sensorDirectionGuide", "Tilt the phone left, right, upward and downward. All four directions must be detected before this item passes.", "Condongkan telefon ke kiri, kanan, atas dan bawah. Keempat-empat arah mesti dikesan sebelum item ini lulus.", "请把手机分别向左、向右、向上、向下倾斜。四个方向都检测到后才会通过。");
        add("sensorDirectionWaiting", "Waiting for left / right / up / down movement", "Menunggu gerakan kiri / kanan / atas / bawah", "等待检测左 / 右 / 上 / 下动作");
        add("sensorPassDetailed", "Left, right, up and down movement detected", "Gerakan kiri, kanan, atas dan bawah dikesan", "已检测到左、右、上、下动作");
        add("sensorLeft", "Tilt left", "Condong kiri", "向左倾斜");
        add("sensorRight", "Tilt right", "Condong kanan", "向右倾斜");
        add("sensorUp", "Tilt up", "Condong atas", "向上倾斜");
        add("sensorDown", "Tilt down", "Condong bawah", "向下倾斜");
        add("markSensorFail", "Mark motion sensor as failed", "Tanda sensor gerakan gagal", "标记动作传感器异常");
        add("sensorUserFail", "User marked motion sensor problem", "Pengguna tandakan masalah sensor gerakan", "用户标记动作传感器异常");
        add("proximityTitle", "Top sensor cover test", "Ujian tutup sensor atas", "顶部感应器遮挡测试");
        add("proximityGuide", "Keep the top sensor uncovered first. After calibration, cover the top speaker / sensor area with your hand. If the sensor works, the phone will vibrate.", "Jangan tutup sensor atas dahulu. Selepas kalibrasi, tutup kawasan speaker / sensor atas dengan tangan. Jika sensor berfungsi, telefon akan bergetar.", "请先不要遮挡顶部感应器。校准后再用手遮挡屏幕上方听筒/感应器区域。如果正常，手机会震动。");
        add("proximityWaiting", "Waiting for top cover...", "Menunggu bahagian atas ditutup...", "等待遮挡屏幕上方...");
        add("proximityCalibrating", "Calibrating uncovered sensor. Do not cover yet...", "Sedang kalibrasi sensor tanpa tutup. Jangan tutup dahulu...", "正在校准未遮挡状态，请先不要遮挡...");
        add("proximityCoverTop", "Cover the top speaker / sensor area", "Tutup kawasan speaker / sensor atas", "请遮挡听筒/顶部感应器区域");
        add("proximityValue", "Proximity value", "Nilai jarak", "接近感应器数值");
        add("lightValue", "Light value", "Nilai cahaya", "光线数值");
        add("lightStart", "start", "mula", "开始值");
        add("proximityPass", "Proximity sensor detected cover", "Sensor jarak mengesan tutupan", "接近感应器已检测到遮挡");
        add("lightSensorPass", "Light sensor detected cover", "Sensor cahaya mengesan tutupan", "光线感应器已检测到遮挡");
        add("proximityVibrated", "Phone vibrated to confirm", "Telefon bergetar untuk pengesahan", "手机已震动确认");
        add("proximityMissing", "No proximity or light sensor detected", "Sensor jarak atau cahaya tidak dikesan", "未检测到接近或光线感应器");
        add("markProximityFail", "Mark top sensor as failed", "Tanda sensor atas gagal", "标记顶部感应器异常");
        add("proximityUserFail", "User marked top sensor problem", "Pengguna tandakan masalah sensor atas", "用户标记顶部感应器异常");
        add("proximityContinue", "Top sensor passed - continue", "Sensor atas lulus - teruskan", "顶部感应器通过 - 继续");
        add("nfcTestTitle", "NFC card/tag reading test", "Ujian bacaan kad/tag NFC", "NFC 持卡读卡测试");
        add("nfcTestGuide", "Turn on NFC, then hold an NFC card or tag against the back/top area of the phone. The phone will vibrate and pass only after a tag is detected.", "Hidupkan NFC, kemudian letakkan kad atau tag NFC pada bahagian belakang/atas telefon. Telefon akan bergetar dan lulus hanya selepas tag dikesan.", "请先开启 NFC，然后把 NFC 卡或 Tag 靠近手机背面/上方感应区域。只有读到卡后才会震动并通过。");
        add("nfcWaitingCard", "NFC is on. Waiting for NFC card/tag...", "NFC aktif. Menunggu kad/tag NFC...", "NFC 已开启，等待靠近 NFC 卡/Tag...");
        add("nfcTagPass", "NFC card/tag detected", "Kad/tag NFC dikesan", "已读到 NFC 卡/Tag");
        add("nfcTagId", "Tag ID", "ID tag", "Tag ID");
        add("nfcTagTech", "Supported NFC tech count", "Bilangan teknologi NFC", "支持的 NFC 技术数量");
        add("nfcDisabledReview", "NFC module detected, but NFC is turned off. Open settings and turn it on.", "Modul NFC dikesan, tetapi NFC dimatikan. Buka tetapan dan hidupkan.", "已检测到 NFC 模块，但 NFC 关闭。请打开设置并开启。");
        add("nfcMissing", "No NFC module detected on this phone", "Modul NFC tidak dikesan pada telefon ini", "这台手机未检测到 NFC 模块");
        add("checkNfcNow", "Start / refresh NFC card test", "Mula / segar semula ujian kad NFC", "开始 / 刷新 NFC 持卡测试");
        add("openNfcSettings", "Open NFC settings", "Buka tetapan NFC", "打开 NFC 设置");
        add("markNfcFail", "Mark NFC as failed", "Tanda NFC gagal", "标记 NFC 异常");
        add("nfcUserFail", "User marked NFC problem", "Pengguna tandakan masalah NFC", "用户标记 NFC 异常");
        add("nfcSettingsFailed", "Could not open NFC settings", "Tidak dapat buka tetapan NFC", "无法打开 NFC 设置");
        add("simSlotTestTitle", "SIM slot report", "Laporan slot SIM", "SIM 卡槽报告");
        add("simSlotTestGuide", "Insert an active SIM, then check. Passing now requires the SIM to be ready and mobile network service to be available, not only card detection.", "Masukkan SIM aktif, kemudian semak. Lulus kini memerlukan SIM sedia dan servis rangkaian mudah alih tersedia, bukan hanya kad dikesan.", "请插入可用 SIM 后检查。现在通过条件是 SIM 已就绪并且有移动网络服务，不只是读到卡。");
        add("simSlotDetected", "Reported SIM slots / modems", "Slot SIM / modem dilaporkan", "系统报告 SIM 槽 / modem 数量");
        add("simNoCardLimit", "No-card test only confirms the phone reports SIM hardware. Full slot reading needs a SIM card.", "Ujian tanpa kad hanya mengesahkan telefon melaporkan perkakasan SIM. Bacaan slot penuh perlukan kad SIM.", "无卡测试只能确认手机系统报告 SIM 硬件。完整读卡能力需要插入 SIM 卡。");
        add("simNoTelephony", "No cellular phone module detected", "Modul telefon selular tidak dikesan", "未检测到蜂窝电话模块");
        add("simPermissionNeeded", "Phone state permission is needed to read inserted SIM status. Allow permission, then check again.", "Kebenaran status telefon diperlukan untuk membaca status SIM. Benarkan kebenaran, kemudian semak semula.", "需要电话状态权限才能读取插卡状态。请允许权限后重新检查。");
        add("simUnknown", "Unknown", "Tidak pasti", "未知");
        add("simReady", "SIM ready", "SIM sedia", "SIM 已就绪");
        add("simAbsent", "No SIM inserted", "Tiada SIM dimasukkan", "未插入 SIM");
        add("simPin", "SIM PIN required", "PIN SIM diperlukan", "需要 SIM PIN");
        add("simPuk", "SIM PUK required", "PUK SIM diperlukan", "需要 SIM PUK");
        add("simLocked", "Network locked", "Rangkaian dikunci", "网络锁定");
        add("simIoError", "SIM read I/O error", "Ralat I/O bacaan SIM", "SIM 读取 I/O 错误");
        add("simRestricted", "SIM restricted", "SIM dihadkan", "SIM 受限制");
        add("simReadyPass", "SIM inserted and reported ready. SIM slot reading passed.", "SIM dimasukkan dan dilaporkan sedia. Bacaan slot SIM lulus.", "已插入 SIM 且系统显示已就绪，SIM 卡槽读卡通过。");
        add("simReadyNetworkPass", "SIM is ready and mobile network service is available. SIM slot usable.", "SIM sedia dan servis rangkaian mudah alih tersedia. Slot SIM boleh digunakan.", "SIM 已就绪且有移动网络服务，SIM 卡槽可使用。");
        add("simReadyNoNetworkReview", "SIM is detected, but mobile network service is not available. Cannot confirm whether this is SIM/network issue or slot issue.", "SIM dikesan, tetapi servis rangkaian mudah alih tidak tersedia. Tidak boleh sahkan sama ada isu SIM/rangkaian atau slot.", "已检测到 SIM，但没有移动网络服务。暂时无法确认是 SIM/网络问题还是卡槽问题。");
        add("simNeedsInsertedReady", "Insert a SIM and wait until the slot shows SIM ready. If it still does not show ready, mark SIM slot as failed.", "Masukkan SIM dan tunggu sehingga slot menunjukkan SIM sedia. Jika masih tidak sedia, tanda slot SIM gagal.", "请插入 SIM 并等待卡槽显示 SIM 已就绪。如果仍无法就绪，请标记 SIM 卡槽异常。");
        add("checkSimNow", "Check SIM slot now", "Semak slot SIM sekarang", "立即检查 SIM 卡槽");
        add("markSimFail", "Mark SIM slot as failed", "Tanda slot SIM gagal", "标记 SIM 卡槽异常");
        add("simUserFail", "User marked SIM slot problem", "Pengguna tandakan masalah slot SIM", "用户标记 SIM 卡槽异常");
        add("telcoLockTitle", "Telco lock / SIM lock check", "Semakan telco lock / SIM lock", "Telco lock / SIM 锁检测");
        add("telcoLockGuide", "Insert a SIM from a different telco if possible, then check. If Android reports Network locked, the phone is likely telco locked. If the inserted SIM is ready and shows operator/network info, no lock is detected for that SIM.", "Masukkan SIM daripada telco lain jika boleh, kemudian semak. Jika Android melaporkan Network locked, telefon kemungkinan telco locked. Jika SIM sedia dan maklumat operator/rangkaian muncul, tiada lock dikesan untuk SIM itu.", "建议插入不同 telco 的 SIM 后检查。如果 Android 显示 Network locked，手机很可能有 telco lock。若插入的 SIM 已就绪并显示运营商/网络信息，则该 SIM 未检测到锁。");
        add("checkTelcoLockNow", "Check telco lock now", "Semak telco lock sekarang", "立即检查 Telco lock");
        add("markTelcoLockFail", "Mark as telco locked", "Tanda sebagai telco locked", "标记为 Telco lock");
        add("telcoUserFail", "User marked possible telco lock", "Pengguna tandakan kemungkinan telco lock", "用户标记疑似 Telco lock");
        add("simOperator", "SIM operator", "Operator SIM", "SIM 运营商");
        add("networkOperator", "Network operator", "Operator rangkaian", "网络运营商");
        add("mobileServiceState", "Mobile service state", "Status servis mudah alih", "移动网络服务状态");
        add("mobileDataState", "Mobile data state", "Status data mudah alih", "移动数据状态");
        add("activeCellularNetwork", "Active cellular network", "Rangkaian selular aktif", "当前蜂窝网络");
        add("serviceIn", "In service", "Dalam servis", "有服务");
        add("serviceOut", "Out of service", "Tiada servis", "无服务");
        add("serviceEmergency", "Emergency only", "Kecemasan sahaja", "仅限紧急呼叫");
        add("servicePowerOff", "Radio off", "Radio dimatikan", "蜂窝模块关闭");
        add("dataConnected", "Connected", "Bersambung", "已连接");
        add("dataConnecting", "Connecting", "Sedang bersambung", "连接中");
        add("dataDisconnected", "Disconnected", "Terputus", "未连接");
        add("dataSuspended", "Suspended", "Digantung", "已暂停");
        add("telcoLockDetected", "Network locked state detected. Possible telco lock.", "Keadaan Network locked dikesan. Kemungkinan telco lock.", "检测到 Network locked 状态，疑似 Telco lock。");
        add("telcoNoLockForInsertedSim", "No telco lock detected for the inserted SIM. For stronger proof, test with another telco SIM too.", "Tiada telco lock dikesan untuk SIM yang dimasukkan. Untuk bukti lebih kuat, uji juga dengan SIM telco lain.", "当前插入的 SIM 未检测到 Telco lock。更准确建议再用另一个 telco 的 SIM 测试。");
        add("telcoSimReadyNoNetwork", "SIM is ready but has no mobile network service. Cannot confirm telco lock; check SIM slot/network first or test another telco SIM.", "SIM sedia tetapi tiada servis rangkaian mudah alih. Tidak boleh sahkan telco lock; semak slot/rangkaian dahulu atau uji SIM telco lain.", "SIM 已就绪但没有移动网络服务，无法确认是否 Telco lock；请先检查 SIM 卡槽/网络，或换另一个 telco 的 SIM 测试。");
        add("telcoNeedsOtherSim", "Cannot confirm telco lock yet. Insert an active SIM, preferably from a different telco, then check again.", "Belum boleh sahkan telco lock. Masukkan SIM aktif, sebaiknya daripada telco lain, kemudian semak semula.", "暂时无法确认 Telco lock。请插入可用 SIM，最好是不同 telco 的 SIM，然后重新检查。");
        add("biometricTestTitle", "Fingerprint verification check", "Semakan pengesahan cap jari", "指纹验证检测");
        add("biometricTestGuide", "Tap Start fingerprint verification. This test only passes when the system accepts a fingerprint. PIN, pattern or password will not pass this item.", "Tekan Mula pengesahan cap jari. Ujian ini hanya lulus apabila sistem menerima cap jari. PIN, corak atau kata laluan tidak akan meluluskan item ini.", "点击开始指纹验证。此测试只有系统接受指纹才会通过，PIN、图案或密码不会让此项目通过。");
        add("fingerprint", "Fingerprint", "Cap jari", "指纹");
        add("fingerprintMissing", "No fingerprint hardware reported", "Tiada perkakasan cap jari dilaporkan", "未检测到指纹硬件");
        add("secureLock", "Secure lock", "Kunci selamat", "安全锁");
        add("detected", "Detected", "Dikesan", "已检测到");
        add("notDetected", "Not detected", "Tidak dikesan", "未检测到");
        add("no", "No", "Tidak", "否");
        add("biometricMissing", "No fingerprint hardware reported", "Tiada perkakasan cap jari dilaporkan", "未检测到指纹硬件");
        add("biometricReadyPass", "Biometric authentication is available", "Pengesahan biometrik tersedia", "生物验证可用");
        add("biometricReadyNeedPrompt", "Fingerprint hardware is ready. Tap Start fingerprint verification to confirm the fingerprint reader works.", "Perkakasan cap jari sedia. Tekan Mula pengesahan cap jari untuk sahkan pembaca cap jari berfungsi.", "指纹硬件已就绪。请点击开始指纹验证，确认指纹模组可用。");
        add("fingerprintReadyNeedPrompt", "Fingerprint/system verification is ready. Tap Start fingerprint/system verification.", "Pengesahan cap jari/sistem sedia. Tekan Mula pengesahan cap jari/sistem.", "指纹/系统验证已就绪，请点击开始指纹/系统验证。");
        add("biometricNoneEnrolled", "Fingerprint hardware exists, but no fingerprint is enrolled", "Perkakasan cap jari ada, tetapi cap jari belum didaftarkan", "有指纹硬件，但尚未录入指纹");
        add("fingerprintNoneEnrolled", "No fingerprint is enrolled or enrollment is unavailable. Fingerprint marked failed.", "Tiada cap jari didaftarkan atau pendaftaran tidak tersedia. Cap jari ditanda gagal.", "未录入指纹或无法录入指纹，指纹检测判定失败。");
        add("biometricUnavailable", "Biometric authentication is currently unavailable", "Pengesahan biometrik belum tersedia sekarang", "生物验证目前不可用");
        add("fingerprintUnavailable", "Fingerprint verification is unavailable. Fingerprint marked failed.", "Pengesahan cap jari tidak tersedia. Cap jari ditanda gagal.", "指纹验证不可用，指纹检测判定失败。");
        add("biometricReview", "Biometric status needs manual review", "Status biometrik perlu semakan manual", "生物验证状态需要人工复核");
        add("fingerprintReview", "Fingerprint/system verification needs review", "Pengesahan cap jari/sistem perlu semakan", "指纹/系统验证需要复核");
        add("biometricLimit", "Full confirmation requires testing the system fingerprint prompt or enrolling a fingerprint in settings.", "Pengesahan penuh perlukan ujian prompt cap jari sistem atau pendaftaran cap jari dalam tetapan.", "完整确认需要测试系统指纹弹窗，或在设置中录入指纹。");
        add("startBiometricPrompt", "Start fingerprint verification", "Mula pengesahan cap jari", "开始指纹验证");
        add("checkBiometricNow", "Check biometric now", "Semak biometrik sekarang", "立即检查生物验证");
        add("openBiometricSettings", "Open fingerprint settings", "Buka tetapan cap jari", "打开指纹设置");
        add("openFingerprintSettings", "Open fingerprint enrollment", "Buka daftar cap jari", "打开指纹录入");
        add("refreshFingerprintStatus", "Refresh fingerprint status", "Segar semula status cap jari", "重新检查指纹状态");
        add("biometricPromptTitle", "Verify fingerprint", "Sahkan cap jari", "验证指纹");
        add("biometricPromptSubtitle", "Use the phone's fingerprint prompt", "Gunakan prompt cap jari telefon", "使用手机系统指纹弹窗");
        add("biometricPromptPass", "Fingerprint verification passed", "Pengesahan cap jari lulus", "指纹验证通过");
        add("biometricCredentialPass", "System verification passed", "Pengesahan sistem lulus", "系统验证通过");
        add("biometricCredentialSubtitle", "Use fingerprint, PIN or pattern if available", "Guna cap jari, PIN atau corak jika tersedia", "可使用指纹、PIN 或图案");
        add("biometricPromptFailed", "Verification failed. Try again.", "Pengesahan gagal. Cuba lagi.", "验证失败，请重试。");
        add("biometricPromptError", "Fingerprint verification could not run", "Pengesahan cap jari tidak dapat dijalankan", "无法运行指纹验证");
        add("biometricPromptUnsupported", "System biometric prompt is not supported on this Android version", "Prompt biometrik sistem tidak disokong pada versi Android ini", "此 Android 版本不支持系统生物验证弹窗");
        add("biometricNeedEnroll", "Fingerprint cannot be verified or enrolled now. Fingerprint marked failed.", "Cap jari tidak boleh disahkan atau didaftarkan sekarang. Cap jari ditanda gagal.", "当前无法验证或录入指纹，指纹检测判定失败。");
        add("cancel", "Cancel", "Batal", "取消");
        add("markBiometricFail", "Mark biometric as failed", "Tanda biometrik gagal", "标记生物验证异常");
        add("biometricUserFail", "User marked biometric problem", "Pengguna tandakan masalah biometrik", "用户标记生物验证异常");
        add("biometricSettingsFailed", "Could not open biometric settings", "Tidak dapat buka tetapan biometrik", "无法打开生物验证设置");
        add("touchGuide", "FULLSCREEN TOUCH TEST V5: drag across the entire screen. Press any volume key to finish.", "UJIAN SENTUHAN SKRIN PENUH V5: seret ke seluruh skrin. Tekan butang volume untuk tamat.", "全屏触控测试 V5：请划过整个屏幕。按任意音量键结束。");
        add("touchOverlay", "V5 fine grid touch - press volume key to finish", "V5 grid halus - tekan volume untuk tamat", "V5 细格全屏触控 - 按音量键结束");
        add("saveTouch", "Save touch result", "Simpan keputusan sentuhan", "保存触控结果");
        add("coverage", "Coverage", "Liputan", "覆盖率");
        add("screenGuide", "Check for dead pixels, lines, burn-in or color spots.", "Semak piksel mati, garis, burn-in atau tompok warna.", "检查坏点、线条、烧屏或色斑。");
        add("screenTapNext", "Tap screen for next color", "Sentuh skrin untuk warna seterusnya", "触碰屏幕切换颜色");
        add("screenConfirmTitle", "Screen color result", "Keputusan warna skrin", "屏幕颜色测试结果");
        add("white", "White", "Putih", "白色");
        add("black", "Black", "Hitam", "黑色");
        add("red", "Red", "Merah", "红色");
        add("green", "Green", "Hijau", "绿色");
        add("blue", "Blue", "Biru", "蓝色");
        add("nextColor", "Next color", "Warna seterusnya", "下一个颜色");
        add("screenLooksGood", "Screen looks good", "Skrin nampak baik", "屏幕正常");
        add("screenProblem", "I see a problem", "Saya nampak masalah", "发现问题");
        add("screenPass", "User confirmed color test", "Pengguna sahkan ujian warna", "用户确认纯色测试正常");
        add("screenFail", "User reported display issue", "Pengguna lapor masalah paparan", "用户报告屏幕异常");
        add("buttonGuide", "Press Volume Up and Volume Down. Each key will light up when detected.", "Tekan Volume Up dan Volume Down. Setiap butang akan menyala apabila dikesan.", "请分别按音量加和音量减。检测到后对应区域会亮起。");
        add("buttonsPass", "Both volume buttons detected", "Kedua-dua butang volume dikesan", "已检测到两个音量键");
        add("volumeTestTitle", "Volume key live test", "Ujian langsung butang volume", "音量键实时测试");
        add("volumeWaiting", "Waiting for both volume keys", "Menunggu kedua-dua butang volume", "等待两个音量键输入");
        add("volumeBothDetected", "Both keys detected. Continue when ready.", "Kedua-dua butang dikesan. Teruskan apabila sedia.", "两个音量键都已检测到。确认后继续。");
        add("volumeUp", "Volume Up", "Volume Naik", "音量加");
        add("volumeDown", "Volume Down", "Volume Turun", "音量减");
        add("buttonsContinue", "Volume keys passed - continue", "Butang volume lulus - teruskan", "音量键通过 - 继续");
        add("markButtonsFail", "Mark volume keys as failed", "Tanda butang volume gagal", "标记音量键异常");
        add("buttonsUserFail", "User marked volume key problem", "Pengguna tandakan masalah butang volume", "用户标记音量键异常");
        add("conditionTitle", "Phone condition", "Keadaan telefon", "手机成色");
        add("conditionGuide", "Please enter the visible physical condition. These fields affect the estimated trade-in price.", "Sila isi keadaan fizikal yang boleh dilihat. Maklumat ini akan mempengaruhi anggaran harga trade-in.", "请填写可见外观成色。这些项目会影响回收预估价。");
        add("overallCondition", "Overall condition", "Keadaan keseluruhan", "整体成色");
        add("screenScratch", "Screen scratches", "Calar pada skrin", "屏幕刮花");
        add("backCoverCondition", "Back cover condition", "Keadaan penutup belakang", "后壳状态");
        add("conditionExcellent", "Excellent - like new", "Sangat baik - seperti baru", "优秀 - 接近全新");
        add("conditionGood", "Good - normal use", "Baik - penggunaan biasa", "良好 - 正常使用痕迹");
        add("conditionFair", "Fair - visible wear", "Sederhana - kesan jelas", "一般 - 明显使用痕迹");
        add("conditionPoor", "Poor - heavy wear", "Teruk - kesan berat", "较差 - 严重磨损");
        add("scratchNone", "No scratches", "Tiada calar", "无刮花");
        add("scratchLight", "Light hairline scratches", "Calar halus ringan", "轻微细纹");
        add("scratchVisible", "Visible scratches", "Calar jelas", "明显刮花");
        add("scratchHeavy", "Heavy scratches / deep marks", "Calar teruk / kesan dalam", "严重刮花 / 深痕");
        add("backGood", "Good back cover", "Penutup belakang baik", "后壳良好");
        add("backLightWear", "Light wear on back", "Kesan ringan pada belakang", "后壳轻微磨损");
        add("backDents", "Dents / paint peel", "Kemek / cat tertanggal", "凹痕 / 掉漆");
        add("backCracked", "Cracked or broken back", "Belakang retak atau pecah", "后壳破裂");
        add("saveCondition", "Save condition", "Simpan keadaan", "保存成色");
        add("conditionSummary", "Condition summary", "Ringkasan keadaan", "成色总结");
        add("conditionDeduction", "Condition deduction", "Potongan keadaan", "成色扣减");
        add("conditionNotSet", "Not entered", "Belum diisi", "未填写");
        add("estimate", "Estimate", "Anggaran", "估价");
        add("estimateTitle", "Estimated trade-in price", "Anggaran harga trade-in", "回收预估价");
        add("matched", "Matched device version:", "Versi peranti dipadankan:", "匹配设备版本：");
        add("base", "Base price", "Harga asas", "基础价格");
        add("deduction", "Condition deduction", "Potongan keadaan", "成色扣减");
        add("updated", "Price table updated", "Jadual harga dikemas kini", "价格表更新");
        add("testSummary", "Test summary", "Ringkasan ujian", "检测总结");
        add("disclaimer", "This is an estimated price. Final offer depends on manual inspection.", "Ini ialah anggaran harga. Tawaran akhir bergantung pada pemeriksaan manual.", "这是预估价格。最终报价以人工验机结果为准。");
        add("savePdf", "Save PDF report", "Simpan laporan PDF", "保存 PDF 报告");
        add("pdfSaved", "PDF saved", "PDF disimpan", "PDF 已保存");
        add("pdfFailed", "Could not save PDF", "Tidak dapat simpan PDF", "无法保存 PDF");
        add("submit", "Submit for review", "Hantar untuk semakan", "提交复核");
        add("submitTitle", "Manual review", "Semakan manual", "人工复核");
        add("submitBody", "Enter customer details, then send the locked PDF report to WhatsApp for final review.", "Isi butiran pelanggan, kemudian hantar laporan PDF ke WhatsApp untuk semakan akhir.", "填写客户资料后，直接把 PDF 报告发送到 WhatsApp 做最终复核。");
        add("name", "Name", "Nama", "姓名");
        add("phone", "Phone / WhatsApp", "Telefon / WhatsApp", "电话 / WhatsApp");
        add("notes", "Notes", "Nota", "备注");
        add("notesHint", "Screen protector, box included...", "Pelindung skrin, kotak disertakan...", "贴膜、盒子齐全等...");
        add("send", "Send review request", "Hantar permintaan semakan", "发送复核请求");
        add("sendWhatsapp", "Send PDF by WhatsApp", "Hantar PDF melalui WhatsApp", "通过 WhatsApp 发送 PDF");
        add("whatsappPdfCaption", "Phone check PDF report attached.", "Laporan PDF semakan telefon dilampirkan.", "已附上手机检测 PDF 报告。");
        add("whatsappTitle", "Phone Check Review Request", "Permintaan Semakan Telefon", "手机检测复核请求");
        add("whatsappMissing", "WhatsApp could not be opened", "WhatsApp tidak dapat dibuka", "无法打开 WhatsApp");
    }

    private void seedDeveloperText() {
        add("developerConsole", "Developer Online Console", "Konsol Online Developer", "开发者联网控制台");
        add("developerUnlocked", "Developer Online Console unlocked", "Konsol Developer dibuka", "已开启开发者控制台");
        add("onlineTestSettings", "Online test settings", "Tetapan ujian online", "联网测试设置");
        add("onlineTestSettingsBody", "Use Netlify for APK updates and Google Apps Script for compatibility report upload.", "Gunakan Netlify untuk kemas kini APK dan Google Apps Script untuk muat naik laporan keserasian.", "使用 Netlify 做 APK 更新，使用 Google Apps Script 上传兼容性测试报告。");
        add("netlifyVersionUrl", "Netlify version.json URL", "URL version.json Netlify", "Netlify version.json 链接");
        add("appsScriptUploadUrl", "Google Apps Script upload URL", "URL muat naik Google Apps Script", "Google Apps Script 上传链接");
        add("saveOnlineSettings", "Save online settings", "Simpan tetapan online", "保存联网设置");
        add("saved", "Saved", "Disimpan", "已保存");
        add("currentTestDevice", "Current test device", "Peranti ujian semasa", "当前测试手机");
        add("logFile", "Log file", "Fail log", "日志文件");
        add("actions", "Actions", "Tindakan", "操作");
        add("ready", "Ready", "Sedia", "准备就绪");
        add("uploadCompatibilityReport", "Upload compatibility report", "Muat naik laporan keserasian", "上传兼容性报告");
        add("checkApkUpdate", "Check APK update", "Semak kemas kini APK", "检查 APK 更新");
        add("internalHardwarePanel", "Internal hardware test panel", "Panel ujian perkakasan dalaman", "内部硬件测试面板");
        add("shareLocalDebugLog", "Share local debug log", "Kongsi log debug tempatan", "分享本机调试日志");
        add("clearLocalDebugLog", "Clear local debug log", "Kosongkan log debug tempatan", "清空本机调试日志");
        add("localLogCleared", "Local log cleared", "Log tempatan dikosongkan", "本机日志已清空");
        add("noLogToClear", "No log to clear", "Tiada log untuk dikosongkan", "没有可清空的日志");
        add("recentLogPreview", "Recent log preview", "Pratonton log terkini", "最近日志预览");
        add("internalHardwareTest", "Internal Hardware Test", "Ujian Perkakasan Dalaman", "内部硬件测试");
        add("fastCompatibilityTesting", "Fast compatibility testing", "Ujian keserasian pantas", "快速兼容性测试");
        add("fastCompatibilityBody", "Jump directly to any hardware test on this phone. Each jump, result, crash, and manual note is saved into the debug log for upload.", "Lompat terus ke mana-mana ujian perkakasan. Setiap lompatan, keputusan, crash, dan nota manual disimpan ke log debug untuk dimuat naik.", "可以直接跳到这台手机的任何硬件测试。每次跳转、结果、闪退和人工备注都会保存到调试日志，方便上传。");
        add("deviceName", "Device", "Peranti", "设备");
        add("jumpToTest", "Jump to test", "Lompat ke ujian", "跳转到测试");
        add("recordIssue", "Record issue", "Rekod masalah", "记录问题");
        add("recordIssueBody", "Write what happened on this phone: black screen, skipped page, wrong pass/fail result, permission problem, freeze, or crash after reopening.", "Tulis apa yang berlaku pada telefon ini: skrin hitam, halaman dilangkau, keputusan lulus/gagal salah, isu kebenaran, beku, atau crash selepas buka semula.", "写下这台手机发生的问题：黑屏、页面被跳过、通过/失败判断错误、权限问题、卡住，或重新打开后发现闪退。");
        add("recordIssueHint", "Example: Back camera black screen but app allowed pass", "Contoh: Kamera belakang skrin hitam tetapi aplikasi benarkan lulus", "例子：后镜头黑屏，但 App 仍显示通过");
        add("saveIssueToLog", "Save issue to debug log", "Simpan masalah ke log debug", "保存问题到调试日志");
        add("noIssueTextEntered", "No issue text entered", "Tiada teks masalah dimasukkan", "未填写问题内容");
        add("issueSavedToLog", "Issue saved to debug log", "Masalah disimpan ke log debug", "问题已保存到调试日志");
        add("reportActions", "Report actions", "Tindakan laporan", "报告操作");
        add("uploadThisPhoneReport", "Upload this phone test report", "Muat naik laporan telefon ini", "上传这台手机的测试报告");
        add("shareDebugLog", "Share debug log", "Kongsi log debug", "分享调试日志");
        add("backToDeveloperConsole", "Back to Developer Console", "Kembali ke Konsol Developer", "返回开发者控制台");
        add("recentEvents", "Recent events", "Peristiwa terkini", "最近事件");
        add("openUpdateUrl", "Open update URL in browser", "Buka URL kemas kini dalam browser", "用浏览器打开更新链接");
        add("checkingUpdate", "Checking update...", "Menyemak kemas kini...", "正在检查更新...");
        add("newVersionDownloading", "New version found, downloading", "Versi baru dijumpai, sedang muat turun", "发现新版本，正在下载");
        add("noNewerApk", "No newer APK found", "Tiada APK lebih baru", "没有发现更新版本");
        add("apkDownloadedOpening", "APK downloaded, opening installer", "APK dimuat turun, membuka pemasang", "APK 已下载，正在打开安装器");
        add("autoUploadOn", "Auto upload after each test: ON", "Auto muat naik selepas setiap ujian: ON", "每次测试后自动上传：开启");
        add("autoUploadOff", "Auto upload after each test: OFF", "Auto muat naik selepas setiap ujian: OFF", "每次测试后自动上传：关闭");
        add("autoUploadEnabled", "Auto upload enabled", "Auto muat naik diaktifkan", "已开启自动上传");
        add("autoUploadDisabled", "Auto upload disabled", "Auto muat naik dimatikan", "已关闭自动上传");
        add("backInternalShort", "Internal", "Dalaman", "返回内部");
        add("noInternetUpload", "No internet connection. Report was saved locally and can be uploaded later.", "Tiada sambungan internet. Laporan telah disimpan dalam telefon dan boleh dimuat naik kemudian.", "没有网络连接。报告已保存在本机，之后联网可以再上传。");
    }

    private enum Status {
        PENDING, PASS, FAIL, REVIEW;

        String label(MainActivity app) {
            if (this == PASS) return app.t("pass");
            if (this == FAIL) return app.t("fail");
            if (this == REVIEW) return app.t("review");
            return app.t("pending");
        }

        int color() {
            if (this == PASS) return GREEN;
            if (this == FAIL) return RED;
            if (this == REVIEW) return AMBER;
            return MUTED;
        }
    }

    private static class Result {
        final Status status;
        final String note;

        Result(Status status, String note) {
            this.status = status;
            this.note = note;
        }
    }

    private class TouchPad extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean[][] grid = new boolean[18][32];

        TouchPad(Context context) {
            super(context);
            setBackgroundColor(Color.WHITE);
            paint.setColor(GREEN);
            paint.setStrokeWidth(dp(16));
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.rgb(216, 222, 229));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(2, 2, getWidth() - 2, getHeight() - 2, paint);
            paint.setColor(GREEN);
            paint.setStyle(Paint.Style.FILL);
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    if (grid[x][y]) {
                        float left = x * getWidth() / 18f;
                        float top = y * getHeight() / 32f;
                        canvas.drawRect(left, top, left + getWidth() / 18f, top + getHeight() / 32f, paint);
                    }
                }
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(23, 32, 42));
            paint.setTextSize(dp(14));
            canvas.drawText(t("touchOverlay"), dp(12), dp(28), paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int x = Math.max(0, Math.min(17, (int) (event.getX() / Math.max(1, getWidth()) * 18)));
            int y = Math.max(0, Math.min(31, (int) (event.getY() / Math.max(1, getHeight()) * 32)));
            grid[x][y] = true;
            invalidate();
            return true;
        }

        int coveragePercent() {
            int touched = 0;
            int total = 18 * 32;
            for (boolean[] column : grid) {
                for (boolean cell : column) if (cell) touched++;
            }
            return Math.round(touched * 100f / total);
        }
    }
}
