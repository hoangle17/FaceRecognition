package com.example.facerecognition.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ResultUploadResponse {
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("result")
    @Expose
    private boolean result;
    @SerializedName("image_id")
    @Expose
    private String image_id;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getImage_id() {
        return image_id;
    }

    public void setImage_id(String image_id) {
        this.image_id = image_id;
    }
}