package com.ekoja.transform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;

public abstract class Graticule extends TunerView {
    private static final int SIZE = 40;
    private Canvas source;
    private Bitmap bitmap;
    private Bitmap rounded;
    private Paint xferPaint;

    protected  Graticule(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        width = (clipRect.right - clipRect.left);
        height = (clipRect.bottom - clipRect.top);


        rounded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(rounded);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        canvas.drawRoundRect(new RectF(0, 0, width, height), 10, 10, paint);


        xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xferPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        source = new Canvas(bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        source.setMatrix(null);
        drawGraticule(source);
        drawTrace(source);
        source.setMatrix(null);
        //source.drawBitmap(rounded, 0, 0, xferPaint);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    private void drawGraticule(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        paint.setAntiAlias(false);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(1);
        for(int i = (width % SIZE)/2; i<= width; i+= SIZE )
            canvas.drawLine(i, 0, i, height, paint);
        for(int i = (height% SIZE)/2; i<=height; i+= SIZE )
            canvas.drawLine(0, i, width, i, paint);
    }

    protected abstract void drawTrace(Canvas canvas);


}

