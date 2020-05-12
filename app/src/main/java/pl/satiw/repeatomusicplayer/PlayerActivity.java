package pl.satiw.repeatomusicplayer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.repeatomusicplayer.R;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import pl.satiw.repeatomusicplayer.constans.Values;
import pl.satiw.repeatomusicplayer.errors.CustomExceptionHandler;
import pl.satiw.repeatomusicplayer.persistence.LastPlayed;

public class PlayerActivity extends AppCompatActivity {
    private static final String PREF_REPLAY = "PREF_REPLAY";
    private static final String PREF_REPLAY_REPLAY_COUNT = "PREF_REPLAY-REPLAY_COUNT";
    private static final Integer DEFAULT_REPLAY_COUNT = 5;
    private static final String TAG = "PlayerActivity";

    static MediaPlayer myMediaPlayer;

    Button btn_next, btn_previous, btn_pause;
    TextView songTextLabel;
    SeekBar songSeekBar;

    TextView tvReplayCount;
    TextView tvRemainingCount;

    Button btnIncrementReplayCount;
    Button btnDecrementReplayCount;

    int position;
    int remainingCount;
    String sname;

    ArrayList<File> mySongs;
    Thread updateSeekBar;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        findUiViews();

        resetReplayCount();

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) {
            return;
        }

        registerUncaughtExceptionHandler();

        supportActionBar.setTitle("Now Playing");
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setDisplayShowHomeEnabled(true);

        updateSeekBar = createUpdateSeekBarThread();

        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        if (bundle == null) {
            throw new InvalidParameterException();
        }

        //noinspection unchecked
        mySongs = (ArrayList) bundle.getParcelableArrayList("songs");

        if (mySongs == null) {
            throw new InvalidParameterException();
        }

        String playMode = i.getStringExtra("mode");
        if (playMode != null && playMode.equals(Values.RESUME_LAST_SONG)) {
            position = getLastPlayedPosition();
        } else {
            position = bundle.getInt("pos",0);
        }

        playSongByPosition(position);

        songSeekBar.getProgressDrawable().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        songSeekBar.getThumb().setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.SRC_IN);

        songSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                myMediaPlayer.seekTo(seekBar.getProgress());
            }
        });

        btn_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayPauseButton(btn_pause);
            }
        });

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextSong();
            }
        });

        btn_previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevSong();

            }
        });

        btnIncrementReplayCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReplayCount(getReplayCount() + 1);
            }
        });

        btnDecrementReplayCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setReplayCount(getReplayCount() - 1);
            }
        });
    }

    private void registerUncaughtExceptionHandler() {
        Thread.UncaughtExceptionHandler defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        if (!(defaultUEH instanceof CustomExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(defaultUEH));
        }
    }

    private int getLastPlayedPosition() {
        String lastPlayed = LastPlayed.getLastPlayedName(this);
        if (lastPlayed.length() == 0 || lastPlayed.equals(Values.RESUME_LAST_SONG)) {
            return 0;
        }

        for(int i = 0; i < mySongs.size(); i++) {
            File file = mySongs.get(i);

            if (file.getName().equals(lastPlayed)) {
                return i;
            }
        }

        return 0;
    }

    private void findUiViews() {
        btn_next = findViewById(R.id.next);
        btn_previous = findViewById(R.id.previous);
        btn_pause = findViewById(R.id.pause);

        btnIncrementReplayCount = findViewById(R.id.btnIncrementReplayCount);
        btnDecrementReplayCount = findViewById(R.id.btnDecrementReplayCount);

        songTextLabel = findViewById(R.id.songLabel);
        songSeekBar = findViewById(R.id.seekBar);

        tvReplayCount = findViewById(R.id.tvReplayCount);
        tvRemainingCount = findViewById(R.id.etReplayRemaining);
    }

    private void handlePlayPauseButton(Button btn) {
        songSeekBar.setMax(myMediaPlayer.getDuration());

        if(myMediaPlayer.isPlaying()) {
            btn.setBackgroundResource(R.drawable.icon_play);
            myMediaPlayer.pause();
        } else {
            btn.setBackgroundResource(R.drawable.icon_pause);
            myMediaPlayer.start();
        }
    }

    private Thread createUpdateSeekBarThread() {
        return new Thread() {
            @Override
            public void run() {
                try {
                    while (myMediaPlayer == null) {
                        sleep(100);
                    }
                } catch (InterruptedException ignored) {}

                int totalDuration = myMediaPlayer.getDuration();
                int currentpostion = 0;

                while(currentpostion < totalDuration) {
                    try {
                        sleep(500);

                        if (myMediaPlayer != null) {
                            currentpostion = myMediaPlayer.getCurrentPosition();
                            songSeekBar.setProgress(currentpostion);
                        }
                    }
                    catch (IllegalStateException ignored) {}
                    catch (InterruptedException ignored) {}
                }
            }
        };
    }

    private void prevSong() {
        position = ((position - 1) < 0) ? (mySongs.size() - 1) : (position - 1);
        playSongByPosition(position);
    }

    private void nextSong() {
       position = ((position + 1) % mySongs.size());
       playSongByPosition(position);
    }

    private void playSongByPosition(int position) {
        if (position < 0) {
            position = mySongs.size()-1;
        } else if (position > mySongs.size()) {
            position = 0;
        }

        if (updateSeekBar != null) {
            updateSeekBar.interrupt();
        }

        if (myMediaPlayer != null) {
            if (myMediaPlayer.isPlaying()) {
                myMediaPlayer.stop();
            }

            myMediaPlayer.release();
            myMediaPlayer = null;
        }

        File actualSong = mySongs.get(position);
        Uri u = Uri.parse(actualSong.toString());

        myMediaPlayer = MediaPlayer.create(getApplicationContext(), u);
        setMediaPlayerListeners();

        sname = actualSong.getName();
        songTextLabel.setText(sname);
        songTextLabel.setSelected(true);

        LastPlayed.saveLastPlayed(sname, this);

        resetRemaining();

        myMediaPlayer.start();
        songSeekBar.setMax(myMediaPlayer.getDuration());

        updateSeekBar = createUpdateSeekBarThread();
        updateSeekBar.start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private int ParseIntegerInput(EditText et) {
        Editable value = et.getText();
        try {
            int parsed = Integer.parseInt(value.toString());
            return (parsed < 1 || parsed > 100) ? DEFAULT_REPLAY_COUNT : parsed;
        } catch (NumberFormatException e) {
            return DEFAULT_REPLAY_COUNT;
        }
    }

    @SuppressLint("SetTextI18n")
    private void setRemaining(int count) {
        if (count < 0) {
            count = 0;
        } else if (count > this.getReplayCount()) {
            count = this.getReplayCount();
        }

        remainingCount = count;
        tvRemainingCount.setText(String.valueOf(remainingCount));
    }

    private void resetRemaining() {
        int count = getReplayCount()-1;
        setRemaining(count);
    }

    private void setReplayCount(int count) {
        if (count < 1) {
            count = 1;
        } else if (count > 100) {
            count = 100;
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREF_REPLAY, MODE_PRIVATE).edit();
        editor.putInt(PREF_REPLAY_REPLAY_COUNT, count);
        editor.apply();

        resetReplayCount();
    }

    private void resetReplayCount() {
        int replayCount = getReplayCount();
        tvReplayCount.setText(String.valueOf(replayCount));

        resetRemaining();
    }

    private int getReplayCount() {
        SharedPreferences preferences = getSharedPreferences(PREF_REPLAY, MODE_PRIVATE);

        @SuppressWarnings("UnnecessaryLocalVariable")
        int count = preferences.getInt(PREF_REPLAY_REPLAY_COUNT, DEFAULT_REPLAY_COUNT);
        return count;
    }

    private void setMediaPlayerListeners() {
        myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (remainingCount-- > 0) {
                    setRemaining(remainingCount);
                    mp.start();
                } else {
                    remainingCount = getReplayCount();
                    nextSong();
                }
            }
        });
    }
}
