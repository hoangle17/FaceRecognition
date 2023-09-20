package com.example.facerecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.PixelCopy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.facerecognition.config.APIClient;
import com.example.facerecognition.custom.FocusView;
import com.example.facerecognition.model.TypeImageRecognition;
import com.example.facerecognition.service.EKycService;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private SurfaceView surfaceView;
    private CameraSource cameraSource;
    private AppCompatImageView imgCenter, imgRight, imgLeft;
    private View viewCapture;
    private boolean isStraightPass, isRightPass, isLeftPass, isCalling;

    private EKycService eKycService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surface_view);
        eKycService = APIClient.getClient().create(EKycService.class);

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
        imgCenter = findViewById(R.id.imgCenter);
        imgRight = findViewById(R.id.imgRight);
        imgLeft = findViewById(R.id.imgLeft);
        viewCapture = findViewById(R.id.viewCapture);
    }

    private void captureViewOnScreen(SurfaceView view, TypeImageRecognition type) {
        // Create a bitmap with the same size as the view
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a rect for the entire view
        Rect rect = new Rect(0, 0, view.getWidth(), view.getHeight());

        // Perform the pixel copy operation
        PixelCopy.request(view, rect, bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS) {
                //
                if (type == TypeImageRecognition.NAME_IMG_CENTER) {
                    imgCenter.setImageBitmap(cropImage2(bitmap));
                }
                if (type == TypeImageRecognition.NAME_IMG_LEFT) {
                    imgLeft.setImageBitmap(cropImage2(bitmap));
                }
                if (type == TypeImageRecognition.NAME_IMG_RIGHT) {
                    imgRight.setImageBitmap(cropImage2(bitmap));
                }
                //
                ImageUtils.saveBitmapAsJpeg(cropImage2(bitmap), this, type.value);

                if (!isCalling) callApiCheckImg(type);
            } else {
                // The pixel copy operation failed
                Toast.makeText(MainActivity.this, "Unable to capture view", Toast.LENGTH_SHORT).show();
            }
        }, new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Log.e(TAG, "Camera permission not granted.");
            }
        }
    }

    private void startCamera() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create the face detector
        FaceDetector faceDetector = new FaceDetector.Builder(this).setTrackingEnabled(false).setMode(FaceDetector.FAST_MODE).build();

        // Create the camera source using the face detector
        cameraSource = new CameraSource.Builder(this, faceDetector).setFacing(CameraSource.CAMERA_FACING_FRONT).setRequestedFps(30).build();
        // Add callback to the preview surface
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {
                    Log.e(TAG, "Failed to start camera preview.", e);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // No implementation needed
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        // Set the face detector's processor
        faceDetector.setProcessor(new Detector.Processor<Face>() {
            @Override
            public void release() {
                // Handle release logic if needed
            }

            @Override
            public void receiveDetections(@NonNull Detector.Detections<Face> detections) {
                if (isRightPass && isStraightPass && isLeftPass) {
                    return;
                }
                SparseArray<Face> faces = detections.getDetectedItems();

                for (int i = 0; i < faces.size(); i++) {
                    Face face = faces.valueAt(i);
                    float eulerY = face.getEulerY(); // Y-axis rotation of the face in degrees

                    if (!isCalling) {
                        if (eulerY < -30) {
                            // Face is facing right
                            Log.d(TAG, "Face is facing right");
                            if (!isRightPass) {
                                captureViewOnScreen(surfaceView, TypeImageRecognition.NAME_IMG_RIGHT);
                            }
                        } else if (eulerY > 30) {
                            // Face is facing left
                            Log.d(TAG, "Face is facing left");
                            if (!isLeftPass) {
                                captureViewOnScreen(surfaceView, TypeImageRecognition.NAME_IMG_LEFT);
                            }
                        } else {
                            // Face is facing straight ahead
                            Log.d(TAG, "Face is facing straight ahead");
                            if (!isStraightPass) {
                                captureViewOnScreen(surfaceView, TypeImageRecognition.NAME_IMG_CENTER);
                            }
                        }
                    }
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Release the resources
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private Bitmap cropImage2(Bitmap originalBitmap) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        double topCompactRatio = 0.2; // Replace with your desired top compact ratio
        double bottomCompactRatio = 0.4; // Replace with your desired bottom compact ratio
        int topCompactHeight = (int) (originalHeight * topCompactRatio);
        int bottomCompactHeight = (int) (originalHeight * bottomCompactRatio);
        int cropHeight = originalHeight - topCompactHeight - bottomCompactHeight;
        return Bitmap.createBitmap(originalBitmap, 0, topCompactHeight, originalWidth, cropHeight);
    }

    private void callApiCheckImg(TypeImageRecognition type) {
        isCalling = true;
        // Create RequestBody for bucket_id
        RequestBody bucketIdRequestBody = RequestBody.create(MediaType.parse("text/plain"), "ekyc");
        RequestBody gesture = RequestBody.create(MediaType.parse("text/plain"), type.value);
        RequestBody img_name = RequestBody.create(MediaType.parse("text/plain"), type.value);
        // Create MultipartBody.Part for the file
        RequestBody fileRequestBody = RequestBody.create(MediaType.parse("image/jpeg"), ImageUtils.getJpeg(this, type.value));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", type.value, fileRequestBody);
        Call<Object> call = eKycService.uploadFile(bucketIdRequestBody, gesture, img_name, filePart);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                isCalling = false;
                Log.d("uploadFile", "onResponse");
                if (type == TypeImageRecognition.NAME_IMG_CENTER) {
                    isStraightPass = true;
                }
                if (type == TypeImageRecognition.NAME_IMG_LEFT) {
                    isLeftPass = true;
                }
                if (type == TypeImageRecognition.NAME_IMG_RIGHT) {
                    isRightPass = true;
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                isCalling = false;
                Log.d("uploadFile", "onFailure");
            }
        });
    }
}
