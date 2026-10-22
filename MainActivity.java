package com.sampanstore.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import android.webkit.JavascriptInterface;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int CREATE_BACKUP_FILE = 1001;
    private static final int OPEN_BACKUP_FILE = 1002;
    private static final String BACKUP_FILE = "sampan-store-auto-backup.json";
    private WebView webView;
    private String pendingExportJson = "{}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("UTF-8");

        // Only load the bundled local HTML file; do not expose this bridge to arbitrary websites.
        webView.addJavascriptInterface(new BackupBridge(), "SampanBackup");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Give the HTML app a chance to restore from the private auto-backup if its storage is empty.
                view.evaluateJavascript("if (window.sampanRestoreFromNativeBackup) { window.sampanRestoreFromNativeBackup(); }", null);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    private File getAutoBackupFile() {
        return new File(getFilesDir(), BACKUP_FILE);
    }

    private void saveAutoBackup(String json) {
        try (FileOutputStream fos = new FileOutputStream(getAutoBackupFile())) {
            fos.write(json.getBytes(StandardCharsets.UTF_8));
            fos.flush();
        } catch (Exception e) {
            Log.e("SampanStore", "Auto-backup failed", e);
        }
    }

    private String readAutoBackup() {
        File f = getAutoBackupFile();
        if (!f.exists()) return "";
        try (FileInputStream fis = new FileInputStream(f)) {
            ByteArrayOutputStreamEx out = new ByteArrayOutputStreamEx();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = fis.read(buffer)) > 0) out.write(buffer, 0, n);
            return out.toStringUtf8();
        } catch (Exception e) {
            Log.e("SampanStore", "Auto-backup read failed", e);
            return "";
        }
    }

    private class BackupBridge {
        @JavascriptInterface
        public void saveAutoBackup(String json) {
            if (json == null || json.length() > 10_000_000) return;
            try { new JSONObject(json); } catch (JSONException e) { return; }
            runOnUiThread(() -> saveAutoBackup(json));
        }

        @JavascriptInterface
        public String getAutoBackup() {
            return readAutoBackup();
        }

        @JavascriptInterface
        public void exportBackup(String json) {
            if (json == null || json.length() > 10_000_000) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "ব্যাকআপ ফাইলটি অনেক বড় বা সঠিক নয়।", Toast.LENGTH_LONG).show());
                return;
            }
            try { new JSONObject(json); } catch (JSONException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "ব্যাকআপ ডেটা সঠিক নয়।", Toast.LENGTH_LONG).show());
                return;
            }
            pendingExportJson = json;
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "SampanStore-Backup.json");
                startActivityForResult(intent, CREATE_BACKUP_FILE);
            });
        }

        @JavascriptInterface
        public void importBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, OPEN_BACKUP_FILE);
            });
        }

        @JavascriptInterface
        public void showMessage(String message) {
            if (message == null) return;
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == CREATE_BACKUP_FILE) {
            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                if (os == null) throw new Exception("Cannot open output stream");
                os.write(pendingExportJson.getBytes(StandardCharsets.UTF_8));
                os.flush();
                Toast.makeText(this, "ব্যাকআপ ফাইল সংরক্ষণ করা হয়েছে।", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "ব্যাকআপ ফাইল সংরক্ষণ করা যায়নি।", Toast.LENGTH_LONG).show();
                Log.e("SampanStore", "Export failed", e);
            }
        } else if (requestCode == OPEN_BACKUP_FILE) {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) throw new Exception("Cannot open input stream");
                String json = readStream(is);
                // Validate before passing data to the app.
                JSONObject parsed = new JSONObject(json);
                String quoted = JSONObject.quote(parsed.toString());
                webView.evaluateJavascript("if (window.sampanImportBackupFromNative) { window.sampanImportBackupFromNative(" + quoted + "); }", null);
                saveAutoBackup(parsed.toString());
                Toast.makeText(this, "ব্যাকআপ লোড হয়েছে। অ্যাপের হিসাব যাচাই করুন।", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "ফাইলটি সঠিক JSON ব্যাকআপ নয়।", Toast.LENGTH_LONG).show();
                Log.e("SampanStore", "Import failed", e);
            }
        }
    }

    private String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        if (sb.length() > 10_000_000) throw new Exception("Backup file too large");
        return sb.toString();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    // Small helper to avoid requiring any extra library.
    private static class ByteArrayOutputStreamEx extends java.io.ByteArrayOutputStream {
        String toStringUtf8() { return new String(toByteArray(), StandardCharsets.UTF_8); }
    }
}
