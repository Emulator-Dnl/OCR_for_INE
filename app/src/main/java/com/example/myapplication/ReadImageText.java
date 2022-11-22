package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

public class ReadImageText {
    private String tessDataFolderName = "tessdata";

    // variable to hold context
    private Context context;

    // Create Tesseract instance
    TessBaseAPI tess = new TessBaseAPI();

    // Given path must contain subdirectory `tessdata` where are `*.traineddata` language files
    String dataPath = new File(Environment.getExternalStorageDirectory()+"/tesseract").getAbsolutePath();


    public ReadImageText(Context context, String lang) throws IOException {
        this.context=context;

        File folder = new File(dataPath, tessDataFolderName);
        if (!folder.exists()){
            folder.mkdirs();
            Log.d("MainActivity", "se crea path");
        }
        if (folder.exists()){
            addFile("spa.traineddata",R.raw.spa_old);
        }

        tess.init(dataPath, lang);
    }

    public String processImage (Bitmap image){
        tess.setImage(image);
        return tess.getUTF8Text();
    }

    private void addFile (String name, int source) throws IOException {
        File file = new File(dataPath + "/" + tessDataFolderName + "/" + name);

        //Este if escribe el archivo únicamente en caso de que no exista; no sobreescribe; eficiente
        if (!file.exists()){
            Log.d("MainActivity", "se añade archivo de entrenamiento");
            //InputStream inputStream = context.getResources().openRawResource(source);
            //java.nio.file.Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //inputStream.close();
            //file.createNewFile();

            copyFiletoExternalStorage(source, dataPath + "/" + tessDataFolderName + "/" + name);
        }
    }

    private void copyFiletoExternalStorage(int resourceId, String resourceName){
        String pathSDCard = Environment.getExternalStorageDirectory() + "/Android/data/" + resourceName;
        try{
            InputStream in = context.getResources().openRawResource(resourceId);
            FileOutputStream out = null;
            out = new FileOutputStream(pathSDCard);
            byte[] buff = new byte[1024];
            int read = 0;
            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void recycle (){
        tess.recycle();
    }
}
