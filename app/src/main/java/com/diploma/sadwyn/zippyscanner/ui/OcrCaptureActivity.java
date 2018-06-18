
package com.diploma.sadwyn.zippyscanner.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.diploma.sadwyn.zippyscanner.R;
import com.diploma.sadwyn.zippyscanner.google.vision.CameraSource;
import com.diploma.sadwyn.zippyscanner.google.vision.CameraSourcePreview;
import com.diploma.sadwyn.zippyscanner.google.vision.GraphicOverlay;
import com.diploma.sadwyn.zippyscanner.google.vision.OcrDetectorProcessor;
import com.diploma.sadwyn.zippyscanner.google.vision.OcrGraphic;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public final class OcrCaptureActivity extends AppCompatActivity {
    private static final String TAG = "OcrCaptureActivity";

    public static final String DATA_FOLDER = Environment.getExternalStorageDirectory().toString() + "/Android/" + "data/" + "com.diploma.sadwyn.zippyscanner/";
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private FrameLayout topLayout;


    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private View scannerRow;

    private String toLang;
    private String fromLang;
    private boolean isTranslate;

    private TranslateAnimation translateAnimationScannerLine;
    private Animation translateAnimationCopyButtonIn;
    private Animation translateAnimationCopyButtonOut;

    private Button copyButton;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.ocr_capture);

        if (bundle != null) {
            toLang = bundle.getString("toLanguage");
            fromLang = bundle.getString("fromLanguage");
            isTranslate = bundle.getBoolean("isTranslate");
        } else if (getIntent() != null) {
            toLang = getIntent().getStringExtra("toLanguage");
            fromLang = getIntent().getStringExtra("fromLanguage");
            isTranslate = getIntent().getBooleanExtra("isTranslate", false);
        }
        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);
        scannerRow = findViewById(R.id.scannerRow);
        topLayout = findViewById(R.id.topLayout);
        copyButton = findViewById(R.id.copyButton);

        translateAnimationScannerLine = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 1.0f);
        translateAnimationScannerLine.setInterpolator(new AccelerateDecelerateInterpolator());
        translateAnimationScannerLine.setRepeatMode(Animation.REVERSE);
        translateAnimationScannerLine.setRepeatCount(Animation.INFINITE);
        translateAnimationScannerLine.setDuration(500);

        translateAnimationCopyButtonIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_right);
        translateAnimationCopyButtonOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_right);

        copyButton.setOnClickListener(view1 -> {
            StringBuilder copyMessage = new StringBuilder();

            for (String text : OcrDetectorProcessor.textToCopy) {
                copyMessage.append(text).append(" ");
            }
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            assert clipboard != null;
            clipboard.setPrimaryClip(ClipData.newPlainText("Zippy Scanned Text", copyMessage.toString().trim()));
            Toast.makeText(getApplicationContext(), "Copied", Toast.LENGTH_SHORT).show();
        });

        // Set good defaults for capturing text.
        boolean autoFocus = true;
        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraAndStoragePermission();
        }

        gestureDetector = new GestureDetector(this, new CaptureGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("toLanguage", toLang);
        outState.putString("fromLanguage", fromLang);
        outState.putBoolean("isTranslate", isTranslate);
        super.onSaveInstanceState(outState);
    }

    private void requestCameraAndStoragePermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        boolean b = scaleGestureDetector.onTouchEvent(e);

        boolean c = gestureDetector.onTouchEvent(e);

        return b || c || super.onTouchEvent(e);
    }

    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay, toLang, fromLang, isTranslate));

        topLayout.setOnClickListener(view -> {
            if (!OcrDetectorProcessor.isStopped) {
                scannerRow.clearAnimation();

                if (OcrDetectorProcessor.textToCopy.isEmpty())
                    copyButton.setVisibility(View.INVISIBLE);
                else {
                    copyButton.startAnimation(translateAnimationCopyButtonIn);
                    copyButton.setVisibility(View.VISIBLE);
                    writeScannedDataToFile();
                }
            } else {
                if (copyButton.getVisibility() == View.VISIBLE) {
                    copyButton.startAnimation(translateAnimationCopyButtonOut);
                }
                scannerRow.startAnimation(translateAnimationScannerLine);
            }
            OcrDetectorProcessor.isStopped = !OcrDetectorProcessor.isStopped;
        });

        if (!textRecognizer.isOperational()) {

            Log.w(TAG, "Detector dependencies are not yet available.");

            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    private void writeScannedDataToFile() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy_hh.mm.ss");
        Date date = new Date();
        String formattedDate = dateFormat.format(date);

        if (!new File(DATA_FOLDER).exists()) {
            new File(DATA_FOLDER).mkdirs();
        }

        File file = new File(DATA_FOLDER + OcrDetectorProcessor.textToCopy.get(0) + formattedDate + ".txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (DataOutputStream fos = new DataOutputStream(new FileOutputStream(file))) {
            for (String word : OcrDetectorProcessor.textToCopy) {
                byte[] bytes = word.getBytes();
                byte[] space = " ".getBytes();
                fos.write(bytes);
                fos.write(space);
                fos.flush();
            }
            Toast.makeText(this, "Text has been saved to file by path" + DATA_FOLDER, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error while saving file", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = (dialog, id) -> finish();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    private void startCameraSource() throws SecurityException {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private boolean onTap(float rawX, float rawY) {
        OcrGraphic graphic = mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
        Text text = null;
        if (graphic != null) {
            text = graphic.getText();
            if (text != null && text.getValue() != null) {
                Log.d(TAG, "text data is being spoken! " + text.getValue());
                // Speak the string.
            } else {
                Log.d(TAG, "text data is null");
            }
        } else {
            Log.d(TAG, "no text detected");
        }
        return text != null;
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
        }
    }

    private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            if (mCameraSource != null) {
                mCameraSource.doZoom(detector.getScaleFactor());
            }
        }
    }
}
