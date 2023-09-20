package com.example.facerecognition.model;

public enum TypeImageRecognition {
    NAME_IMG_CENTER("img_center"), NAME_IMG_RIGHT("img_right"), NAME_IMG_LEFT("img_left");

    public final String value;

    private TypeImageRecognition(String value) {
        this.value = value;
    }
}
