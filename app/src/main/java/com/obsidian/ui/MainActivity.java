package com.obsidian.ui;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.obsidian.R;

public class MainActivity extends Activity {
    private TextView statusText;
    private TextView porosityText;
    private TextView nodesText;
    private TextView chainsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.status_text);
        porosityText = (TextView) findViewById(R.id.porosity_text);
        nodesText = (TextView) findViewById(R.id.nodes_text);
        chainsText = (TextView) findViewById(R.id.chains_text);

        Button refreshButton = (Button) findViewById(R.id.scan_button);
        Button safetyButton = (Button) findViewById(R.id.inject_button);
        Button localButton = (Button) findViewById(R.id.sync_button);
        Button logsButton = (Button) findViewById(R.id.logs_button);

        updateDashboard("Local-only mode");

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDashboard("Dashboard refreshed");
            }
        });
        safetyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Automation features are disabled.");
            }
        });
        localButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDashboard("Using local data only");
            }
        });
        logsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("No remote logs are available.");
            }
        });
    }

    private void updateDashboard(String status) {
        statusText.setText(status);
        statusText.setTextColor(Color.parseColor("#00FF41"));
        porosityText.setText("Saved Profiles: 0");
        nodesText.setText("Notes Logged: 0");
        chainsText.setText("Review Flags: 0");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
