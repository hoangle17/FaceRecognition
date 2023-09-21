package com.example.facerecognition.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CompareResponse {
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("result")
    @Expose
    private String result;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "CompareResponse{" +
                "status='" + status + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
