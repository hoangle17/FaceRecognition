package com.example.facerecognition.service;

import com.example.facerecognition.model.CompareAvatarRequest;
import com.example.facerecognition.model.ResultUploadResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface EKycService {
    @Multipart
    @POST("api/v1/face/upload-image")
    Call<ResultUploadResponse> uploadFile(
        @Part("bucket_id") RequestBody bucketId,
        @Part("gesture") RequestBody gesture,
        @Part("img_name") RequestBody img_name,
        @Part MultipartBody.Part file
    );

    @POST("/api/v1/face/compare_v6")
    Call<Object> checkAvatar(
            @Body CompareAvatarRequest request
            );
}