package app.template.extension.extension;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class FreediumWebViewDialog extends Dialog {
    private final String url;
    private WebView webView;

    public FreediumWebViewDialog(Context context, String url) {
        super(context, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.url = url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Context context = getContext();
        final boolean isDarkMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        Window window = getWindow();
        if (window != null) {
            window.setWindowAnimations(android.R.style.Animation_InputMethod);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            
            // Set status bar colors and icon visibility dynamically
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(isDarkMode ? Color.BLACK : Color.WHITE);
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    int flags = window.getDecorView().getSystemUiVisibility();
                    if (isDarkMode) {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    } else {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    window.getDecorView().setSystemUiVisibility(flags);
                }
            }
        }

        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setFitsSystemWindows(true);
        rootLayout.setBackgroundColor(isDarkMode ? Color.BLACK : Color.WHITE);

        // Main WebView
        webView = new WebView(context);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        webView.setBackgroundColor(isDarkMode ? Color.BLACK : Color.WHITE);

        // Loading Overlay
        final WebView loaderWebView = new WebView(context);
        loaderWebView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        loaderWebView.setVerticalScrollBarEnabled(false);
        loaderWebView.setHorizontalScrollBarEnabled(false);

        String bgColor = isDarkMode ? "#000000" : "#FFFFFF";
        String textColor = isDarkMode ? "#F8FAFC" : "#0F172A";
        String spinnerColor = "#02B875";
        
        String html = "<html>" +
                "<head>" +
                "  <style>" +
                "    body {" +
                "      background-color: " + bgColor + ";" +
                "      display: flex;" +
                "      flex-direction: column;" +
                "      justify-content: center;" +
                "      align-items: center;" +
                "      height: 100vh;" +
                "      margin: 0;" +
                "      font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, sans-serif;" +
                "      color: " + textColor + ";" +
                "      user-select: none;" +
                "    }" +
                "    .spinner {" +
                "      width: 48px;" +
                "      height: 48px;" +
                "      border: 4px solid " + (isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)") + ";" +
                "      border-radius: 50%;" +
                "      border-top-color: " + spinnerColor + ";" +
                "      animation: spin 0.8s cubic-bezier(0.4, 0, 0.2, 1) infinite;" +
                "    }" +
                "    @keyframes spin {" +
                "      to { transform: rotate(360deg); }" +
                "    }" +
                "    .text {" +
                "      margin-top: 24px;" +
                "      font-size: 15px;" +
                "      font-weight: 500;" +
                "      letter-spacing: 0.03em;" +
                "      animation: pulse 1.5s infinite ease-in-out;" +
                "    }" +
                "    @keyframes pulse {" +
                "      0%, 100% { opacity: 0.5; }" +
                "      50% { opacity: 1; }" +
                "    }" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "  <div class=\"spinner\"></div>" +
                "  <div class=\"text\">Unlocking article...</div>" +
                "</body>" +
                "</html>";
                
        loaderWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);

        loaderWebView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loaderWebView.getVisibility() == View.VISIBLE) {
                    injectCleanStyles(webView, isDarkMode);
                    loaderWebView.setVisibility(View.GONE);
                }
            }
        }, 8000);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                super.onPageCommitVisible(view, url);
                injectCleanStyles(view, isDarkMode);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectCleanStyles(view, isDarkMode);
                loaderWebView.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                loaderWebView.setVisibility(View.GONE);
                Toast.makeText(context, "Mirror connection failed: " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request != null && request.isForMainFrame()) {
                    loaderWebView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                loaderWebView.setVisibility(View.GONE);
                Toast.makeText(context, "SSL certificate warning on mirror", Toast.LENGTH_SHORT).show();
                super.onReceivedSslError(view, handler, error);
            }
        });

        rootLayout.addView(webView);
        rootLayout.addView(loaderWebView);

        setContentView(rootLayout);
        webView.loadUrl(url);
    }

    private void injectCleanStyles(WebView view, boolean isDarkMode) {
        if (view == null) return;

        String bg = isDarkMode ? "#000000" : "#FFFFFF";
        String text = isDarkMode ? "#F8FAFC" : "#0F172A";

        String css =
                "div[role='alert'], .alert, [class*='alert'], [class*='banner'], [class*='toast'], " +
                "[class*='notification'], [class*='announcement'], a[href*='discord'], header, nav, footer { " +
                "  display: none !important; " +
                "} " +
                "html, body { " +
                "  margin: 0 !important; " +
                "  padding: 0 !important; " +
                "  width: 100% !important; " +
                "  background-color: " + bg + " !important; " +
                "  color: " + text + " !important; " +
                "} " +
                "main, article, .container, [class*='container'], [class*='card'], [class*='wrapper'], [class*='shadow'], div[class*='max-w-'] { " +
                "  max-width: 100% !important; " +
                "  width: 100% !important; " +
                "  margin: 0 auto !important; " +
                "  padding-left: 18px !important; " +
                "  padding-right: 18px !important; " +
                "  box-shadow: none !important; " +
                "  border: none !important; " +
                "  border-radius: 0 !important; " +
                "  background: transparent !important; " +
                "  background-color: transparent !important; " +
                "} " +
                "img, figure { max-width: 100% !important; height: auto !important; border-radius: 4px !important; }";

        String js =
                "(function() { " +
                "  var style = document.getElementById('freedium-clean-style'); " +
                "  if (!style) { " +
                "    style = document.createElement('style'); " +
                "    style.id = 'freedium-clean-style'; " +
                "    style.innerHTML = `" + css + "`; " +
                "    if (document.head) { document.head.appendChild(style); } " +
                "    else if (document.documentElement) { document.documentElement.appendChild(style); } " +
                "  } " +
                "  document.querySelectorAll('div, section, aside, [role=\"alert\"]').forEach(function(el) { " +
                "    var t = el.innerText || ''; " +
                "    if ((t.includes('new sources supported') || t.includes('Freedium has been updated') || t.includes('Discord')) && !el.querySelector('article')) { " +
                "      el.style.setProperty('display', 'none', 'important'); " +
                "    } " +
                "  }); " +
                "})();";

        view.evaluateJavascript(js, null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
