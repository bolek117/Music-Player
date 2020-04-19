package com.example.advmusicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

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

import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    private static final String PREF_REPLAY = "PREF_REPLAY";
    private static final String PREF_REPLAY_REPLAY_COUNT = "PREF_REPLAY-REPLAY_COUNT";
    private static final Integer DEFAULT_REPLAY_COUNT = 5;

    static MediaPlayer myMediaPlayer;

    Button btn_next, btn_previous, btn_pause;
    TextView songTextLabel;
    SeekBar songSeekBar;

    // TODO: Hand;e TV
    TextView tvReplayCount;
    TextView tvRemainingCount;

    // TODO: Handle buttons
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

        btn_next = findViewById(R.id.next);
        btn_previous = findViewById(R.id.previous);
        btn_pause = findViewById(R.id.pause);

        songTextLabel = findViewById(R.id.songLabel);
        songSeekBar = findViewById(R.id.seekBar);

        int replayCount = getReplayCount();
        tvReplayCount.setText(String.valueOf(replayCount));
        setRemaining();

        tvRemainingCount = findViewById(R.id.etReplayRemaining);
        tvRemainingCount.setText(String.valueOf(remainingCount));

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) {
            return;
        }

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

        String songName = i.getStringExtra("songname");
        songTextLabel.setText(songName);
        songTextLabel.setSelected(true);

        position = bundle.getInt("pos",0);
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

        tvReplayCount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    return;
                }

                EditText input = (EditText)v;
                Integer parsedValue = ParseIntegerInput(input);

                if (!parsedValue.equals(getReplayCount())) {
                    setReplayCount(parsedValue);
                    tvRemainingCount.setText(parsedValue.toString());

                    setRemaining(parsedValue);
                }
            }
        });
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
        // TODO: Fix progress bar initialization
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
            position = mySongs.size();
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

        sname = actualSong.getName();
        songTextLabel.setText(sname);

        setRemaining();

        setMediaPlayerListeners();
        myMediaPlayer.start();

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
        remainingCount = count;
        tvRemainingCount.setText(String.valueOf(remainingCount));
    }

    private void setRemaining() {
        int count = getReplayCount()-1;
        setRemaining(count);
    }

    private void setReplayCount(int count) {
        if (count < 1 || count > 100) {
            throw new InvalidParameterException();
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREF_REPLAY, MODE_PRIVATE).edit();
        editor.putInt(PREF_REPLAY_REPLAY_COUNT, count);
        editor.apply();

        tvReplayCount.setText(String.valueOf(count));
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
