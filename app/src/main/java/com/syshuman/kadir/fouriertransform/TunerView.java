package com.syshuman.kadir.fouriertransform;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public abstract class TunerView extends View {
    protected MainActivity.Audio audio;
    protected Resources resources;
    protected int width;
    protected int height;
    protected Paint paint;
    protected Rect clipRect;
    private RectF outlineRect;

    protected TunerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        resources = getResources();
    }

    /* On Size Changed */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width = w;
        height = h;
        outlineRect = new RectF( 1,  1, width-1, height-1);
        clipRect    = new Rect( 10, 10, width-10, height-10);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        paint.setStrokeWidth(3);
        paint.setAntiAlias(true);
        paint.setColor(Color.DKGRAY);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRoundRect(outlineRect, 10, 10, paint);
        canvas.clipRect(clipRect);
        canvas.translate(clipRect.left,clipRect.top); // Translate to the clip rect
    }


}
