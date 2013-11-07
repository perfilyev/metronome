package pro.perfilyev.Metronome;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

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
