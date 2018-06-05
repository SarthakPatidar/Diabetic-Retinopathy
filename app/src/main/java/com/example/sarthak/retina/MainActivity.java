package com.example.sarthak.retina;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    ImageView imageView1,imageView2;
    Button btn;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super. onCreate(savedInstanceState);
        setContentView (R.layout.activity_main);


        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        if(!OpenCVLoader.initDebug())
        {
            Toast.makeText(MainActivity.this,"OpenCV not loaded",Toast.LENGTH_LONG).show();
        }
        else
            {
            Toast.makeText(MainActivity.this,"OpenCV loaded",Toast.LENGTH_LONG).show();
            }


        imageView1 =(ImageView)findViewById(R.id.img1);
        imageView2=(ImageView)findViewById(R.id.img2);

        System. loadLibrary ("opencv_java3");
        Bitmap img = BitmapFactory.decodeResource(getResources(),R.drawable.image);


        btn =(Button)findViewById(R.id.click);

        imageView1.setImageBitmap(img);

        btn. setOnClickListener(this);
    }

    public native void imageFromJNI(long ipImge,long opImage);

    @Override
    public void onClick(View v)
    {
        Bitmap img = BitmapFactory.decodeResource(getResources(),R.drawable.image);
        final Mat source = new Mat();
        Utils.bitmapToMat(img,source);
        final Mat dest = new Mat();
        final Button convert = (Button)findViewById(R.id.click);
        convert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int[] colors = {0};
                Bitmap dest_bitmap = Bitmap.createBitmap(colors,source.cols(),source.rows(),Bitmap.Config.RGB_565);
                imageFromJNI(source.getNativeObjAddr(),dest.getNativeObjAddr());
                imageView2.setImageBitmap(dest_bitmap);
                convert.setEnabled(false);
            }
        });

    }


    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
   // public native String stringFromJNI();
}
