package com.zombiewalk;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class PermissionsRationaleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_rationale);

        TextView textView = findViewById(R.id.privacy);
        textView.setText("your app's rationale of the requested permissions, describing how the user's data is used and handled.");

    }
}