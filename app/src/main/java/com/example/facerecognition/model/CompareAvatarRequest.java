package com.example.facerecognition.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompareAvatarRequest {
    @SerializedName("bucket_id1")
    @Expose
    private String bucket_id1;

    @SerializedName("obj_id1")
    @Expose
    private String obj_id1;

    @SerializedName("bucket_id2")
    @Expose
    private String bucket_id2;

    @SerializedName("obj_id2")
    @Expose
    private String obj_id2;

    @SerializedName("model_name")
    @Expose
    private String model_name;

    public String getBucket_id1() {
        return bucket_id1;
    }

    public void setBucket_id1(String bucket_id1) {
        this.bucket_id1 = bucket_id1;
    }

    public String getObj_id1() {
        return obj_id1;
    }

    public void setObj_id1(String obj_id1) {
        this.obj_id1 = obj_id1;
    }

    public String getBucket_id2() {
        return bucket_id2;
    }

    public void setBucket_id2(String bucket_id2) {
        this.bucket_id2 = bucket_id2;
    }

    public String getObj_id2() {
        return obj_id2;
    }

    public void setObj_id2(String obj_id2) {
        this.obj_id2 = obj_id2;
    }

    public String getModel_name() {
        return model_name;
    }

    public void setModel_name(String model_name) {
        this.model_name = model_name;
    }
}
