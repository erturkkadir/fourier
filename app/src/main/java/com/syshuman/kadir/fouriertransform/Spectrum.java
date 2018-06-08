package com.syshuman.kadir.fouriertransform;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.util.AttributeSet;

public class Spectrum extends Graticule{
    private Path path;
    private float max;
    private int len;

    public Spectrum(Context context, AttributeSet attrs) {
        super(context, attrs);
        path = new Path();
    }

    @Override
    protected void drawTrace(Canvas canvas) {
        float x, y;
        String lbl = "";

        if (audio == null || audio.freq == null) return;
        len = audio.freq.length;
        float dx = (float) width / (float) len * 2; // half of the band

        max = audio.freq[0];
        for(int i=1; i<len; i++)
            if (audio.freq[i] > max) max = audio.freq[i];

        canvas.translate(0, height / 2.0f);

        float yScale = max / (height/2);

        path.rewind();
        path.moveTo(0, 0);

        for (int i = 0; i < len/2; i++) {
            y = (float) (-audio.freq[i] / yScale);
            x = i * dx;
            path.lineTo(x, y );
        }

        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        paint.setColor(Color.GREEN);
        canvas.drawPath(path, paint);



        paint.setTextSize(25);
        paint.setColor(Color.WHITE);


        paint.setTextSize(20);
        for (int i=0; i<len/2+1; i+=len/16) {
            x = dx * i;
            paint.setColor(Color.YELLOW);
            canvas.drawLine(x, 0, x, 10, paint);

            canvas.save();
            canvas.rotate(-90, x, 75);
            lbl = Integer.toString(i*44100/len);
            canvas.drawText(lbl, x, 75, paint);
            canvas.restore();
        }

        paint.setColor(Color.WHITE);
        canvas.drawLine(0, 0, width, 0, paint);

        canvas.drawText("Max Y axis      : " + Float.toString(max)  , (float) width - 250, -180, paint);
        canvas.drawText("Window size     : " + Integer.toString(len) + " Points", (float) width - 250, -140, paint);
        canvas.drawText("Max Frequency   : " + lbl + " Hz"          , (float) width - 250, -100 , paint);
        canvas.drawText("Frequency Res.  : " + String.format("%.4f" , (float) (22050.0/len)), (float) width - 250, -60, paint);
    }
}
