package com.example.facerecognition.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class CircleProgressBar extends View {
    private int progress;
    private Paint paint;
    private int color;

    public CircleProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4); // adjust the stroke width as needed
        paint.setColor(Color.RED); // adjust the color as needed
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(centerX, centerY);
        RectF rectF = new RectF(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);
        paint.setColor(color);
        canvas.drawArc(rectF, -90, (float)progress / 100 * 360, false, paint);
    }

    public void setProgress(int progress) {
        if (progress < 0) {
            this.progress = 0;
        } else if (progress > 100) {
            this.progress = 100;
        } else {
            this.progress = progress;
        }
        invalidate();
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }
}
