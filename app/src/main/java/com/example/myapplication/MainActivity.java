package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    static{
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "si");
        }
        else{
            Log.d(TAG, "no");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);

        imageView.setImageResource(R.drawable.ine1);

        try {
            Mat img = Utils.loadResource(this, R.drawable.ine1, CvType.CV_8UC4);

            Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Canny(img,img,50,100);
            Imgproc.dilate(img,img, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Imgproc.findContours(img, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            //Imgproc.drawContours(img, contours, -1, new Scalar(120), 5);

            //TODO: este bloque no tiene importancia todavía, pero en python hago un ordenamiento
            contours.sort(new Comparator<MatOfPoint>() {
                public int compare(MatOfPoint c1, MatOfPoint c2) {
                    return (int) (Imgproc.contourArea(c1)- Imgproc.contourArea(c2));
                }
            });

            MatOfPoint c=new MatOfPoint();
            final int size = contours.size();
            for (int i = 0; i < size; i++)
            {
                c = contours.get(i);
                MatOfPoint2f c2f = new MatOfPoint2f( c.toArray() );
                double epsilon = 0.01*Imgproc.arcLength(c2f, true);

                MatOfPoint2f approx2f = new MatOfPoint2f();
                Imgproc.approxPolyDP(c2f, approx2f, epsilon, true);

                if (approx2f.rows()==4){
                    MatOfPoint approx = new MatOfPoint( approx2f.toArray() );
                    List<MatOfPoint> approxlist = new ArrayList<MatOfPoint>();
                    approxlist.add(approx);
                    Imgproc.drawContours(img, approxlist, 0, new Scalar(200), 5);

                    //TODO: te quedaste en el equivalente de la linea 38 de python, tuviste la idea de la suma de coordenadas para sacar el primero y el ultimo
                    //TODO: pero te quedaste entendiendo la variable approx y cómo obtener las coordenadas
                    Log.d(TAG, approx.toList().toString());
                    //puntos = ordenar_puntos(approxlist);

                }
            }

            Bitmap bmp=Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bmp);
            imageView.setImageBitmap(bmp);



        } catch (IOException e) {
            e.printStackTrace();
        }



        //there could be some processing
        //Imgproc.cvtColor(img, img, Imgproc.COLOR_GRAY2RGB, 4);
        //Utils.matToBitmap(tmp, b);
    }
}