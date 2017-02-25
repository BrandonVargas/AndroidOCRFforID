/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mx.brandonvargas.ocrforid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiDetector;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mx.brandonvargas.ocrforid.camera.CameraSource;
import mx.brandonvargas.ocrforid.camera.CameraSourcePreview;
import mx.brandonvargas.ocrforid.camera.GraphicOverlay;
import mx.brandonvargas.ocrforid.modifiedGoogleCode.FaceTrackerFactory;
import mx.brandonvargas.ocrforid.modifiedGoogleCode.OcrDetectorProcessor;
import mx.brandonvargas.ocrforid.modifiedGoogleCode.OcrGraphic;
import mx.brandonvargas.ocrforid.util.Constants;
import mx.brandonvargas.ocrforid.util.DocumentIdentifier;
import mx.brandonvargas.ocrforid.util.Percentage;

/**
 * Activity for the Ocr Detecting app.  This app detects text and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and contents of each TextBlock.
 */
public final class OcrIdActivity extends AppCompatActivity implements DocumentIdentifier {
    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    private static final String TAG = "OcrIdActivity";
    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static Uri uri1;
    boolean isSamsung = false;
    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private MultiDetector multiDetector;
    private TextRecognizer textRecognizer;
    private ImageView mImageViewClear;
    private ImageView mImageViewFlash;
    private TextView textView1;
    private TextView textView2;
    //Shooter Button
    private Button shoot;
    private PercentRelativeLayout rectangleIfeFront, rectangleIfeBack, rectangleIneBack, detectShape;
    private ProgressDialog indeterminateDialog;
    //Identification variables
    private int type = -1;
    private int faceSide = -1;
    private boolean backTaked = false;
    private boolean isFlashOn = false;
    private boolean firstTime = true;

    /**
     * Create a file Uri for saving an image
     */
    private static Uri getOutputMediaFileUri() {
        return Uri.fromFile(getOutputMediaFile());
    }

