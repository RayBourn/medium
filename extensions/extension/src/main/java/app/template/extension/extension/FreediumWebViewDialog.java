package app.template.extension.extension;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class FreediumWebViewDialog extends Dialog {
    private final String url;

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public FreediumWebViewDialog(Context context, String url) {
        super(context, android.R.style.Theme_DeviceDefault_NoActionBar);
        this.url = url;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Context context = getContext();
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
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
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // Light status bar text
                    } else {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; // Dark status bar text
                    }
                    window.getDecorView().setSystemUiVisibility(flags);
                }
            }
        }

        // Use fitsSystemWindows to prevent drawing content underneath the status bar
        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootLayout.setFitsSystemWindows(true);

        // Main WebView
        WebView webView = new WebView(context);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        // Loading WebView
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

        // Safety timeout: automatically hide loader after 8 seconds so user is never stuck
        loaderWebView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (loaderWebView.getVisibility() == View.VISIBLE) {
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
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
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

        // Top controls bar: Close (left) and Open in Browser (right)
        LinearLayout topControls = new LinearLayout(context);
        topControls.setOrientation(LinearLayout.HORIZONTAL);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        topLp.gravity = Gravity.TOP;
        topLp.setMargins(dpToPx(context, 12), dpToPx(context, 10), dpToPx(context, 12), 0);
        topControls.setLayoutParams(topLp);

        // Close Button
        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕ Close");
        closeBtn.setTextSize(13);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setPadding(dpToPx(context, 14), dpToPx(context, 8), dpToPx(context, 14), dpToPx(context, 8));

        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.RECTANGLE);
        closeBg.setCornerRadius(dpToPx(context, 16));
        closeBg.setColor(Color.parseColor("#80000000")); // Semi-transparent black
        closeBtn.setBackground(closeBg);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        topControls.addView(closeBtn);

        // Spacer
        View spacer = new View(context);
        LinearLayout.LayoutParams spacerLp = new LinearLayout.LayoutParams(0, 1, 1.0f);
        spacer.setLayoutParams(spacerLp);
        topControls.addView(spacer);

        // Open in Browser Button
        TextView browserBtn = new TextView(context);
        browserBtn.setText("↗ Open in Browser");
        browserBtn.setTextSize(13);
        browserBtn.setTypeface(Typeface.DEFAULT_BOLD);
        browserBtn.setTextColor(Color.WHITE);
        browserBtn.setPadding(dpToPx(context, 14), dpToPx(context, 8), dpToPx(context, 14), dpToPx(context, 8));

        GradientDrawable browserBg = new GradientDrawable();
        browserBg.setShape(GradientDrawable.RECTANGLE);
        browserBg.setCornerRadius(dpToPx(context, 16));
        browserBg.setColor(Color.parseColor("#CC02B875")); // Medium Green with slight transparency
        browserBtn.setBackground(browserBg);
        browserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open browser: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        topControls.addView(browserBtn);

        rootLayout.addView(topControls);

        setContentView(rootLayout);
        webView.loadUrl(url);
    }
}
