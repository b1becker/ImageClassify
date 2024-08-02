package com.example.imageclassify;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.imageclassify.ml.AutoModel2;
import com.example.imageclassify.ml.Effnet730Metadata;
import com.example.imageclassify.ml.MobilenetV1075160Quantized;
import com.example.imageclassify.ml.ModelFlowers;
import com.example.imageclassify.ml.Resnet0802Metadata;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button selectBtn, predictBtn, captureBtn;
    TextView result, time_el;
    Bitmap bitmap;
    ImageView imageView;
    Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        spinner = findViewById(R.id.llmselection);
        predictBtn = findViewById(R.id.predictBtn);
        result = findViewById(R.id.result);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_items, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        String[] labels = new String[1001];
        int count=0;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open("labels.txt")));
            String line = bufferedReader.readLine();
            while(line!= null){
                labels[count] = line;
                count ++;
                line = bufferedReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Check if the Spinner's selected item is "Resnet18"
                String selectedItem = spinner.getSelectedItem().toString();
                if (selectedItem.equals("Resnet 50")) {
                    // Set the TextView to "Resnet"
                    result.setText("Resnet");
                    resnetClassify(labels);
                } else if (selectedItem.equals("MobileNet")) {
                    result.setText("MobileNet");
                    mobileNetClassify();
                } else if (selectedItem.equals("EfficentNet")) {
                    result.setText("effnet");
                    effnetClassify();
                } else if (selectedItem.equals("EfficentNet (quantized)")) {
                    result.setText("effnet quantized");
                    effnetQuantizedClassify();
                } else {
                    // Show a toast message indicating the incorrect selection
                    Toast.makeText(MainActivity.this, "Please select 'Resnet18' from the spinner", Toast.LENGTH_SHORT).show();
                }
            }

        });


        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new NormalizeOp(0.0f, 255.0f))
                .add(new ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
                .build();


        getPermission();

        selectBtn = findViewById(R.id.selectBtn);

        captureBtn = findViewById(R.id.captureBtn);

        imageView = findViewById(R.id.imageView);

        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 10);
