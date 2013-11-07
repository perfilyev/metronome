package pro.perfilyev.Metronome;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.InputStream;

public class MetronomeActivity extends Activity {
    TextView beat;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        beat = (TextView) findViewById(R.id.beatText);

        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        winParams.flags |= bits;
        win.setAttributes(winParams);
        getActionBar().hide();
        int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

        int bufferSize = 512;
        final byte [] buffer = new byte[bufferSize];

        final InputStream inputStream =getResources().openRawResource(R.raw.click);
        audioTrack.play();

        final Handler timerHandler = new Handler();
        Runnable timerRunnable = new Runnable() {
            int current = 1;


            @Override
            public void run() {
                beat.setText(String.valueOf(current));
                int i = 0;
                try {
                    while((i = inputStream.read(buffer)) != -1)
                        audioTrack.write(buffer, 0, i);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    inputStream.reset();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (current == 4) {
                    current = 1;
                } else current++;

                timerHandler.postDelayed(this, 500);
            }
        };
        timerHandler.postDelayed(timerRunnable, 0);
    }

}
