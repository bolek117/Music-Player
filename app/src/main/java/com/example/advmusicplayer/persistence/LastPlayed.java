package com.example.advmusicplayer.persistence;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

public class LastPlayed {
    private static final String PREF_BUCKET = "PREF_LAST_PLAYED";
    private static final String KEY_LAST_PLAYED = "LAST_PLAYED_NAME";

    public static void saveLastPlayed(@Nullable String songName, Activity context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_BUCKET, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_LAST_PLAYED, songName);
        editor.apply();
    }

    public static String getLastPlayedName(Activity context) {
        SharedPreferences pref = context.getSharedPreferences(PREF_BUCKET, Context.MODE_PRIVATE);
        return pref.getString(KEY_LAST_PLAYED, "");
    }
}
