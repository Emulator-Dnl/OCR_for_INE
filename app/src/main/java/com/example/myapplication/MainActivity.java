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
            //Log.d(TAG, "si");
        }
        else{
            Log.d(TAG, "no");
        }
    }

    private void ordenarPuntos(){

    }

    private Point getMassCenter(MatOfPoint2f points) {
        double xSum = 0;
        double ySum = 0;
        List<Point> pointList = points.toList();
        int len = pointList.size();
        for (Point point : pointList) {
            xSum += point.x;
            ySum += point.y;
        }
        return new Point(xSum / len, ySum / len);
    }

    private MatOfPoint2f sortCorners(MatOfPoint2f corners) {
        Point center = getMassCenter(corners);
        List<Point> points = corners.toList();
        List<Point> topPoints = new ArrayList<Point>();
        List<Point> bottomPoints = new ArrayList<Point>();

        for (Point point : points) {
            if (point.y < center.y) {
                topPoints.add(point);
            } else {
                bottomPoints.add(point);
            }
        }

        Point topLeft = topPoints.get(0).x > topPoints.get(1).x ? topPoints.get(1) : topPoints.get(0);
        Point topRight = topPoints.get(0).x > topPoints.get(1).x ? topPoints.get(0) : topPoints.get(1);
        Point bottomLeft = bottomPoints.get(0).x > bottomPoints.get(1).x ? bottomPoints.get(1) : bottomPoints.get(0);
        Point bottomRight = bottomPoints.get(0).x > bottomPoints.get(1).x ? bottomPoints.get(0) : bottomPoints.get(1);

        MatOfPoint2f result = new MatOfPoint2f();
        Point[] sortedPoints = {topLeft, topRight, bottomRight, bottomLeft};
        result.fromArray(sortedPoints);

        return result;
    }

    private double getDistance(Point p1, Point p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private MatOfPoint2f getOutline(Mat image) {
        Point topLeft = new Point(0, 0);
        Point topRight = new Point(image.cols(), 0);
        Point bottomRight = new Point(image.cols(), image.rows());
        Point bottomLeft = new Point(0, image.rows());
        Point[] points = {topLeft, topRight, bottomRight, bottomLeft};

        MatOfPoint2f result = new MatOfPoint2f();
        result.fromArray(points);

        return result;
    }
    private Size getRectangleSize(MatOfPoint2f rectangle) {
        Point[] corners = rectangle.toArray();

        double top = getDistance(corners[0], corners[1]);
        double right = getDistance(corners[1], corners[2]);
        double bottom = getDistance(corners[2], corners[3]);
        double left = getDistance(corners[3], corners[0]);

        double averageWidth = (top + bottom) / 2f;
        double averageHeight = (right + left) / 2f;

        return new Size(new Point(averageWidth, averageHeight));
    }

    public Mat transform(Mat src, MatOfPoint2f corners) {
        MatOfPoint2f sortedCorners = sortCorners(corners);
        Size size = getRectangleSize(sortedCorners);

        Mat result = Mat.zeros(size, src.type());
        MatOfPoint2f imageOutline = getOutline(result);

        Mat transformation = Imgproc.getPerspectiveTransform(sortedCorners, imageOutline);
        Imgproc.warpPerspective(src, result, transformation, size);

        return result;
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
            final Mat hierarchy = new Mat();
            Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            //Imgproc.drawContours(img, contours, -1, new Scalar(120), 2);

            //Ordena los contornos de menor a mayor, el mayor es la ine
            contours.sort(new Comparator<MatOfPoint>() {
                public int compare(MatOfPoint c1, MatOfPoint c2) {
                    return (int) (Imgproc.contourArea(c1)- Imgproc.contourArea(c2));
                }
            });

            //imprime en logcat el último contorno (el mayor), en caso de la imagen de ejemplo; 25
            Log.d(TAG, contours.get(contours.size()-1).toList().toString());
            //Log.d(TAG, String.valueOf(contours.size()));

            MatOfPoint c = new MatOfPoint();
            c = contours.get(contours.size()-1);

            MatOfPoint2f c2f = new MatOfPoint2f( c.toArray() );
            double epsilon = 0.01*Imgproc.arcLength(c2f, true);

            MatOfPoint2f approx2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx2f, epsilon, true);

            if (approx2f.rows()==4){
                MatOfPoint approx = new MatOfPoint( approx2f.toArray() );
                List<MatOfPoint> approxlist = new ArrayList<MatOfPoint>();
                approxlist.add(approx);
                Imgproc.drawContours(img, approxlist, 0, new Scalar(200), 5);

                Log.d(TAG, approx.toList().toString());

                //ordena los puntos
                List<Point> approxSorted= new ArrayList<Point>();
                approxSorted = approx.toList();
                approxSorted.sort(new Comparator<Point>() {
                    public int compare(Point c1, Point c2) {
                        return (int) (c1.x- c2.x);
                    }
                });

                MatOfPoint2f sortedCorners = sortCorners(approx2f);

                Log.d(TAG, approxSorted.toString());

                Imgproc.circle(img,approxSorted.get(0),7, new Scalar(50), 10);
                Imgproc.circle(img,approxSorted.get(1),7, new Scalar(100), 10);
                Imgproc.circle(img,approxSorted.get(2),7, new Scalar(200), 10);
                Imgproc.circle(img,approxSorted.get(3),7, new Scalar(255), 10);

                MatOfPoint pts2 = new MatOfPoint();
                pts2.fromArray(new Point(0,540),new Point(0,0),new Point(860,0),new Point(860,540));

                //requiero mat pero tengo lista de point
                //Mat M = Imgproc.getPerspectiveTransform(approx, pts2);
                //Imgproc.warpPerspective(img, img, M, new Size(860, 540),0,3, new Scalar(200));

                img=transform(img,approx2f);




            }


            Bitmap bmp=Bitmap.createBitmap(img.width(), img.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(img, bmp);
            imageView.setImageBitmap(bmp);



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}