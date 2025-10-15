package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BootstrapChooser extends Activity {

    private static final int PICK_BOOTSTRAP_FILE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launch the file picker immediately
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // allow all file types
        String[] mimeTypes = {"application/zip", "application/x-zip-compressed"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            startActivityForResult(intent, PICK_BOOTSTRAP_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker found on device!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_BOOTSTRAP_FILE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                extractBootstrap(uri);
            } else {
                Toast.makeText(this, "Invalid file selected.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void extractBootstrap(Uri uri) {
        File targetDir = new File(getFilesDir(), "usr");
        if (!targetDir.exists()) targetDir.mkdirs();

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File newFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    File parent = newFile.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, len);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }

            Toast.makeText(this, "Bootstrap installed successfully!", Toast.LENGTH_LONG).show();

            // Go back to TermuxActivity
            startActivity(new Intent(this, TermuxActivity.class));
            finish();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to extract bootstrap: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }
}
