package com.example

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val dataString = result.data?.dataString
            if (dataString != null) {
                fileUploadCallback?.onReceiveValue(arrayOf(Uri.parse(dataString)))
            } else {
                fileUploadCallback?.onReceiveValue(null)
            }
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WebViewScreen(
                url = "https://appsikap-rho.vercel.app/",
                onShowFileChooser = { callback ->
                    fileUploadCallback = callback
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    fileChooserLauncher.launch(Intent.createChooser(intent, "Pilih File JSON"))
                },
                context = this
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, onShowFileChooser: (ValueCallback<Array<Uri>>) -> Unit, context: Context) {
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowFileAccess = true
                    allowContentAccess = true
                }
                
                webViewClient = WebViewClient()
                
                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback?.let { onShowFileChooser(it) }
                        return true
                    }
                }
                
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                    try {
                        if (downloadUrl.startsWith("data:")) {
                            // Extract Base64 from Data URI (e.g. data:application/json;base64,.....)
                            val base64Data = downloadUrl.substring(downloadUrl.indexOf(",") + 1)
                            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                            
                            var ext = ".json"
                            if (downloadUrl.startsWith("data:text/csv")) ext = ".csv"
                            
                            val fileName = "SIKAP_Export_${System.currentTimeMillis()}$ext"
                            val file = java.io.File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                            
                            java.io.FileOutputStream(file).use { it.write(bytes) }
                            Toast.makeText(ctx, "Berhasil! File $fileName disimpan di folder Downloads HP Anda.", Toast.LENGTH_LONG).show()
                        } else {
                            // Fallback to normal download manager for HTTP links
                            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                                setMimeType(mimetype)
                                addRequestHeader("cookie", CookieManager.getInstance().getCookie(downloadUrl))
                                addRequestHeader("User-Agent", userAgent)
                                setDescription("Mengunduh file SIKAP...")
                                setTitle("SIKAP_Export")
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "SIKAP_Export_${System.currentTimeMillis()}")
                            }
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            Toast.makeText(ctx, "Mengunduh file ke folder Downloads...", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(ctx, "Gagal mengunduh: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

