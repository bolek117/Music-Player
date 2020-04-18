package com.example.advmusicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import java.util.ArrayList;

public class PlayerActivity extends AppCompatActivity {
    Button btn_next, btn_previous, btn_pause;
    TextView songTextLabel;
    SeekBar songSeekBar;

    static MediaPlayer myMediaPlayer;
    int position;
    String sname;

    ArrayList<File> mySongs;
    Thread updateSeekBar;

    final Integer defaultReplayCount = 5;

    TextView etReplayCount;
    Integer replayCount = defaultReplayCount;

    TextView etRemainingCount;
    Integer remainingCount = defaultReplayCount;

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

        etReplayCount = findViewById(R.id.etReplayCount);
        etReplayCount.setText(replayCount.toString());

        etRemainingCount = findViewById(R.id.etReplayRemaining);
        etRemainingCount.setText(remainingCount.toString());


        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) {
            return;
        }

        supportActionBar.setTitle("Now Playing");
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setDisplayShowHomeEnabled(true);

        updateSeekBar = createUpdateSeekBar();

        if(myMediaPlayer != null)
        {
            myMediaPlayer.stop();
            myMediaPlayer.release();
        }

        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        mySongs = (ArrayList) bundle.getParcelableArrayList("songs");

        sname = mySongs.get(position).getName();

        String songName = i.getStringExtra("songname");

        songTextLabel.setText(songName);
        songTextLabel.setSelected(true);

        position = bundle.getInt("pos",0);

        Uri u = Uri.parse(mySongs.get(position).toString());

        myMediaPlayer = MediaPlayer.create(getApplicationContext(),u);
        myMediaPlayer.start();
        songSeekBar.setMax(myMediaPlayer.getDuration());

        updateSeekBar.start();

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
                songSeekBar.setMax(myMediaPlayer.getDuration());

                if(myMediaPlayer.isPlaying())
                {
                    btn_pause.setBackgroundResource(R.drawable.icon_play);
                    myMediaPlayer.pause();
                }
                else
                {
                    btn_pause.setBackgroundResource(R.drawable.icon_pause);
                    myMediaPlayer.start();
                }
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

        etReplayCount.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) return;

                EditText input = (EditText)v;
                Integer parsedValue = ParseIntegerInput(input);

                if (!parsedValue.equals(replayCount)) {
                    etReplayCount.setText(parsedValue.toString());
                    etRemainingCount.setText(parsedValue.toString());

                    replayCount = parsedValue;
                    updateRemainingCount(parsedValue);
                }
            }
        });
    }

    private Thread createUpdateSeekBar()
    {
        return new Thread()
        {
            @Override
            public void run() {
                int totalDuration = myMediaPlayer.getDuration();
                int currentpostion = 0;

                while(currentpostion < totalDuration)
                {
                    try {
                        sleep(500);
                        currentpostion = myMediaPlayer.getCurrentPosition();
                        songSeekBar.setProgress(currentpostion);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    private void prevSong()
    {
        stopSeekBar();

        myMediaPlayer.stop();
        myMediaPlayer.release();
        position = ((position - 1) < 0) ? (mySongs.size() - 1) : (position - 1);

        Uri u = Uri.parse(mySongs.get(position).toString());
        myMediaPlayer = MediaPlayer.create(getApplicationContext(), u);

        sname = mySongs.get(position).getName();
        songTextLabel.setText(sname);

        myMediaPlayer.start();
        setListeners();

        updateSeekBar = createUpdateSeekBar();
        updateSeekBar.start();
    }

    private void nextSong()
    {
        stopSeekBar();
        // TODO: update remaining count to default value

        myMediaPlayer.stop();
        myMediaPlayer.release();
        position = ((position + 1) % mySongs.size());

        Uri u = Uri.parse(mySongs.get(position).toString());

        myMediaPlayer = MediaPlayer.create(getApplicationContext(), u);

        sname = mySongs.get(position).getName();
        songTextLabel.setText(sname);

        myMediaPlayer.start();
        setListeners();

        updateSeekBar = createUpdateSeekBar();
        updateSeekBar.start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        if(item.getItemId() == android.R.id.home)
        {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private int ParseIntegerInput(EditText et) {
        Editable value = et.getText();
        try {
            int parsed = Integer.parseInt(value.toString());
            return (parsed < 1 || parsed > 100) ? defaultReplayCount : parsed;
        } catch (NumberFormatException e) {
            return defaultReplayCount;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateRemainingCount(int count) {
        remainingCount = count;
        etRemainingCount.setText(remainingCount.toString());
    }

    private void setListeners() {
        myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (remainingCount-- > 0) {
                    updateRemainingCount(remainingCount);
                    mp.seekTo(1);
                    mp.start();
                } else {
                    remainingCount = replayCount;
                    updateRemainingCount(remainingCount);
                    nextSong();
                }
            }
        });
    }

    private void stopSeekBar() {
        updateSeekBar.interrupt();
        while(!updateSeekBar.isInterrupted()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
