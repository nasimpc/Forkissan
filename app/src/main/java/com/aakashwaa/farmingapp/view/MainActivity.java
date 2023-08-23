package com.aakashwaa.farmingapp.view;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.aakashwaa.farmingapp.R;
import com.aakashwaa.farmingapp.ml.DiseaseDetection;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity {
    TextView result, demoTxt, classified, clickHere,des;
    ImageView imageView, arrowImage;
    LinearLayout picture;

    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        des=findViewById(R.id.des);
        imageView = findViewById(R.id.imageView);
        picture = findViewById(R.id.button);

        demoTxt = findViewById(R.id.demoText);
        clickHere = findViewById(R.id.click_here);
        //arrowImage = findViewById(R.id.demoArrow);
        classified = findViewById(R.id.classified);

        demoTxt.setVisibility(View.VISIBLE);
        clickHere.setVisibility(View.GONE);
        //arrowImage.setVisibility(View.GONE);
        classified.setVisibility(View.VISIBLE);
        result.setVisibility(View.GONE);

        picture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                int requestCode;
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            imageView.setImageBitmap(image);

            demoTxt.setVisibility(View.GONE);
            clickHere.setVisibility(View.VISIBLE);
            //arrowImage.setVisibility(View.GONE);
            classified.setVisibility(View.VISIBLE);
            result.setVisibility(View.VISIBLE);

            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);


        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void classifyImage(Bitmap image) {
        try {
            DiseaseDetection model = DiseaseDetection.newInstance(getApplicationContext());
            //create in put reference
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            //get 1D array of 224 * 224 pixels in image
            int[] intValue = new int[imageSize * imageSize];
            image.getPixels(intValue,0, image.getWidth(), 0,0,image.getWidth(),image.getHeight());

            // iterate over pixels and extract R, G,B values, add to bytebuffer
            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValue[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }
            inputFeature0.loadBuffer(byteBuffer);

            //run the model interface and get the result
            DiseaseDetection.Outputs outputs = model.process (inputFeature0) ;
            TensorBuffer outputFeatures0 = outputs.getOutputFeature0AsTensorBuffer();
            float[] confidence = outputFeatures0.getFloatArray();
            String s=" ";
            //find the index of the class with biggest confidence
            int maxPos = 0;
            float maxConfidence =0;
            for (int i = 0; i < confidence. length; i++){
                //System.out.print(confidence[i]);
                //String st = Float. toString(confidence[i]);
                //s=s.concat(st+" ");
                if (confidence[i] > maxConfidence) {
                    maxConfidence = confidence[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Apple_Apple_scab","Apple_Black_rot","Apple_cedar_apple_rust","Apple_healthy","Cherry_Healthy","Cherry_powdery_mildew","pepper_Bell_bacterial_spot","Rice__leaf_blight","Rice_Hispa","Rice_healthy","scan again"};
            //result.setText(classes[maxPos]);
            if(maxPos==0){
                des.setText("Remedy Recommendation:\nChoose scab-resistant varieties of apple or crabapple trees, Prune your apple and crabapple trees to keep their crowns open so light and air can move through");
            }
            else if(maxPos==1){
                des.setText("Remedy Recommendation:\nPrune out dead or diseased branches, All infected plant parts should be burned, buried or sent to a municipal composting site.");
            }
            else if(maxPos==2) {
                des.setText("Remedy Recommendation:\n Fungicides with the active ingredient Myclobutanil are most effective in preventing rust, Fungicides are only effective if applied before leaf spots or fruit infection appear.");
            }
            else if(maxPos==3) {
                des.setText(" ");
            }
            else if(maxPos==4) {
                des.setText(" ");
            }
            else if(maxPos==5) {
                des.setText("Remedy Recommendation:\n Plant resistant cultivars in sunny locations whenever possible, Prune or stake plants to improve air circulation. Make sure to disinfect your pruning tools (one part bleach to 4 parts water) after each cut.,Remove diseased foliage from the plant and clean up fallen debris on the ground.");
            }
            else if(maxPos==6) {
                des.setText("Remedy Recommendation:\n Copper sprays can be used to control bacterial leaf spot, but they are not as effective when used alone on a continuous basis");
            }
            else if(maxPos==7) {
                des.setText("Remedy Recommendation:\n Spray Streptomycin sulphate + Tetracycline combination 300 g + Copper oxychloride 1.25kg/ha. If necessary repeat 15 days later. Application of bleaching powder @ 5 kg/ha in the irrigation water is recommended in the kresek stage.");
            }
            else if(maxPos==8) {
                des.setText("Remedy Recommendation:\n Avoid over fertilizing the field, Close plant spacing results in greater leaf densities that can tolerate higher hispa numbers.,Leaf tip containing blotch mines should be destroyed. ");
            }
            else if(maxPos==9) {
                des.setText(" ");
            }
            else {
                des.setText(" ");
            }
            String n=classes[maxPos];
            //s=s.concat(n);
            result.setText(n);
            result.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //to search the disease on the internet
                    //startActivity(new Intent(Intent.ACTION_VIEW,
                    //Uri.parse("http://google.com/search?="+result.getText())));
                    //Uri.parse("http://google.com/search?="+"cat")));
                    Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
                    intent.putExtra(SearchManager.QUERY,result.getText()); // query contains search string
                    startActivity(intent);
                }
            });
            model.close();
        }catch (IOException e){
            //Handle the exception

        }
    }
}