    /**
     * Create a File for saving an image
     */
    private static File getOutputMediaFile() {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "AndroidOCR");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e("ERROR", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.ocr_capture);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);
        shoot = (Button) findViewById(R.id.button_shutter);
        shoot.setEnabled(false);
        textView1 = (TextView) findViewById(R.id.text_view_scan);
        mImageViewClear = (ImageView) findViewById(R.id.image_view_clear);
        mImageViewFlash = (ImageView) findViewById(R.id.image_view_flash);
        rectangleIfeFront = (PercentRelativeLayout) findViewById(R.id.rl_ife_view_front);
        rectangleIfeBack = (PercentRelativeLayout) findViewById(R.id.rl_ife_view_back);
        rectangleIneBack = (PercentRelativeLayout) findViewById(R.id.rl_ine_view_back);
        detectShape = (PercentRelativeLayout) findViewById(R.id.rl_view_detect);

        // Set good defaults for capturing text.
        boolean autoFocus = true;
        boolean useFlash = false;

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int rc2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rc == PackageManager.PERMISSION_GRANTED && rc2 == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return super.onTouchEvent(e);
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     * <p>
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated multi-processor instance
        // is set to receive the text recognition results, track the text, and maintain
        // graphics for each text block on screen.  The factory is used by the multi-processor to
        // create a separate tracker instance for each text block.
        textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay, this));
        // A face detector is created to track faces.  An associated multi-processor instance
        // is set to receive the face detection results, track the faces, and maintain graphics for
        // each face on screen.  The factory is used by the multi-processor to create a separate
        // tracker instance for each face.
        FaceDetector faceDetector = new FaceDetector.Builder(context).build();
        FaceTrackerFactory faceFactory = new FaceTrackerFactory(mGraphicOverlay, this);
        faceDetector.setProcessor(
                new MultiProcessor.Builder<>(faceFactory).build());

        // A multi-detector groups the two detectors together as one detector.  All images received
        // by this detector from the camera will be sent to each of the underlying detectors, which
        // will each do face and barcode detection, respectively.  The detection results from each
        // are then sent to associated tracker instances which maintain per-item graphics on the
        // screen.
        multiDetector = new MultiDetector.Builder()
                .add(faceDetector)
                .add(textRecognizer)
                .build();


        if (!multiDetector.isOperational()) {
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), multiDetector)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1600, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();

    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
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

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }
        if (android.os.Build.MANUFACTURER.equals("samsung"))
            isSamsung = true;
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
                shoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCameraSource.takePicture(new CameraSource.ShutterCallback() {
                            @Override
                            public void onShutter() {
                                shoot.setEnabled(false);
                            }
                        }, new CameraSource.PictureCallback() {
                            @Override
                            public void onPictureTaken(byte[] data) {
                                processImage(data);
                            }
                        });
                    }
                });
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void processImage(byte[] data) {
        Uri fileUri = getOutputMediaFileUri();
        File pictureFile = new File(fileUri.getPath());
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            String filePath = pictureFile.getPath();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            Bitmap bitmap2;
            if (isSamsung) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                if (bitmap.getWidth() == bitmap.getHeight()) {
                    int xc = (int) ((bitmap.getWidth() / 2) - (bitmap.getWidth() * 0.213));
                    int yc = (int) ((bitmap.getHeight() / 2) - (bitmap.getHeight() * 0.327));
                    Bitmap bitmapTemp = Bitmap.createBitmap(bitmap, xc, yc, (int) (bitmap.getWidth() * 0.425), (int) (bitmap.getHeight() * 0.654));
                    bitmap2 = Bitmap.createBitmap(bitmapTemp, 0, 0, bitmapTemp.getWidth(), bitmapTemp.getHeight(), matrix, true);
                } else {
                    int xc = (int) ((bitmap.getWidth() / 2) - (bitmap.getWidth() * 0.37));
                    int yc = (int) ((bitmap.getHeight() / 2) - (bitmap.getHeight() * 0.19));
                    Bitmap bitmapTemp = Bitmap.createBitmap(bitmap, xc, yc, (int) (bitmap.getWidth() * 0.74), (int) (bitmap.getHeight() * 0.37));
                    bitmap2 = Bitmap.createBitmap(bitmapTemp, 0, 0, bitmapTemp.getWidth(), bitmapTemp.getHeight(), matrix, true);
                }
            } else if (bitmap.getWidth() > bitmap.getHeight()) {
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                int xc = (int) ((bitmap.getWidth() / 2) - (bitmap.getWidth() * 0.1092));
                int yc = (int) ((bitmap.getHeight() / 2) - (bitmap.getHeight() * 0.375));
                bitmap2 = Bitmap.createBitmap(bitmap, xc, yc, (int) (bitmap.getWidth() * 0.3632), (int) (bitmap.getHeight() * 0.75), matrix, true);
            } else {
                int xc = (int) ((bitmap.getWidth() / 2) - (bitmap.getWidth() * 0.37));
                int yc = (int) ((bitmap.getHeight() / 2) - (bitmap.getHeight() * 0.114));
                bitmap2 = Bitmap.createBitmap(bitmap, xc, yc, (int) (bitmap.getWidth() * 0.74), (int) (bitmap.getHeight() * 0.37));
            }
            shoot.setEnabled(true);
            uCrop(getImageUri(OcrIdActivity.this, bitmap2));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public void setFaceSide(int faceSide) {
        this.faceSide = faceSide;
        if (this.faceSide > -1 && this.type > -1) {
            checkType();
        }
    }

    public void checkType() {
        if (firstTime) {
            detectShape.setVisibility(View.GONE);
            switch (this.faceSide) {
                case Constants.FACE_RIGHT:
                    if (type == Constants.IFEB) {
                        rectangleIfeFront.setVisibility(View.VISIBLE);
                        /*this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(OcrIdActivity.this, "IFE-B o IFE-C", Toast.LENGTH_SHORT).show();
                            }
                        });*/
                    } else if (type == Constants.IFEE) {
                        rectangleIfeFront.setVisibility(View.VISIBLE);
                        /*this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(OcrIdActivity.this, "IFE-D o INE-E", Toast.LENGTH_SHORT).show();
                            }
                        });*/
                    }
                    break;
                case Constants.FACE_LEFT:
                    if (type == Constants.IFEE || type == Constants.IFEB) {
                        rectangleIfeFront.setVisibility(View.VISIBLE);
                        /*this.runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(OcrIdActivity.this, "IFE-D o INE-E", Toast.LENGTH_SHORT).show();
                            }
                        });*/
                    }
                    break;
            }
            shoot.setEnabled(true);
            firstTime = false;
        }
        Log.e("LadoCara/TipoIFE", faceSide + "/" + type);
    }

    private void uCrop(Uri fileUri) {
        UCrop.Options options = new UCrop.Options();
        options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL);
        options.setCompressionQuality(100);
        options.setHideBottomControls(true);
        UCrop.of(fileUri, fileUri)
                .withAspectRatio(8.6f, 5.4f)
                .withMaxResultSize(3000, 3000)
                .withOptions(options)
                .start(OcrIdActivity.this);
    }

    private void showProgressDialog() {
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Espere por favor");
        indeterminateDialog.setMessage("Realizando OCR");
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            textView1.setText(R.string.identify_document);
            if (backTaked) {
                performOCRIFE(type, uri1, UCrop.getOutput(data));
                backTaked = false;
            } else {
                rectangleIfeFront.setVisibility(View.GONE);
                rectangleIfeBack.setVisibility(View.GONE);
                rectangleIneBack.setVisibility(View.GONE);
                uri1 = UCrop.getOutput(data);
                backTaked = true;
                if (type == Constants.IFEB || type == Constants.IFEC) {
                    rectangleIfeBack.setVisibility(View.VISIBLE);
                } else if (type == Constants.IFED || type == Constants.IFEE) {
                    rectangleIneBack.setVisibility(View.VISIBLE);
                }
            }
        } else {
            //final Throwable cropError = UCrop.getError(data);
        }
    }

    private void performOCRIFE(int type, Uri uri1, Uri uri2) {
        Percentage p;
        boolean isIne = false;
        String name = "", curp = "", address = "", id = "", motherLastName = "", lastName = "", electorKey = "",
                state = "", town = "", section = "", cic = "";
        String[] fullName;
        showProgressDialog();
        switch (type) {
            case Constants.IFEB:
            case Constants.IFEC:
                p = new Percentage(0.02f, 0.25f, 0.45f, 0.30f);
                fullName = extractNameIFE(performOCR(getBitmapCroppedFromUri(uri1, p, false)));
                name = fullName[2];
                motherLastName = fullName[1];
                lastName = fullName[0];
                p = new Percentage(0.098f, 0.74f, 0.33f, 0.092f);
                curp = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.02f, 0.48f, 0.55f, 0.22f);
                address = extractAddressIFE(performOCR(getBitmapCroppedFromUri(uri1, p, false)));
                p = new Percentage(0.22f, 0.69f, 0.37f, 0.092f);
                electorKey = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.119f, 0.8f, 0.046f, 0.08f);
                state = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.289f, 0.8f, 0.08f, 0.08f);
                town = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.286f, 0.833f, 0.1f, 0.09f);
                section = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.13f, 0.02f, 0.60f, 0.1f);
                id = extractIdIFE(performOCR(getBitmapCroppedFromUri(uri2, p, true)));
                break;
            case Constants.IFED:
            case Constants.IFEE:
                isIne = true;
                p = new Percentage(0.30f, 0.25f, 0.30f, 0.30f);
                fullName = extractNameIFE(performOCR(getBitmapCroppedFromUri(uri1, p, false)));
                name = fullName[2];
                motherLastName = fullName[1];
                lastName = fullName[0];
                p = new Percentage(0.372f, 0.685f, 0.32f, 0.09f);
                curp = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.29f, 0.46f, 0.47f, 0.32f);
                address = extractAddressIFE(performOCR(getBitmapCroppedFromUri(uri1, p, false)));
                p = new Percentage(0.488f, 0.648f, 0.37f, 0.092f);
                electorKey = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.395f, 0.77f, 0.1f, 0.08f);
                state = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.622f, 0.77f, 0.08f, 0.08f);
                town = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.79f, 0.77f, 0.1f, 0.08f);
                section = performOCR(getBitmapCroppedFromUri(uri1, p, false));
                p = new Percentage(0.197f, 0.64f, 0.311f, 0.13f);
                cic = extractIdIFE(performOCR(getBitmapCroppedFromUri(uri2, p, false)));
                p = new Percentage(0.546f, 0.64f, 0.406f, 0.13f);
                id = extractIdIFE(performOCR(getBitmapCroppedFromUri(uri2, p, false)));
                break;
        }
        indeterminateDialog.dismiss();
        Intent resultIntent = new Intent();
        Bundle extras = new Bundle();
        extras.putBoolean("IS_INE", isIne);
        extras.putString("NAME", name);
        extras.putString("LAST_NAME", lastName);
        extras.putString("M_LAST_NAME", motherLastName);
        extras.putString("CURP", curp);
        extras.putString("ADDRESS", address);
        extras.putString("ELECTOR", electorKey);
        extras.putString("STATE", state);
        extras.putString("TOWN", town);
        extras.putString("SECTION", section);
        extras.putString("ID", id);
        if (!TextUtils.isEmpty(cic)) extras.putString("CIC", cic);
        extras.putString("URI1", uri1.toString());
        extras.putString("URI2", uri2.toString());
        resultIntent.putExtras(extras);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private String extractIdIFE(String s) {
        String[] lines = s.split("\r\n|\r|\n");
        String result = s.replaceAll("\\s+", "").replaceAll("[-+.^:,]", "");
        if (result.length() > 13) result = result.substring(0, 13);

        if (!TextUtils.isDigitsOnly(result)) {
            result = result.replace('b', '6').replace('L', '6').replace('l', '1').replace('i', '1').replace('I', '1')
                    .replace('y', '4').replace('o', '0').replace('O', '0').replace('s', '5').replace('S', '5')
                    .replace('z', '2').replace('Z', '2').replace('g', '9').replace('Y', '4').replace('e', '2')
                    .replace('?', '7').replace('E', '6');
        }
        return result;
    }

    private String extractAddressIFE(String s) {
        String result = "";
        String[] lines = s.split("\r\n|\r|\n");
        if (lines[0].contains("DOMICILIO") && lines.length > 3) {
            for (int i = 1; i <= 3; i++) {
                result += lines[i];
            }
        } else if (lines.length <= 3) {
            for (int i = 0; i <= 2; i++) {
                result += lines[i];
            }
        } else {
            for (int i = 1; i < lines.length; i++) {
                result += lines[i];
            }
        }
        return result;
    }

    private String extractBirthdayIFE(String s) {
        String[] lines = s.split("\r\n|\r|\n");
        String result = "";
        for (int i = 1; i < lines.length; i++) {
            result += lines[i];
        }
        result = s.replaceAll("\\s+", "");
        String re1 = ".*?";    // Non-greedy match on filler
        String re2 = "(\\d)";    // Any Single Digit 1
        String re3 = "(\\d)";    // Any Single Digit 2
        String re4 = "(\\d)";    // Any Single Digit 3
        String re5 = "(\\d)";    // Any Single Digit 4
        String re6 = "(\\d)";    // Any Single Digit 5
        String re7 = "(\\d)";    // Any Single Digit 6

        Pattern p = Pattern.compile(re1 + re2 + re3 + re4 + re5 + re6 + re7, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(result);
        if (m.find()) {
            String d1 = m.group(1);
            String d2 = m.group(2);
            String d3 = m.group(3);
            String d4 = m.group(4);
            String d5 = m.group(5);
            String d6 = m.group(6);
            result = (d5 + d6 + "/" + d3 + d4 + "/" + d1 + d2);
        }
        return result;
    }

    private String[] extractNameIFE(String s) {
        String result = "";
        String[] lines = s.split("\r\n|\r|\n");
        String[] fullName = new String[3];
        if (lines[0].contains("NOMBRE") && lines.length > 3) {
            for (int i = 1; i <= 3; i++) {
                fullName[i - 1] = lines[i];
            }
        } else if (lines.length >= 3) {
            for (int i = 0; i <= 2; i++) {
                fullName[i] = lines[i];
            }
        } else {
            for (int i = 1; i < lines.length; i++) {
                fullName[i] = lines[i];
            }
        }
        return fullName;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Picture", null);
        return Uri.parse(path);
    }

    public Bitmap getBitmapCroppedFromUri(Uri uri, Percentage p, boolean rotate) {
        Bitmap bitmap = null;
        Bitmap bitmap2 = null;
        Matrix m = new Matrix();
        m.postRotate(90);
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            if (rotate)
                bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rotate && bitmap2 != null) {
            bitmap = Bitmap.createBitmap(bitmap2, Math.round((p.getX()) * bitmap2.getWidth()), Math.round((p.getY()) * bitmap2.getHeight()), Math.round(p.getWidth() * bitmap2.getWidth()), Math.round(p.getHeight() * bitmap2.getHeight()));
            return bitmap;
        } else if (bitmap != null) {
            bitmap2 = Bitmap.createBitmap(bitmap, Math.round((p.getX()) * bitmap.getWidth()), Math.round((p.getY()) * bitmap.getHeight()), Math.round(p.getWidth() * bitmap.getWidth()), Math.round(p.getHeight() * bitmap.getHeight()));
            return bitmap2;
        }
        return null;
    }

    private String performOCR(Bitmap bitmap) {
        getImageUri(OcrIdActivity.this, bitmap);
        String textResult = "";
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        SparseArray<TextBlock> textblock = textRecognizer.detect(frame);
        TextBlock tb = null;
        List<Text> texto = new ArrayList<>();
        for (int i = 0; i < textblock.size(); i++) {
            tb = textblock.get(textblock.keyAt(i));
            Log.e("TEXT", tb.toString() + "");
            texto.addAll(tb.getComponents());
        }
        for (Text t : texto) {
            for (Text t2 : t.getComponents()) {
                textResult += t2.getValue() + " ";
            }
            textResult += "\n";
        }

        if (!textResult.equals("")) {
            bitmap.recycle();
            return textResult;
        } else {
            Toast toast = Toast.makeText(this, R.string.ocr_fail, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return "";
        }
    }

    public void finishActivity(View view) {
        finish();
    }

    public void toggleFlash(View view) {
        if (!isFlashOn) mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        else mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        try {
            mImageViewFlash.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);

            isFlashOn = !isFlashOn;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
