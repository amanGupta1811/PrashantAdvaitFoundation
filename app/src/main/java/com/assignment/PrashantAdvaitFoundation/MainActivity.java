package com.assignment.PrashantAdvaitFoundation;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageAdapter adapter;

    ProgressBar progressBar;

    private LruCache<String, Bitmap> memoryCache;
    private File cacheDir;

    BroadcastReceiver networkReceiver;


    private final String apiKey = "5Mes_tu63S8vyj7pD-_0xrqgVPSDKMhzm_2gf-itYdw";
    private final String apiUrl = "https://api.unsplash.com/photos/random?count=30&client_id=" + apiKey;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progress);
        recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        adapter = new ImageAdapter();
        recyclerView.setAdapter(adapter);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<>(cacheSize);
        cacheDir = getCacheDir();

        networkReceiver = new NetworkReceiver();


//        fetchImages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkReceiver();
        checkInternetConnectivity();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkReceiver();
    }

    private void registerNetworkReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
    }

    private void unregisterNetworkReceiver() {
        unregisterReceiver(networkReceiver);
    }

    private void checkInternetConnectivity() {
        if (NetworkUtil.isNetworkAvailable(this)) {
            fetchImages();
        } else {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
        }
    }




    @SuppressLint("StaticFieldLeak")
    private void fetchImages() {
        new AsyncTask<Void, Void, List<Bitmap>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected List<Bitmap> doInBackground(Void... voids) {
                List<Bitmap> images = new ArrayList<>();
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();

                    InputStream inputStream = connection.getInputStream();
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = bufferedInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, length);
                    }
                    byte[] bytes = outputStream.toByteArray();

                    String response = new String(bytes);

                    JSONArray jsonArray = new JSONArray(response);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String imageUrl = jsonObject.getJSONObject("urls").getString("regular");

                        Bitmap bitmap = getBitmapFromCache(imageUrl);
                        if (bitmap == null) {
                            bitmap = downloadImage(imageUrl);
                            if (bitmap != null) {
                                addBitmapToCache(imageUrl, bitmap);
                            }
                        }

                        if (bitmap != null) {
                            images.add(bitmap);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return images;
            }

            @Override
            protected void onPostExecute(List<Bitmap> images) {
                super.onPostExecute(images);
                progressBar.setVisibility(View.GONE);
                adapter.setImages(images);
            }
        }.execute();
    }

    private Bitmap downloadImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromCache(key) == null) {
            memoryCache.put(key, bitmap);

            saveBitmapToDiskCache(key, bitmap);
        }
    }

    private Bitmap getBitmapFromCache(String key) {
        return memoryCache.get(key);
    }

    private void saveBitmapToDiskCache(String key, Bitmap bitmap) {
        try {
            File file = new File(cacheDir, key.hashCode() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromDiskCache(String key) {
        try {
            File file = new File(cacheDir, key.hashCode() + ".png");
            FileInputStream inputStream = new FileInputStream(file);
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (NetworkUtil.isNetworkAvailable(context)) {
                fetchImages();
            } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

}