package com.darktornado.youtubedownloader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.darktornado.library.SimpleRequester;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "License").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        startActivity(new Intent(this, LicenseActivity.class));
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getActionBar().setDisplayShowHomeEnabled(false);
            getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FF4081")));
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(1);
            TextView txt1 = new TextView(this);
            txt1.setText("URL : ");
            txt1.setTextSize(17);
            layout.addView(txt1);
            final EditText txt2 = new EditText(this);
            txt2.setHint("Input Vidoe's url...");
            layout.addView(txt2);
            Button video = new Button(this);
            video.setText("Download Video");
            video.setTransformationMethod(null);
            video.setOnClickListener(view -> {
                String input = txt2.getText().toString();
                if (input.equals("")) {
                    toast("Please input video's url.");
                } else {
                    final AlertDialog dialog = showProgress();
                    download(parseVideoId(input), dialog);
                }
            });
            layout.addView(video);
            Button image = new Button(this);
            image.setText("Download Thumbnail");
            image.setTransformationMethod(null);
            image.setOnClickListener(view -> {
                String input = txt2.getText().toString();
                if (input.equals("")) {
                    toast("Please input video's url.");
                } else {
                    final String videoId = parseVideoId(input);
                    new Thread(() -> {
                        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/";
                        String name = videoId + "_thumbnail.jpg";
                        boolean downloaded = copyFromWeb("https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg", path + name);
                        if (downloaded)
                            toast("Video's thumbnail is downloaded.\nName: " + name + ".mp4\nPath: " + path);
                    }).start();
                }
            });
            layout.addView(image);

            TextView txt = new TextView(this);
            txt.setText("\nThis App is NOT related with Google and YouTube.\nDeveloper and This App are NOT responsible for any problems by using this app.");
            txt.setTextSize(18);
            txt.setTextColor(Color.BLACK);
            layout.addView(txt);

            TextView maker = new TextView(this);
            maker.setText("\n© 2020-2021 Dark Tornado, All rights reserved.\n");
            maker.setTextSize(13);
            maker.setTextColor(Color.BLACK);
            maker.setGravity(Gravity.CENTER);
            layout.addView(maker);

            int pad = dip2px(16);
            layout.setPadding(pad, pad, pad, pad);
            setContentView(layout);
            permissionCheck();
        } catch (Exception e) {
            toast(e.toString());
        }
    }

    private String parseVideoId(String url) {
        if (url.startsWith("https://www.youtube.com/watch?v=")) {
            url = url.split("&")[0];
            return url.substring(url.lastIndexOf("=") + 1);
        }
        return url.substring(url.lastIndexOf("/") + 1);
    }

    private void download(final String videoId, final AlertDialog dialog) {
        new Thread(() -> {
            try {
//                final String dataa = SimpleRequester.create("https://youtube.com/get_video_info?html5=1&video_id=" + videoId).execute().body;
                String url = "https://www.youtube.com/get_video_info?html5=1&video_id=" + videoId +
                        "&eurl=" + URLEncoder.encode("https://youtube.googleapis.com/v/" + videoId, "UTF-8") +
                        "&c=TVHTML5&cver=6.20180913";
                String dataa = SimpleRequester.create(url).execute().body;
                String[] data0 = dataa.split("&");
                String data2 = null;
                for (final String s : data0) {
                    if (s.startsWith("player_response=")) {
                        data2 = URLDecoder.decode(s.substring(s.indexOf("=") + 1), "UTF-8");
                        break;
                    }
                }
                if (data2 == null) {
                    runOnUiThread(() -> {
                        dialog.dismiss();
                        toast("Cannot load video's info.");
                    });
                    return;
                }
                JSONObject data = new JSONObject(data2);
                final String title = data.getJSONObject("videoDetails").getString("title").replace("/", "");
                JSONArray data3 = data.getJSONObject("streamingData").getJSONArray("formats");
                int size = data3.length();
                final String[] sizes = new String[size], urls = new String[size];
                for (int n = 0; n < data3.length(); n++) {
                    JSONObject json = data3.getJSONObject(n);
                    sizes[n] = json.getString("width") + " × " + json.getString("height");
                    urls[n] = json.getString("url");
                }
                runOnUiThread(() -> selectVideoSize(title, sizes, urls, dialog));
            } catch (Exception e) {
                toast("Failed to parse video's data.\n" + e.toString());
            }
        }).start();
    }

    private void selectVideoSize(final String title, final String[] sizes, final String[] urls, final AlertDialog dialog0) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Select Video's Resolution");
        dialog.setItems(sizes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog1, final int which) {
                download(title + " (" + sizes[which] + ")", urls[which], dialog0);
                toast(sizes[which] + " is selected.");
            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog0.dismiss();
                toast("Download is canceled.");
            }
        });
        dialog.show();
    }

    private void download(final String title, final String url, final AlertDialog dialog) {
        new Thread(() -> {
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/";
            if (copyFromWeb(url, path + title + ".mp4")) {
                toast("Video is downloaded.\nName: " + title + ".mp4\nPath: " + path);
            } else {
                toast("Failed to downloaded video.");
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.dismiss();
                }
            });
        }).start();
    }

    private boolean copyFromWeb(String url, String path) {
        try {
            URLConnection con = new URL(url).openConnection();
            if (con != null) {
                con.setConnectTimeout(5000);
                con.setUseCaches(false);
                BufferedInputStream bis = new BufferedInputStream(con.getInputStream(), 1024);
                File file = new File(path);
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);
                int buf;
                while ((buf = bis.read()) != -1) {
                    bos.write(buf);
                }
                bis.close();
                bos.close();
                fos.close();
            }
            return true;
        } catch (Exception e) {
            toast("Download Failed.");
//            toast("Download Failed.\n" + e.toString());
            return false;
        }
    }

    private AlertDialog showProgress() {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
        ProgressBar bar = new ProgressBar(this);
        layout.addView(bar);
        TextView txt = new TextView(this);
        txt.setText("Downloading...");
        txt.setTextSize(14);
        txt.setGravity(Gravity.CENTER);
        layout.addView(txt);
        int pad = dip2px(5);
        layout.setPadding(pad, pad, pad, pad);
        dialog.setView(layout);
        dialog.show();
        dialog.getWindow().setLayout(dip2px(170), -2);
        return dialog;
    }


    private void permissionCheck() {
        if (Build.VERSION.SDK_INT < 23) return;
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            toast("Please allow permissions.");
        }
    }

    private void toast(final String msg) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show());
    }

    public int dip2px(int dips) {
        return (int) Math.ceil(dips * this.getResources().getDisplayMetrics().density);
    }

}
