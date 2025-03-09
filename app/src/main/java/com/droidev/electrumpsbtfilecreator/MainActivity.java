package com.droidev.electrumpsbtfilecreator;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private String sharedText;
    private Uri savedFileUri;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        String action = getIntent().getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            handleSendIntent(getIntent());
        }
    }

    private void handleSendIntent(Intent intent) {
        sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            createFile();
        }
    }

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Set MIME type for PSBT files (generic for binary data)
        intent.setType("application/octet-stream");

        String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.getDefault()).format(new Date());
        String fileName = "Electrum-PSBT_" + timeStamp + ".psbt";
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            savedFileUri = data.getData();
            saveTextToFile();
        }
    }

    private void saveTextToFile() {
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(getContentResolver().openOutputStream(savedFileUri)));
            writer.write(sharedText);
            writer.close();
            Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
            checkBluetoothAndSendFile();
        } catch (Exception e) {
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkBluetoothAndSendFile() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            showSendFileDialog();
        } else {
            MainActivity.this.finish();
        }
    }

    private void showSendFileDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Send via Bluetooth")
                .setCancelable(false)
                .setMessage("Bluetooth is enabled. Do you want to send the file?")
                .setPositiveButton("Yes", (dialog, which) -> sendFileViaBluetooth())
                .setNegativeButton("No", (dialog, which) -> MainActivity.this.finish())
                .show();
    }

    private void sendFileViaBluetooth() {
        if (savedFileUri == null) {
            Toast.makeText(this, "File URI is null", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, savedFileUri);
        intent.setPackage("com.android.bluetooth");
        startActivity(Intent.createChooser(intent, "Send file via Bluetooth"));
        MainActivity.this.finish();
    }
}