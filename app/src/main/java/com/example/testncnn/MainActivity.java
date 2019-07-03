package com.example.testncnn;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.FileOutputStream;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int USE_PHOTO = 1001;
    private String camera_image_path;
    private ImageView show_image;
    private TextView result_text;
    private boolean load_result = false;
    private int[] ddims = {1, 3, 385, 385}; //这里的维度的值要和train model的input 一一对应
    private int model_index = 1;
    private List<String> resultLabel = new ArrayList<>();
    private NcnnWrapper mobileNetssd = new NcnnWrapper(); //java接口实例化　下面直接利用java函数调用NDK c++函数

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try
        {
            initNcnn();
        } catch (IOException e) {
            Log.e("MainActivity", "initMobileNetSSD error");
        }
        init_view();
        readCacheLabelFromLocalFile();
    }

    /**
     *
     * MobileNetssd初始化，也就是把model文件进行加载
     */
    private void initNcnn() throws IOException {
        byte[] bin = null;
        String paramString = null;
        {
            //用io流读取二进制文件，最后存入到byte[]数组中
            InputStream assetsInputStream = getAssets().open("test_net.param");// param：  网络结构文件
            int available = assetsInputStream.available();
            byte[] param = new byte[available];
            int byteCode = assetsInputStream.read(param);
            paramString = new String(param);
            assetsInputStream.close();
        }
        {
            //用io流读取二进制文件，最后存入到byte上，转换为int型
            InputStream assetsInputStream = getAssets().open("test_net.bin");//bin：   model文件
            int available = assetsInputStream.available();
            bin = new byte[available];
            int byteCode = assetsInputStream.read(bin);
            assetsInputStream.close();
        }

        load_result = mobileNetssd.Init(paramString, bin);// 再将文件传入java的NDK接口(c++ 代码中的init接口 )
        Log.d("load model", "Load_model_result:" + load_result);
    }


    // initialize view
    private void init_view() {
        request_permissions();
        show_image = (ImageView) findViewById(R.id.show_image);
        result_text = (TextView) findViewById(R.id.result_text);
        result_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        Button use_photo = (Button) findViewById(R.id.use_photo);
        // use photo click
        use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!load_result) {
                    Toast.makeText(MainActivity.this, "never load model", Toast.LENGTH_SHORT).show();
                    return;
                }
                PhotoUtil.use_photo(MainActivity.this, USE_PHOTO);
            }
        });
    }

    // load label's name
    private void readCacheLabelFromLocalFile() {
        try {
            AssetManager assetManager = getApplicationContext().getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("words.txt")));//这里是label的文件
            String readLine = null;
            while ((readLine = reader.readLine()) != null) {
                resultLabel.add(readLine);
            }
            reader.close();
        } catch (Exception e) {
            Log.e("labelCache", "error " + e);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        String image_path;
        RequestOptions options = new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case USE_PHOTO:
                    if (data == null) {
                        Log.w(TAG, "user photo data is null");
                        return;
                    }
                    Uri image_uri = data.getData();

                    //Glide.with(MainActivity.this).load(image_uri).apply(options).into(show_image);

                    // get image path from uri
                    image_path = PhotoUtil.get_path_from_URI(MainActivity.this, image_uri);
                    // predict image
                    predict_image(image_path);
                    break;
            }
        }
    }

    //  predict image
    private void predict_image(String image_path) {
        // picture to float array
        Bitmap input_bmp = PhotoUtil.getSizedBitmap(image_path, 385);
        try {
            // Data format conversion takes too long
            // Log.d("inputData", Arrays.toString(inputData));
            long start = System.currentTimeMillis();
            // get predict result
            float[] result = mobileNetssd.Detect(input_bmp);

            long end = System.currentTimeMillis();

            float minValue = Float.MAX_VALUE, maxValue = -Float.MAX_VALUE;
            final int WIDTH = 513;
            final int HEIGHT = 513;
            int resultSize = WIDTH * HEIGHT;
            for (int i = 0; i < resultSize; ++ i) {
                if (result[i] > maxValue)
                    maxValue = result[i];
                else if (result[i] < minValue)
                    minValue = result[i];
            }

            // Normalize to 0~255
            for (int i = 0; i < resultSize; ++ i)
                result[i] = (result[i] - minValue) / (maxValue - minValue) * 255.f;

            Bitmap bmpResult = Bitmap.createBitmap(513, 513, Bitmap.Config.ARGB_8888);
            bmpResult.setHasAlpha(false);
            for (int y = 0; y < HEIGHT; ++ y){
                for (int x = 0; x < WIDTH; ++ x) {
                    int gray = (int)(result[y * WIDTH + x]);
                    int color = (gray & 0xff) << 16 | (gray & 0xff) << 8 | (gray & 0xff);
                    bmpResult.setPixel(x, y, color);
                }
            }

            show_image.setImageBitmap(bmpResult);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // get max probability label
    private float[] get_max_result(float[] result) {
        int num_rs = result.length / 6;
        float maxProp = result[1];
        int maxI = 0;
        for(int i = 1; i<num_rs;i++){
            if(maxProp<result[i*6+1]){
                maxProp = result[i*6+1];
                maxI = i;
            }
        }
        float[] ret = {0,0,0,0,0,0};
        for(int j=0;j<6;j++){
            ret[j] = result[maxI*6 + j];
        }
        return ret;
    }

    // request permissions(add)
    private void request_permissions() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        // if list is not empty will request permissions
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            String s = permissions[i];
                            Toast.makeText(this, s + "permission was denied", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }
}

