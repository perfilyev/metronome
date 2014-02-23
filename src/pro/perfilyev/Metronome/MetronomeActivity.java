package pro.perfilyev.Metronome;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class MetronomeActivity extends Activity {
  TextView beat;
  TextView bps;
  private ScaleGestureDetector mScaleDetector;
  Integer delayMillis = 500; //1/2 sec = 120bpm; 1 sec = 60bpm
  double bitRate = 44.1 * 16;
  private byte[] lowClick;
  private byte[] hiClick;
  private byte[] patternClick;
  private final Mode mode = Mode.PROD;

  private enum Mode {
    DEV, PROD
  }

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


    int minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT);
    final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);

    final InputStream lowSource = getResources().openRawResource(R.raw.logic_low);
    final InputStream hiSource = getResources().openRawResource(R.raw.logic_hi);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      lowSource.skip(44);
      int reads = lowSource.read();
      while (reads != -1) {
        baos.write(reads);
        reads = lowSource.read();
      }
      lowClick = baos.toByteArray();

      baos.reset();

      hiSource.skip(44);
      reads = hiSource.read();
      while (reads != -1) {
        baos.write(reads);
        reads = hiSource.read();
      }
      hiClick = baos.toByteArray();

      lowSource.close();
      hiSource.close();
      baos.close();
    } catch (IOException e) {

    }


    patternClick = makePattern();

    double dur = ((patternClick.length) * 8.0) / bitRate;

    audioTrack.play();


    final Handler widgetHandler = new Handler();
    final BeatUpdater widgetRunnable = new BeatUpdater() {
      int current = 1;
      long startTime = System.nanoTime();

      @Override
      public void run() {

        beat.setText(String.valueOf(current));

        if (current == 4) {
          current = 1;
        } else {
          current++;
        }

        widgetHandler.postDelayed(this, delayMillis);

        long processTime = (System.nanoTime() - startTime) / 1000000;
        long delay = processTime - delayMillis;

        if (mode.equals(Mode.DEV)) {
          System.out.println("Beat time " + processTime + " ms");
          System.out.println("Delay time " + delay + " ms");
        }

        startTime = System.nanoTime();
      }

      @Override
      public void sync() {
        current = 1;
      }
    };


    final BeatTimeUpdater beatTimeUpdater = new BeatTimeUpdater() {

      private long startTime = System.nanoTime();

      @Override
      public void run() {
        try {


          try {
            long processTime = (System.nanoTime() - startTime) / 1000000;
            long delay = processTime - delayMillis;
            if (mode.equals(Mode.DEV)) {

              System.out.println("Thread sleep time " + processTime + " ms");
              System.out.println("Delay thread time " + delay + " ms");
            }


            startTime = System.nanoTime();
            Thread.sleep(delay > 0 ? delayMillis - delay : delayMillis);
            beat.post(widgetRunnable);


            run();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
//       widgetHandler.postDelayed(this, time);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void sync() {
        startTime = System.nanoTime();
      }
    };
    final Thread beatThread = new Thread(beatTimeUpdater);

    Thread clickThread = new Thread(new Runnable() {
      long startTime = System.nanoTime();

      @Override
      public void run() {
        try {
          long endTime = System.nanoTime();
          if (mode.equals(Mode.DEV))
            System.out.println("Click pattern time " + (endTime - startTime) / 0.001 + " s");
          startTime = System.nanoTime();
          audioTrack.write(patternClick, 0, patternClick.length);
//          beatTimeUpdater.sync();
          widgetRunnable.sync();
          run();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    clickThread.start();
    widgetHandler.post(widgetRunnable);
//    beatThread.start();
//    widgetRunnable.run();


  }

  private abstract class BeatThread extends Thread {
    long startTime = System.nanoTime();

    protected BeatThread(Runnable runnable) {
      super(runnable);
    }

    public void sync() {
      setStartTime(System.nanoTime());
    }

    public long getStartTime() {
      return startTime;
    }

    public void setStartTime(long startTime) {
      this.startTime = startTime;
    }
  }

  private abstract class BeatUpdater implements Runnable {
    public abstract void sync();
  }

  private abstract class BeatTimeUpdater implements Runnable {
    public abstract void sync();
  }

  private byte[] makePattern() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      baos.write(normalizeClick(hiClick));
      byte[] normalizedLow = normalizeClick(lowClick);
      baos.write(normalizedLow);
      baos.write(normalizedLow);
      baos.write(normalizedLow);

      baos.write(baos.toByteArray());
    } catch (IOException e) {
    }
    return baos.toByteArray();
  }

  private double getPatternDuration() {
    return ((patternClick.length) * 8.0) / bitRate;
  }

  private byte[] normalizeClick(byte[] click) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      baos.write(click);
      int size = baos.size();

      int needed = (int) (delayMillis * bitRate / 8);

      for (int i = 0; i < needed - size; i++) {
        baos.write(0);
      }
    } catch (IOException e) {
    }
    return baos.toByteArray();
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
      bps.setText(String.valueOf(new Double(60 / (delayMillis / 1000.0)).intValue()) + "bpm");
      return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      patternClick = makePattern();
    }
  }
}
