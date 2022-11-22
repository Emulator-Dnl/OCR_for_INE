package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

//TODO: recibo mapa de bits, retorno diccionario de datos
public class ReadINE {
    private static String TAG = "MainActivity";
    private ReadImageText readImageText;
    private Bundle fields;

    public Bundle getFields(){
        return fields;
    }

    public String getString(String key){
        return fields.getString(key);
    }

    public ReadINE(Context context, Bitmap bitmap) {
        fields = new Bundle();

        try {
            Mat img = new Mat();
            Utils.bitmapToMat(bitmap, img);

            //Mat img = Utils.loadResource(context, R.drawable.ine1, CvType.CV_8UC4);
            Mat imgGray = new Mat();
            Mat imgCanny = new Mat();
            Mat imgDilate = new Mat();
            Mat dst = new Mat();

            Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.Canny(imgGray, imgCanny,50,100);
            Imgproc.dilate(imgCanny, imgDilate, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            final Mat hierarchy = new Mat();
            Imgproc.findContours(imgDilate, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            //Imgproc.drawContours(img, contours, -1, new Scalar(120), 2);

            //Ordena los contornos de menor a mayor, el mayor es la ine
            contours.sort(new Comparator<MatOfPoint>() {
                public int compare(MatOfPoint c1, MatOfPoint c2) {
                    return (int) (Imgproc.contourArea(c1)- Imgproc.contourArea(c2));
                }
            });

            //imprime en logcat el último contorno (el mayor), en caso de la imagen de ejemplo; 25
            //Log.d(TAG, contours.get(contours.size()-1).toList().toString());

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

                //esos puntos no están ordenados porque los ordeno en las funciones robadas
                Imgproc.circle(img,approx.toList().get(0),7, new Scalar(50), 10);
                Imgproc.circle(img,approx.toList().get(1),7, new Scalar(100), 10);
                Imgproc.circle(img,approx.toList().get(2),7, new Scalar(200), 10);
                Imgproc.circle(img,approx.toList().get(3),7, new Scalar(255), 10);

                dst = transform(imgGray,approx2f);

            }else{
                //Si se detecta que la figura no tiene 4 lados, el programa falla
                Log.d(TAG, "Se detectó un numero diferente a los 4 lados de la credencial");
                return;
            }

            //A diferencia de en la implementación en python, aquí definimos alto y ancho, no otro punto
            Mat nacimiento = cropMat(dst, 81,30,15,6);
            Mat sexo = cropMat(dst, 93,36,4,4);
            Mat nombre = cropMat(dst, 31,30,29,15);
            Mat domicilio = cropMat(dst, 31,50,34,10);
            Mat clave = cropMat(dst, 49,66,31,6);
            Mat curp = cropMat(dst, 37,72,29,6);
            Mat estado = cropMat(dst, 40,80,5,5);
            Mat municipio = cropMat(dst, 62,80,7,5);
            Mat seccion = cropMat(dst, 80,80,8,5);
            Mat localidad = cropMat(dst, 42,86,8,6);

            String[] fieldNames = {"nacimiento", "sexo", "nombre", "domicilio", "clave", "curp", "estado", "municipio", "seccion", "localidad"};
            Mat[] ine = {nacimiento, sexo, nombre, domicilio, clave, curp, estado, municipio, seccion, localidad};

            int i = 0;
            for (Mat campo: ine
            ) {
                Bitmap bmp=Bitmap.createBitmap(campo.width(), campo.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(campo, bmp);
                //imageView.setImageBitmap(bmp);

                readImageText = new ReadImageText(context);
                Log.d(TAG, readImageText.processImage(bmp, "spa"));

                fields.putString(fieldNames[i], readImageText.processImage(bmp, "spa"));
                i++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Mat cropMat(Mat mat, int x, int y, int width, int height){
        float rows1percent = (float) mat.rows()/100;
        float cols1percent = (float) mat.cols()/100;

        return new Mat(mat, new Rect(Math.round(cols1percent*x),
                Math.round(rows1percent*y),
                Math.round(cols1percent*width),
                Math.round(rows1percent*height)));
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

    protected void finalize(){
        readImageText.recycle();
    }
}
