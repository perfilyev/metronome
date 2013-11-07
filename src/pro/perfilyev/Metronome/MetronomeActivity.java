package pro.perfilyev.Metronome;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class MetronomeActivity extends Activity {
  TextView beat;
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    beat = (TextView) findViewById(R.id.beatText);

    final Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
      int current = 1;
      @Override
      public void run() {
        beat.setText(String.valueOf(current));
        if (current == 4) {
          current = 1;
        } else current++;

        timerHandler.postDelayed(this, 500);
      }
    };

    timerHandler.postDelayed(timerRunnable, 0);

  }



}
