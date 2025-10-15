package com.termux.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.termux.shared.file.FileUtils;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class BootstrapChooser extends Activity {

    private static final int FILE_PICK_REQUEST = 1001;
    private static final String LOG_TAG = "BootstrapChooser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If bootstrap exists, no need to run
        File bootstrapDir = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH);
        if (bootstrapDir.exists() && bootstrapDir.isDirectory()) {
            Toast.makeText(this, "Bootstrap already installed.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Install Bootstrap")
                .setMessage("Bootstrap files not found.\nSelect the bootstrap ZIP to install Termux environment.")
                .setPositiveButton("Choose File", (dialog, which) -> openFilePicker())
                .setNegativeButton("Cancel", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/zip");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Bootstrap ZIP"), FILE_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICK_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Toast.makeText(this, "Invalid file selected.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            installBootstrap(uri);
        } else {
            finish();
        }
    }

    private void installBootstrap(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(this, "Unable to open selected file.", Toast.LENGTH_SHORT).show();
                return;
            }

            File targetDir = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH);
            if (!targetDir.exists()) targetDir.mkdirs();

            File tempZip = new File(getCacheDir(), "bootstrap.zip");
            try (OutputStream out = FileUtils.openOutputStream(tempZip)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Extract bootstrap (use your existing extraction method)
            TermuxInstaller.extractBootstrapZip(this, tempZip);

            Toast.makeText(this, "Bootstrap installed successfully.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Bootstrap installation failed.", e);
            Toast.makeText(this, "Installation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            finish();
        }
    }
    }
