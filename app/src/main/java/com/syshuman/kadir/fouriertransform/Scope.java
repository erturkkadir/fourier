package com.syshuman.kadir.fouriertransform;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.util.AttributeSet;

public class Scope extends Graticule {
    private Path path;
    private int len;
    private float max;

    public Scope(Context context, AttributeSet attrs) {
        super(context, attrs);
        path = new Path();
    }

    @Override
    protected void drawTrace(Canvas canvas) {
        if (audio == null || audio.data == null) return;
        len = audio.data.length;

        float dx = (float) width / (float) len;

        max = (float) audio.data[0];
        for(int i=1; i<len; i++)
            if (audio.data[i]>max) max = (int) audio.data[i];

        canvas.translate(0, height/2);

        float yScale = max / (height/4);

        path.rewind();
        path.moveTo(0, 0);



        for (int i = 0; i < len; i++) {
            float y = -audio.data[i] / yScale;
            float x = i * dx;
            path.lineTo(x, y);
        }

        paint.setColor(Color.WHITE);
        canvas.drawLine(0, 0, width, 0, paint);

        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        canvas.drawPath(path, paint);
    }
}
