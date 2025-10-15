package com.termux.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;

import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.file.TermuxFileUtils;
import com.termux.shared.logger.Logger;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BootstrapChooser extends Activity {

    private static final int PICK_BOOTSTRAP_FILE = 1001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Open file picker
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        Toast.makeText(this, "Select your Termux bootstrap ZIP file", Toast.LENGTH_LONG).show();
        startActivityForResult(Intent.createChooser(intent, "Choose Bootstrap ZIP"), PICK_BOOTSTRAP_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_BOOTSTRAP_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Toast.makeText(this, "No file selected.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            extractBootstrap(uri);
        } else {
            Toast.makeText(this, "Bootstrap selection cancelled.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void extractBootstrap(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            File destDir = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH);
            if (!destDir.exists()) destDir.mkdirs();

            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File outFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int len;
                        while ((len = zipInputStream.read(buffer)) > 0) {
                            fos.write(buffer, len, 0, len);
                        }
                    }
                }
            }

            Toast.makeText(this, "Bootstrap installed successfully!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Logger.logError("BootstrapChooser", "Extraction failed: " + e.getMessage());
            Toast.makeText(this, "Extraction failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            finish();
        }
    }
}
