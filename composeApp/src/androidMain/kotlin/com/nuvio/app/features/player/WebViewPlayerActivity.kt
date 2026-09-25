package com.nuvio.app.features.player

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.nuvio.app.R
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class WebViewPlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"

        fun newIntent(context: android.content.Context, url: String, title: String? = null): Intent {
            return Intent(context, WebViewPlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
        }
    }

    private var webView: WebView? = null

    // Matches <iframe ...sandbox="..." and removes the sandbox attribute from the opening tag
    // Uses non-greedy [^>]*? to stop at the first > of the opening tag
    private val sandboxPattern = Pattern.compile(
        "(<iframe(?:\\s+[^>]*)?)\\s+sandbox\\s*=\\s*[\"']?[^>]*?>",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_TITLE) ?: "Playing"

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                setSupportMultipleWindows(true)
                setGeolocationEnabled(false)
                javaScriptCanOpenWindowsAutomatically = true
                setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW)
                setUseWideViewPort(true)
                setLoadWithOverviewMode(true)
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                setAllowUniversalAccessFromFileURLs(true)
                setAllowFileAccessFromFileURLs(true)
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    setContentView(view)
                }

                override fun onHideCustomView() {
                    setContentView(R.layout.activity_web_view_player)
                }

                override fun onPermissionRequest(request: android.webkit.PermissionRequest) {
                    request.grant(request.resources.toList().toTypedArray())
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    return false
                }

                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return false
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                    val response = super.shouldInterceptRequest(view, request)
                    return stripSandboxIfNeeded(response, request.url.toString())
                }

                @Suppress("DEPRECATION")
                override fun shouldInterceptRequest(view: WebView?, url: String): WebResourceResponse? {
                    val response = super.shouldInterceptRequest(view, url)
                    return stripSandboxIfNeeded(response, url)
                }

                private fun stripSandboxIfNeeded(
                    response: WebResourceResponse?,
                    sourceUrl: String
                ): WebResourceResponse? {
                    if (response == null || response.data == null) return response
                    val mime = response.mimeType ?: ""
                    if (!mime.contains("html") && !mime.contains("text")) return response
                    return try {
                        val rawHtml = String(response.data.readBytes(), StandardCharsets.UTF_8)
                        val modified = sandboxPattern.matcher(rawHtml).replaceAll("$1>")
                        if (modified != rawHtml) {
                            val bytes = modified.toByteArray(StandardCharsets.UTF_8)
                            WebResourceResponse(mime, response.encoding, ByteArrayInputStream(bytes))
                        } else {
                            response
                        }
                    } catch (e: Exception) {
                        response
                    }
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    view?.evaluateJavascript(
                        """(function() {
                            var iframes = document.getElementsByTagName('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                iframes[i].removeAttribute('sandbox');
                            }
                            if (typeof window.__sandboxObserver !== 'undefined') {
                                window.__sandboxObserver.disconnect();
                            }
                            var observer = new MutationObserver(function(mutations) {
                                for (var m = 0; m < mutations.length; m++) {
                                    var added = mutations[m].addedNodes;
                                    for (var n = 0; n < added.length; n++) {
                                        if (added[n].nodeType === 1) {
                                            var newIframes = added[n].getElementsByTagName('iframe');
                                            for (var f = 0; f < newIframes.length; f++) {
                                                newIframes[f].removeAttribute('sandbox');
                                            }
                                        }
                                    }
                                }
                            });
                            window.__sandboxObserver = observer;
                            observer.observe(document.body, { childList: true, subtree: true });
                        })();""",
                        null
                    )
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    view?.evaluateJavascript(
                        """(function() {
                            var iframes = document.getElementsByTagName('iframe');
                            for (var i = 0; i < iframes.length; i++) {
                                iframes[i].removeAttribute('sandbox');
                            }
                        })();""",
                        null
                    )
                }
            }

            val startUrl = intent.getStringExtra(EXTRA_URL) ?: "about:blank"
            loadUrl(startUrl)
        }

        setContentView(webView)
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
