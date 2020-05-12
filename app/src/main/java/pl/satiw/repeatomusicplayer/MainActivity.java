package pl.satiw.repeatomusicplayer;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.repeatomusicplayer.R;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import pl.satiw.repeatomusicplayer.constans.Values;

public class MainActivity extends AppCompatActivity {
    ListView myListViewForSongs;
    String[] items;

    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    boolean permissionsGranted = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myListViewForSongs = findViewById(R.id.mySongListView);
        runtimePermission();
    }

    public void runtimePermission() {
        Dexter.withActivity(this)
                .withPermissions(permissions)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            display();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    public ArrayList<File> findSong(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();

        if (files == null) return arrayList;

        for (File singleFile : files) {
            String absPath = singleFile.getAbsolutePath();

            if (singleFile.isDirectory() && !singleFile.isHidden()) {
                arrayList.addAll(findSong(singleFile));
            }

            if (absPath.contains("player") &&
                    (singleFile.getName().endsWith(".mp3") || singleFile.getName().endsWith(".wav"))
            ) {
                arrayList.add(singleFile);
            }
        }
        return arrayList;
    }

    void display() {
        final ArrayList<File> mySongs = findSong(Environment.getExternalStorageDirectory());
        addResumeElement(mySongs);

        items = new String[mySongs.size()];
        for (int i = 0; i < mySongs.size(); i++) {
            items[i] = mySongs.get(i).getName();
            //.toString().replace(".mp3","").replace(".wav","");
        }

        ArrayAdapter<String> myAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                items);

        myListViewForSongs.setAdapter(myAdapter);
        myListViewForSongs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String songName = myListViewForSongs.getItemAtPosition(i).toString();

                @SuppressWarnings("unchecked")
                ArrayList<File> songs = new ArrayList(mySongs.subList(1, mySongs.size()));

                if (i == 0) {
                    startActivity(new Intent(getApplicationContext(), PlayerActivity.class)
                            .putExtra("mode", Values.RESUME_LAST_SONG)
                            .putExtra("songs", songs)
                    );
                } else {
                    startActivity(new Intent(getApplicationContext(), PlayerActivity.class)
                            .putExtra("songs", songs)
                            .putExtra("songname", songName)
                            .putExtra("pos", i - 1)
                    );
                }
            }
        });
    }

    private void addResumeElement(ArrayList<File> songs) {
        if (songs.size() == 0) {
            return;
        }

        File resumePlaceholder = new File("Resume where I stopped...");
        songs.add(0, resumePlaceholder);
    }
}