//                registerForActivityResult(intent, 10);
            }
        });

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);

            }
        });

    }



    int getMax(float[] arr){
        int max= 0;
        for(int i = 0 ; i < arr.length ; i++){
            if(arr[i] > arr[max]){
                max = i;

            }
        }
        return max;
    }
    void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 11){
            if(grantResults.length>0){
                if(grantResults[0]!=PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(requestCode==10){
            if(data!=null){
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (requestCode == 12) {
            bitmap = (Bitmap) data.getExtras().get("data");
            imageView.setImageBitmap(bitmap);

        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    void effnetQuantizedClassify(){
        result = findViewById(R.id.result);
        time_el = findViewById(R.id.time_elapsed);
        TimeTracker t = new TimeTracker();

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    AutoModel2 model = AutoModel2.newInstance(MainActivity.this);

                    // Creates inputs for reference.
                    TensorImage image = TensorImage.fromBitmap(bitmap);

                    long startTime = System.nanoTime();

                    // Runs model inference and gets result.
                    AutoModel2.Outputs outputs = model.process(image);
                    List<Category> probability = outputs.getProbabilityAsCategoryList();

                    long endTime = System.nanoTime();

                    t.setTimeElapsed(startTime, endTime);

                    // Find the category with the highest probability
                    Category bestCategory = null;
                    for (Category category : probability) {
                        if (bestCategory == null || category.getScore() > bestCategory.getScore()) {
                            bestCategory = category;
                        }
                    }

                    // Display the result using result.setText()
                    if (bestCategory != null) {
                        result.setText(String.format("%s: %.2f%%", bestCategory.getLabel(), bestCategory.getScore() * 100));
                    } else {
                        result.setText("No categories found");
                    }
                    time_el.setText(t.getTimeElapsed()+" miliseconds");

                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }

            }
        });
    }

    void mobileNetClassify(){
        result = findViewById(R.id.result);
        time_el = findViewById(R.id.time_elapsed);
        TimeTracker t = new TimeTracker();
        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    ModelFlowers model = ModelFlowers.newInstance(MainActivity.this);

                    // Creates inputs for reference.
                    long startTime = System.nanoTime();
                    TensorImage image = TensorImage.fromBitmap(bitmap);

                    // Runs model inference and gets result. We are going to apply this to resnet tmrw
                    ModelFlowers.Outputs outputs = model.process(image);
                    List<Category> probability = outputs.getProbabilityAsCategoryList();
                    long endTime = System.nanoTime();

                    t.setTimeElapsed(startTime, endTime);

                    // Find the category with the highest probability
                    Category bestCategory = null;
                    for (Category category : probability) {
                        if (bestCategory == null || category.getScore() > bestCategory.getScore()) {
                            bestCategory = category;
                        }
                    }

                    // Display the result using result.setText()
                    if (bestCategory != null) {
                        result.setText(String.format("%s: %.2f%%", bestCategory.getLabel(), bestCategory.getScore() * 100));
                    } else {
                        result.setText("No categories found");
                    }
                    time_el.setText(t.getTimeElapsed()+" miliseconds");

                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }

            }
        });
    }

    void resnetClassify(String[] labels){
        result = findViewById(R.id.result);
        TimeTracker t = new TimeTracker();
        time_el = findViewById(R.id.time_elapsed);

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Sample image
                ImageView imageView = findViewById(R.id.imageView);

                // Load the image from assets

                try {
                    Resnet0802Metadata model = Resnet0802Metadata.newInstance(MainActivity.this);

                    // Creates inputs for reference.
                    long startTime = System.nanoTime();
                    TensorImage image = TensorImage.fromBitmap(bitmap);

                    // Runs model inference and gets result.
                    Resnet0802Metadata.Outputs outputs = model.process(image);
                    List<Category> probability = outputs.getProbabilityAsCategoryList();

                    long endTime = System.nanoTime();
                    t.setTimeElapsed(startTime,endTime);
                    // Find the category with the highest probability
                    Category bestCategory = null;
                    for (Category category : probability) {
                        if (bestCategory == null || category.getScore() > bestCategory.getScore()) {
                            bestCategory = category;
                        }
                    }


                    // Display the result using result.setText()
                    if (bestCategory != null) {
                        result.setText(String.format("%s: %.2f%%", bestCategory.getLabel(), bestCategory.getScore()));
                    } else {
                        result.setText("No categories found");
                    }
                    time_el.setText(t.getTimeElapsed()+" milliseconds");

                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }

            }
        });

    }
    public ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocateDirect(bytes);
        buffer.order(ByteOrder.nativeOrder());
        bitmap.copyPixelsToBuffer(buffer);
        buffer.rewind();
        return buffer;
    }




    void effnetClassify() {
        result = findViewById(R.id.result);
        time_el = findViewById(R.id.time_elapsed);
        TimeTracker t = new TimeTracker();
        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    AutoModel2 model = AutoModel2.newInstance(MainActivity.this);

                    // Creates inputs for reference.
                    TensorImage image = TensorImage.fromBitmap(bitmap);

                    long startTime = System.nanoTime();
                    // Runs model inference and gets result.
                    AutoModel2.Outputs outputs = model.process(image);
                    List<Category> probability = outputs.getProbabilityAsCategoryList();
                    long endTime = System.nanoTime();

                    t.setTimeElapsed(startTime, endTime);

                    // Find the category with the highest probability
                    Category maxCategory = null;
                    for (Category category : probability) {
                        if (maxCategory == null || category.getScore() > maxCategory.getScore()) {
                            maxCategory = category;
                        }
                    }

                    // Print the category with the highest probability
                    if (maxCategory != null) {
                        result.setText(maxCategory.getLabel() + " " + maxCategory.getScore());
                    } else {
                        result.setText("No categories found.");
                    }

                    time_el.setText("Time Elapsed: "+ t.getTimeElapsed() +" miliseconds");

                    // Releases model resources if no longer used.
                    model.close();
                } catch (IOException e) {
                    // TODO Handle the exception
                }




            }
        });

    }

    private int getMaxIndex(float[] array) {
        int maxIndex = -1;
        float maxValue = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }



}

