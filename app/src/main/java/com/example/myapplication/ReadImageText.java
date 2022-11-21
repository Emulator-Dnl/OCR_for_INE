package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
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


    public ReadImageText(Context context) throws IOException {
        this.context=context;

        File folder = new File(dataPath, tessDataFolderName);
        if (!folder.exists()){
            folder.mkdirs();
            Log.d("MainActivity", "se crea path");
        }

        //TODO: Siempre se añade/sobreescribe si la carpeta existe, aún si ya está ahí el archivo de entrenamiento; ineficiente
        if (folder.exists()){
            addFile("spa.traineddata",R.raw.spa_old);
            //Log.d("MainActivity", "se añade archivo de entrenamiento");
        }
    }

    private void addFile (String name, int source) throws IOException {
        File file = new File(dataPath + "/" + tessDataFolderName + "/" + name);
        if (!file.exists()){

            InputStream inputStream = context.getResources().openRawResource(source);
            java.nio.file.Files.copy(
                    inputStream,
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            file.createNewFile();
            inputStream.close();
        }
    }

    public String processImage (Bitmap image, String lang){
        //Log.d("MainActivity", dataPath);
        tess.init(dataPath, lang);
        tess.setImage(image);
        return tess.getUTF8Text();
    }

    public void recycle (){
        tess.recycle();
    }
}
