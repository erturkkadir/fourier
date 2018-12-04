package com.ekoja.transform;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class MainActivity extends AppCompatActivity {

    private Audio audio;
    private Spectrum spectrum;
    private Scope scope;
    private boolean isActive = false;
    private static int SFREQ   = 44100;
    private static int SGN_LEN = 2048;

    private ImageButton btnStart, btnInfo;
    private RadioGroup rbDraw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    15);

        }

        spectrum = (Spectrum) findViewById(R.id.spectrum);
        scope    = (Scope) findViewById(R.id.scope);

        rbDraw = (RadioGroup) findViewById(R.id.radioGroup1);

        audio = new Audio();

        if(spectrum != null) spectrum.audio = audio;
        if(scope    != null) scope.audio    = audio;

        btnStart = (ImageButton) this.findViewById(R.id.btnStart);
        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(isActive) {
                    audio.stop();
                    //btnStart.setText("Start Listening");
                    isActive = false;
                } else {
                    audio.start();
                    //btnStart.setText("Stop Listening");
                    isActive = true;
                }
            }
        });


        btnInfo = (ImageButton) this.findViewById(R.id.btnInfo);
        btnInfo.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);

            }
        });



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    void showAlert(int appName, int errorBuffer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(appName);
        builder.setMessage(errorBuffer);
        builder.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected class Audio implements Runnable {
        protected boolean lock;
        protected double fps;
        protected Thread thread;
        protected short data[];
        protected short freq[];
        protected short filt[];
        private AudioRecord audioRecord;
        private int minBufSize;

        private Complex x;


        protected Audio() {
            data   = new short[SGN_LEN];
            x      = new Complex(SGN_LEN);
            freq   = new short[SGN_LEN];

        }

        protected void start() {
            thread = new Thread(this, "Audio");
            thread.start();
        }

        @Override
        public void run() {
            processAudio();
        }

        public void stop() {
            Thread t = thread;
            thread = null;
            while (t != null && t.isAlive()) Thread.yield();
        }

        protected void processAudio() {
            minBufSize = AudioRecord.getMinBufferSize(SFREQ, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minBufSize < 0) return;  // 8192

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SFREQ, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, 2*minBufSize);

            int state = audioRecord.getState();
            if (state != AudioRecord.STATE_INITIALIZED) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showAlert(R.string.app_name, R.string.error_init);
                    }
                });
                audioRecord.release();
                thread = null;
                return;
            }

            audioRecord.startRecording();
            while(thread != null) {
                int size = audioRecord.read(data, 0, SGN_LEN);
                if (size==0) {
                    thread = null;
                    break;
                }


                if(scope!=null && !lock) scope.postInvalidate();

                for(int i=0; i<SGN_LEN; i++) x.r[i] =  data[i];

                fftr(x); // take fft

                String rb = ((RadioButton) findViewById(rbDraw.getCheckedRadioButtonId() )).getText().toString();

                if(rb.equals("Ampl."))
                    for(int i=0; i<SGN_LEN; i++) freq[i] = (short) Math.sqrt(x.r[i]*x.r[i] + x.i[i]*x.i[i]);

                if(rb.equals("Phase")) {
                    for(int i=0; i<SGN_LEN; i++)
                        if (x.r[i] != 0)
                            freq[i] = (short) Math.atan2(x.i[i], x.r[i]);
                        else
                            freq[i] = 0;
                    unwrap(freq, freq.length);
                }

                if(rb.equals("Real"))
                    for(int i=0; i<SGN_LEN; i++) freq[i] = (short) x.r[i];

                if(rb.equals("Imag"))
                    for(int i=0; i<SGN_LEN; i++) freq[i] = (short) x.i[i];


                if (spectrum != null)  spectrum.postInvalidate();
            }

            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
            }
        }

        public void unwrap(short[] freq, int N) {

            float dp[] = new float[N];
            float dps[] = new float[N];
            float dp_corr[] = new float[N];
            float cumsum[] = new float[N];


            int j;
            double M_PI =Math.PI;

            for (j = 0; j < N-1; j++)
                dp[j] =  (freq[j+1] - freq[j]);

            for (j = 0; j < N-1; j++)
                dps[j] = (float) ((dp[j]+M_PI) - Math.floor((dp[j]+M_PI) / (2*M_PI))*(2*M_PI) - M_PI);

            for (j = 0; j < N-1; j++)
                if ((dps[j] == -M_PI) && (dp[j] > 0))
                    dps[j] = (float) M_PI;

            for (j = 0; j < N-1; j++)
                dp_corr[j] = dps[j] - dp[j];

            for (j = 0; j < N-1; j++)
                if (Math.abs(dp[j]) < M_PI)
                    dp_corr[j] = 0;

            cumsum[0] = dp_corr[0];
            for (j = 1; j < N-1; j++)
                cumsum[j] = cumsum[j-1] + dp_corr[j];

            for (j = 1; j < N; j++)
                freq[j] += cumsum[j-1];
        }

        private class Complex {  // Holds Complex Array l size
            double r[];
            double i[];
            private Complex(int l) {
                r = new double[l];
                i = new double[l];
            }
        }

        private void fftr(Complex a) {
            final int n = a.r.length;
            final double norm = Math.sqrt(1.0 / n);
            for (int i = 0, j = 0; i < n; i++) {
                if (j >= i) {
                    double tr = a.r[j] * norm;
                    a.r[j] = a.r[i] * norm;
                    a.i[j] = 0.0;
                    a.r[i] = tr;
                    a.i[i] = 0.0;
                }

                int m = n / 2;
                while (m >= 1 && j >= m) {
                    j -= m;
                    m /= 2;
                }
                j += m;
            }

            for (int mmax = 1, istep = 2 * mmax; mmax < n; mmax = istep, istep = 2 * mmax) {
                double delta = (Math.PI / mmax);
                for (int m = 0; m < mmax; m++) {
                    double w = m * delta;
                    double wr = Math.cos(w);
                    double wi = Math.sin(w);
                    for (int i = m; i < n; i += istep) {
                        int j = i + mmax;
                        double tr = wr * a.r[j] - wi * a.i[j];
                        double ti = wr * a.i[j] + wi * a.r[j];
                        a.r[j] = a.r[i] - tr;
                        a.i[j] = a.i[i] - ti;
                        a.r[i] += tr;
                        a.i[i] += ti;
                    }
                }
            }
        }
    }
}
