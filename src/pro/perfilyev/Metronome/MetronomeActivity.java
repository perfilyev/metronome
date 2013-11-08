package pro.perfilyev.Metronome;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.InputStream;

public class MetronomeActivity extends Activity {
  TextView beat;
  TextView bps;
  private ScaleGestureDetector mScaleDetector;
  Integer delayMillis = 500; //1/2 sec = 120bpm; 1 sec = 60bpm

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    mScaleDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleListener());
    beat = (TextView) findViewById(R.id.beatText);
    bps = (TextView) findViewById(R.id.bpsText);

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
    final byte[] buffer = new byte[bufferSize];

    final InputStream inputStream = getResources().openRawResource(R.raw.click);
    audioTrack.play();

    final Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
      int current = 1;

      @Override
      public void run() {
        beat.setText(String.valueOf(current));
        int i = 0;
        try {
          while ((i = inputStream.read(buffer)) != -1)
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

        timerHandler.postDelayed(this, delayMillis);
      }
    };
    timerHandler.postDelayed(timerRunnable, 0);


  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    mScaleDetector.onTouchEvent(ev);
    return true;
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      delayMillis = new Float(delayMillis / detector.getScaleFactor()).intValue();
      bps.setText(String.valueOf(new Double(60 / (delayMillis.doubleValue() / 1000)).intValue()) + "bpm");
      return true;
    }
  }
}
