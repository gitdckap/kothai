package com.example.customkeyboard.async;

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.giphy.sdk.ui.views.GiphyGridView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadGIF extends AsyncTask<String, String, Uri> {
    private final int i;
    private String gifURL = "";
    private final Context context;
    private final InputConnection inputConnection;
    private final WeakReference<LinearLayout> gif_click_progress_container_ref;
    private final WeakReference<GiphyGridView> giphyGridView_ref;

    public DownloadGIF(int i, String gifURL, Context context, InputConnection inputConnection, LinearLayout gif_click_progress_container, GiphyGridView giphyGridView) {
        this.i = i;
        this.gifURL = gifURL;
        this.context = context.getApplicationContext();
        this.inputConnection = inputConnection;
        this.gif_click_progress_container_ref = new WeakReference<>(gif_click_progress_container);
        this.giphyGridView_ref = new WeakReference<>(giphyGridView);
    }

    @Override
    protected void onPreExecute() {
        LinearLayout gif_click_progress_container = gif_click_progress_container_ref.get();
        GiphyGridView gridView = giphyGridView_ref.get();
        if (gif_click_progress_container != null && gridView != null) {
            gif_click_progress_container.setVisibility(View.VISIBLE);
            gridView.setVisibility(View.GONE);
        }
        File gifDir = new File(context.getFilesDir(), "gifs");
        if (!gifDir.exists()) {
            gifDir.mkdirs();
        }
    }

    @Override
    protected Uri doInBackground(String... strings) {
        Uri contentUri = null;
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            URL url = new URL(gifURL);
            File gifFile = getFile(url);
            contentUri = FileProvider.getUriForFile(context, "com.dckap.kothai.fileprovider", gifFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return contentUri;
    }

    private @NonNull File getFile(URL url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.connect();
        InputStream inputStream = httpURLConnection.getInputStream();
        File gifFile = new File(context.getFilesDir(), "gifs/example" + i + ".gif");
        try (FileOutputStream outputStream = new FileOutputStream(gifFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return gifFile;
    }

    @Override
    @SuppressLint("NewApi")
    protected void onPostExecute(Uri uri) {
        ClipDescription description = new ClipDescription("GIF", new String[]{"image/gif"});
        InputContentInfo inputContentInfo = new InputContentInfo(uri, description, null);
        inputConnection.commitContent(inputContentInfo, InputConnection.INPUT_CONTENT_GRANT_READ_URI_PERMISSION, null);
        LinearLayout gif_click_progress_container = gif_click_progress_container_ref.get();
        GiphyGridView gridView = giphyGridView_ref.get();
        if (gif_click_progress_container != null && gridView != null) {
            gif_click_progress_container.setVisibility(View.GONE);
            gridView.setVisibility(View.VISIBLE);
        }
    }
}
