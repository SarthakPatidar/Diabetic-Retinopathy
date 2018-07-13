package com.example.sarthak.retina;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.MORPH_ELLIPSE;
import static org.opencv.imgproc.Imgproc.createCLAHE;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }


    ImageView imageView1,imageView2,imageView3;
    Button btn;
    Bitmap img;
    public native void imageFromJNI(long inpAddr,long outAddr);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(MainActivity.this, "OpenCV not loaded", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "OpenCV loaded", Toast.LENGTH_LONG).show();
        }


        imageView1 = (ImageView) findViewById(R.id.img1);
        imageView2 = (ImageView) findViewById(R.id.img2);
        imageView3 = (ImageView) findViewById(R.id.img3);

        btn = (Button) findViewById(R.id.click);

        System.loadLibrary("opencv_java3");
        img = BitmapFactory.decodeResource(getResources(), R.drawable.image);

        imageView1.setImageBitmap(img);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Mat source = new Mat();
                Mat optic_cup = new Mat();
                Mat optic_disc = new Mat();
                Utils.bitmapToMat(img,source);

                Mat sample1 = new Mat();
                source.copyTo(sample1);

                Mat sample2 = new Mat();
                source.copyTo(sample2);

                System.out.println("Size of Source: "+source.size());
                System.out.println("Type of Source: "+ source.type());

                List<Mat> channels = new ArrayList<Mat>(3);
                Core.split(source, channels);
                Mat Green = channels.get(1);
                Mat Red = channels.get(0);


                CLAHE clahe = createCLAHE();
                clahe.setClipLimit(4);
                clahe.apply(Red,Red);

                Imgproc.medianBlur(Red,Red,3);

                Bitmap dest_disc_bitmap = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.RGB_565);
                Bitmap dest_cup_bitmap = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.RGB_565);
                Bitmap sample_bitmap = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.RGB_565);
                Bitmap source_bitmap = Bitmap.createBitmap(img.getWidth(),img.getHeight(), Bitmap.Config.RGB_565);


                Utils.matToBitmap(source,source_bitmap);
                imageView1.setImageBitmap(source_bitmap);

                Mat red_element = Imgproc.getStructuringElement(MORPH_ELLIPSE,new Size(49,49));
                Imgproc.morphologyEx(Red,optic_disc,MORPH_CLOSE,red_element);


                Mat green_element = Imgproc.getStructuringElement(MORPH_ELLIPSE,new Size(49,49));
                Imgproc.morphologyEx(Green,optic_cup,MORPH_CLOSE,green_element);

                double max_cup = apply_kmeans(optic_cup,1,4);
                double max_disc = apply_kmeans(optic_disc,2,5);

                Core.MinMaxLocResult max = Core.minMaxLoc(optic_disc);
                Point maxPoint = max.maxLoc;

                int cup_radius = fit_contours(optic_cup,max_cup,maxPoint,sample1);
                int disc_radius = fit_contours(optic_disc,max_disc,maxPoint,sample2);

                Toast.makeText(MainActivity.this, "Radius of Cup: "+cup_radius, Toast.LENGTH_LONG).show();
                Toast.makeText(MainActivity.this, "Radius of Disk: "+disc_radius, Toast.LENGTH_LONG).show();


                Utils.matToBitmap(sample1,dest_cup_bitmap);
                imageView2.setImageBitmap(dest_cup_bitmap);

                Utils.matToBitmap(sample2,dest_disc_bitmap);
                imageView3.setImageBitmap(dest_disc_bitmap);

            }
        });
    }

    protected double apply_kmeans(Mat object,int val,int number){

        Mat sample = object.reshape(1,object.cols()*object.rows());
        Mat sample32f = new Mat();

        sample.convertTo(sample32f, CvType.CV_32F,1.0/255.0);

        Mat labels = new Mat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS,10000,0.001);

        Mat centers = new Mat();

        Core.kmeans(sample32f,number,labels,criteria,2,Core.KMEANS_RANDOM_CENTERS,centers);

        System.out.println("Center Size: "+centers.size());
        centers.convertTo(centers,CvType.CV_8UC1,255.0);
        centers.reshape(number);


        System.out.println("Centers: "+centers.dump());

        Core.MinMaxLocResult r = Core.minMaxLoc(centers);
        double max = r.maxVal;

        if(val == 2)
        {
            double maxTwo = 0;
            for(int i=0;i<centers.rows();i++){
                double[] n = centers.get(i,0);
                System.out.println("n value: "+n[0]);
                if(max < n[0]){
                    maxTwo = max;
                    max = n[0];

                }else if(maxTwo < n[0] && n[0] != max){
                    maxTwo = n[0];
                }
            }
            max = (maxTwo+max)/2;
        }
        System.out.println("Max value: "+max);
        return max;
    }

    protected int fit_contours(Mat object,double thresh,Point maxPoint,Mat source){

        int maxRadius = 0;

        Mat binary_object = new Mat();
        Imgproc.threshold(object,binary_object,thresh,255,Imgproc.THRESH_BINARY);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(binary_object, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        double maxArea = 0;double[] intensity = new double[(int)object.total()*object.channels()];
        float[] radius = new float[1];
        Point center = new Point();

        double min_distance = 9999,distance;
        Point required_center = new Point();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint c = contours.get(i);
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            Imgproc.minEnclosingCircle(c2f, center, radius);

            System.out.println("Max Point x: "+maxPoint.x);
            System.out.println("Max Point y: "+maxPoint.y);
            System.out.println("Center Point x: "+center.x);
            System.out.println("Center Point y: "+center.y);

            distance = Math.sqrt(Math.pow(center.x - maxPoint.x,2)+Math.pow(center.y - maxPoint.y,2));

            System.out.println("Distance is: "+distance);

             if(min_distance > distance){
                maxRadius = (int)radius[0];
                required_center = center.clone();
                min_distance = distance;
                 System.out.println("Minimum Distance is: "+distance);
            }
        }

        System.out.println("Required Radius: "+maxRadius);
        System.out.println("Final Center is: x="+required_center.x+" y="+required_center.y);

        Imgproc.circle(source, required_center,maxRadius, new Scalar(0, 255, 0), 15);

        return  maxRadius;
    }
}