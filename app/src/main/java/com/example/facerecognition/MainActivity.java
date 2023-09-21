package com.example.facerecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.PixelCopy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.facerecognition.config.APIClient;
import com.example.facerecognition.custom.CircleProgressBar;
import com.example.facerecognition.model.CompareAvatarRequest;
import com.example.facerecognition.model.CompareResponse;
import com.example.facerecognition.model.ResultUploadResponse;
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
    private boolean isStraightPass, isRightPass, isLeftPass, isCalling, checked;
    private EKycService eKycService;
    private EKycService eKycService2;

    private String idImgStraight = "";
    private final String idImgAvatarNFC = "id_avatar_nfc.jpg";
    private LinearLayout vStatusActive;
    private CircleProgressBar circleProgressBar;
    private AppCompatImageView icStatus, icBack ,imgLoadingCompare;
    private TextView tvStatus, tvStep;
    private int stepCurrent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceView = findViewById(R.id.surface_view);
        eKycService = APIClient.getClient().create(EKycService.class);
        eKycService2 = APIClient.getClient2().create(EKycService.class);

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
        imgCenter = findViewById(R.id.imgCenter);
        imgRight = findViewById(R.id.imgRight);
        imgLeft = findViewById(R.id.imgLeft);
        vStatusActive = findViewById(R.id.vStatusActive);
        icStatus = findViewById(R.id.icStatus);
        icBack = findViewById(R.id.icBack);
        tvStatus = findViewById(R.id.tvStatus);
        tvStep = findViewById(R.id.tvStep);
        imgLoadingCompare = findViewById(R.id.imgLoadingCompare);
        circleProgressBar = findViewById(R.id.circleProgressBar);
        setProgressCustom(100, Color.RED);
        icBack.setOnClickListener(view -> finish());
    }

    private void setProgressCustom(int i, int red) {
        circleProgressBar.setProgress(i);
        circleProgressBar.setColor(red);
    }

    private void captureViewOnScreen(SurfaceView view, TypeImageRecognition type) {
        // Create a bitmap with the same size as the view
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a rect for the entire view
        Rect rect = new Rect(0, 0, view.getWidth(), view.getHeight());

        // Perform the pixel copy operation
        PixelCopy.request(view, rect, bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS && !isCalling) {
                //
                if (type == TypeImageRecognition.NAME_IMG_CENTER) {
                    imgCenter.setImageBitmap(cropImage2(bitmap));
                    setProgressCustom(33, Color.GREEN);
                }
                if (type == TypeImageRecognition.NAME_IMG_RIGHT) {
                    imgLeft.setImageBitmap(cropImage2(bitmap));
                    setProgressCustom(100, Color.GREEN);
                }
                if (type == TypeImageRecognition.NAME_IMG_LEFT) {
                    imgRight.setImageBitmap(cropImage2(bitmap));
                    setProgressCustom(66, Color.GREEN);
                }
                //
                ImageUtils.saveBitmapAsJpeg(cropImage2(bitmap), this, type.value);

                callApiCheckImg(type);
            } else {
                // The pixel copy operation failed
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
                    if (!checked && !idImgStraight.isEmpty())
                        checkImageAvatar(idImgStraight, idImgAvatarNFC);
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
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                if (!isRightPass && stepCurrent == 2) {
                                    captureViewOnScreen(surfaceView, TypeImageRecognition.NAME_IMG_RIGHT);
                                }
                            }, 150);

                        } else if (eulerY > 30) {
                            // Face is facing left
                            Log.d(TAG, "Face is facing left");
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                if (!isLeftPass && stepCurrent == 1) {
                                    captureViewOnScreen(surfaceView, TypeImageRecognition.NAME_IMG_LEFT);
                                }
                            }, 150);
                        } else {
                            // Face is facing straight ahead
                            Log.d(TAG, "Face is facing straight ahead");
                            if (isFaceInsideFrame(face)) {
                                final Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(() -> {
                                    if (!isStraightPass && stepCurrent == 0) {
                                        captureViewOnScreen(surfaceView, TypeImageRecognition.NAME_IMG_CENTER);
                                    }
                                }, 300);
                            }
                        }
                    }
                }
            }
        });
    }

    private boolean isFaceInsideFrame(Face face) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;

        float faceCenterX = face.getPosition().x + face.getWidth() * 0.5f; // Calculate the horizontal center of the face
        float faceCenterY = face.getPosition().y + face.getHeight() * 0.5f + (float) (9 * screenHeight / 20); // Calculate the vertical center of the face
        float cameraWidth = surfaceView.getWidth(); // Replace with the width of your camera preview
        float cameraHeight = surfaceView.getHeight(); // Replace with the height of your camera preview

        float centerThreshold = 0.2f; // Define a threshold to determine if the face is centered

        boolean isFaceCenteredWidth = Math.abs(faceCenterX - cameraWidth * 0.5f) < cameraWidth * centerThreshold;
        boolean isFaceCenteredHeight = Math.abs(faceCenterY - cameraHeight * 0.5f) < cameraHeight * centerThreshold;
        return isFaceCenteredWidth && isFaceCenteredHeight;
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
        Call<ResultUploadResponse> call = eKycService.uploadFile(bucketIdRequestBody, gesture, img_name, filePart);
        call.enqueue(new Callback<ResultUploadResponse>() {
            @Override
            public void onResponse(Call<ResultUploadResponse> call, Response<ResultUploadResponse> response) {
                isCalling = false;
                if (type == TypeImageRecognition.NAME_IMG_CENTER) {
                    setNextStep();
                    isStraightPass = true;
                    if (response.body() != null) {
                        idImgStraight = response.body().getImage_id();
                    }
                }
                if (type == TypeImageRecognition.NAME_IMG_LEFT) {
                    isLeftPass = true;
                    setNextStep();
                }
                if (type == TypeImageRecognition.NAME_IMG_RIGHT) {
                    isRightPass = true;
                    setNextStep();
                }
            }

            @Override
            public void onFailure(Call<ResultUploadResponse> call, Throwable t) {
                isCalling = false;
            }
        });
    }

    private void checkImageAvatar(String idImgStraight, String idImgAvatarNFC) {
        showLoading(true);
        checked = true;
        CompareAvatarRequest request = new CompareAvatarRequest();
        request.setBucket_id1("ekyc");
        request.setObj_id1(idImgStraight);
        request.setBucket_id2("ekyc");
        request.setObj_id2(idImgAvatarNFC);
        request.setModel_name("Adaface");

        Call<CompareResponse> call = eKycService2.checkAvatar(request);
        call.enqueue(new Callback<CompareResponse>() {
            @Override
            public void onResponse(Call<CompareResponse> call, Response<CompareResponse> response) {
                showLoading(false);
                CompareResponse compareResponse = response.body();
                if (compareResponse.getStatus().equals("status")) {
                    setResultActive(true);
                } else {
                    setResultActive(false);
                }
            }

            @Override
            public void onFailure(Call<CompareResponse> call, Throwable t) {
                showLoading(false);
                setResultActive(false);
            }
        });
    }

    private void setResultActive(boolean isActive) {
        vStatusActive.setVisibility(View.VISIBLE);
        tvStep.setVisibility(View.GONE);
        tvStatus.setText(isActive ? "Xác thực khuôn mặt thành công!" : "Xác thực khuôn mặt thất bại!");
        tvStatus.setTextColor(isActive ? Color.parseColor("#17C37B") : Color.parseColor("#FF4A5A"));
        icStatus.setImageResource(isActive ? R.drawable.ic_active : R.drawable.ic_active_fail);
    }

    private void setNextStep() {
        stepCurrent++;
        Log.d("stepCurrent", String.valueOf(stepCurrent));
        if (stepCurrent == 0) {
            tvStep.setText("Vui lòng đưa khuôn mặt vào khung hình");
        } else if (stepCurrent == 1) {
            tvStep.setText("Vui lòng quay mặt từ từ sang bên trái ");
        } else {
            tvStep.setText("Vui lòng quay mặt từ từ sang bên phải");
        }
    }

    private void showLoading(boolean isShow) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> imgLoadingCompare.setVisibility(isShow ? View.VISIBLE : View.GONE), 1);
    }
}
