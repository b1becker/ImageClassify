package com.example.imageclassify;

import android.app.Activity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SpinnerActivity extends Activity implements AdapterView.OnItemSelectedListener{

    Button predictBtn;
    TextView result;


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
        predictBtn = findViewById(R.id.predictBtn);
        result = findViewById(R.id.result);


        String selectedItem = parent.getItemAtPosition(pos).toString();
        if (selectedItem.equals("Resnet18")) {
            predictBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    result.setText("Resnet");
                }
            });
            Toast.makeText(SpinnerActivity.this, "Resnet18", Toast.LENGTH_SHORT).show();
        } else if (selectedItem.equals("MobileNet")) {
            // Execute code for Item 2
            Toast.makeText(SpinnerActivity.this, "Item 2 selected", Toast.LENGTH_SHORT).show();
        } else if (selectedItem.equals("Item 3")) {
            // Execute code for Item 3
            Toast.makeText(SpinnerActivity.this, "Item 3 selected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}