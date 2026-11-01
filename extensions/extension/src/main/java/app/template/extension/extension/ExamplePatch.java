package app.template.extension.extension;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExamplePatch {

    private static final Pattern MEDIUM_URL_PATTERN = Pattern.compile(
            "https?://[a-zA-Z0-9.-]*(?:medium\\.com|towardsdatascience\\.com|betterprogramming\\.pub|uxdesign\\.cc)/[^\\s\"',;]+"
    );

    private static final Pattern ID_OR_SLUG_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]{8,64}$"
    );

    private static final Pattern TO_STRING_ID_PATTERN = Pattern.compile(
            "(?:postId|post_id|articleId|article_id|id)[=:]\\s*[\"']?([a-zA-Z0-9_-]{8,64})[\"']?"
    );

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static void onPostFragmentViewCreated(final Object fragment, final View root) {
        if (!(root instanceof ViewGroup)) return;
        final ViewGroup viewGroup = (ViewGroup) root;
        final Context context = root.getContext();

        // 1. Create a transparent full-screen overlay to guarantee correct layout alignment
        final FrameLayout overlay = new FrameLayout(context);
        overlay.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        overlay.setClickable(false);
        overlay.setFocusable(false);

        // 2. Create the pill-shaped button
        final FrameLayout button = new FrameLayout(context);
        FrameLayout.LayoutParams buttonLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLp.gravity = Gravity.BOTTOM | Gravity.END;
        buttonLp.setMargins(0, 0, dpToPx(context, 16), dpToPx(context, 76));
        button.setLayoutParams(buttonLp);
        button.setClickable(true);
        button.setFocusable(true);

        // Rounded rectangle background (pill shape)
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dpToPx(context, 20));
        background.setColor(Color.parseColor("#02B875")); // Medium Green
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            button.setElevation(dpToPx(context, 6));
            // Add a lighter green splash ripple for premium feedback
            android.content.res.ColorStateList rippleColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#33D191"));
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(rippleColor, background, null);
            button.setBackground(ripple);
        } else {
            button.setBackground(background);
        }

        // Add text "Unlock" inside the pill
        TextView textView = new TextView(context);
        textView.setText("Unlock");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(14);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        textView.setGravity(Gravity.CENTER);
        
        FrameLayout.LayoutParams textLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textLp.gravity = Gravity.CENTER;
        int padLr = dpToPx(context, 16);
        int padTb = dpToPx(context, 10);
        textView.setPadding(padLr, padTb, padLr, padTb);
        button.addView(textView, textLp);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String target = extractPostUrlOrId(fragment, context);
                    if (target != null && !target.isEmpty()) {
                        String articleUrl;
                        target = target.trim();
                        if (target.startsWith("http://") || target.startsWith("https://")) {
                            articleUrl = target;
                        } else if (target.startsWith("medium.com/") || target.startsWith("www.medium.com/")) {
                            articleUrl = "https://" + target;
                        } else if (target.startsWith("/")) {
                            articleUrl = "https://medium.com" + target;
                        } else {
                            articleUrl = "https://medium.com/p/" + target;
                        }

                        SharedPreferences prefs = context.getSharedPreferences("freedium_prefs", Context.MODE_PRIVATE);
                        String host = prefs.getString("freedium_host", "freedium-mirror.cfd");
                        String freediumUrl = "https://" + host + "/" + articleUrl;

                        Activity activity = findActivity(context);
                        if (activity == null && fragment != null) {
                            try {
                                Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                                Object actObj = getActivityMethod.invoke(fragment);
                                if (actObj instanceof Activity) {
                                    activity = (Activity) actObj;
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                        Context dialogContext = activity != null ? activity : context;

                        // Open the WebView dialog
                        FreediumWebViewDialog dialog = new FreediumWebViewDialog(dialogContext, freediumUrl);
                        dialog.show();
                    } else {
                        Toast.makeText(context, "Could not determine article ID", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error opening article: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                button.setVisibility(View.GONE);
                return true;
            }
        });

        // Add button to our overlay, then add overlay to the fragment's root layout
        overlay.addView(button);
        viewGroup.addView(overlay);

        overlay.bringToFront();
        button.bringToFront();
        root.post(new Runnable() {
            @Override
            public void run() {
                overlay.bringToFront();
                button.bringToFront();
            }
        });
    }

    private static String cleanStringCandidate(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;

        Matcher urlMatcher = MEDIUM_URL_PATTERN.matcher(s);
        if (urlMatcher.find()) {
            return urlMatcher.group(0);
        }

        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s;
        }

        if (s.startsWith("medium.com/") || s.startsWith("www.medium.com/")) {
            return "https://" + s;
        }

        if (s.startsWith("/") && s.length() > 2) {
            return "https://medium.com" + s;
        }

        if (ID_OR_SLUG_PATTERN.matcher(s).matches()) {
            if (!s.equalsIgnoreCase("settings") && !s.equalsIgnoreCase("null") &&
                !s.equalsIgnoreCase("undefined") && !s.equalsIgnoreCase("fragment") &&
                !s.equalsIgnoreCase("activity")) {
                return s;
            }
        }

        return null;
    }

    private static String extractIdFromPostObject(Object obj) {
        return extractIdFromPostObject(obj, 0);
    }

    private static String extractIdFromPostObject(Object obj, int depth) {
        if (obj == null || depth > 2) return null;

        if (obj instanceof String) {
            return cleanStringCandidate((String) obj);
        }

        if (obj instanceof Uri) {
            return cleanStringCandidate(obj.toString());
        }

        String[] methodNames = new String[]{
                "getPostId", "getId", "getUrl", "getCanonicalUrl", "getShareUrl",
                "getWebUrl", "getSlug", "getPost", "getArticle", "getStory",
                "getPostModel", "getPostMetadata", "getFeedItem"
        };

        for (String methodName : methodNames) {
            try {
                Method m = obj.getClass().getMethod(methodName);
                if (m.getParameterTypes().length == 0 && m.getReturnType() != void.class) {
                    Object res = m.invoke(obj);
                    if (res != null) {
                        if (res instanceof String) {
                            String cleaned = cleanStringCandidate((String) res);
                            if (cleaned != null) return cleaned;
                        } else {
                            String nested = extractIdFromPostObject(res, depth + 1);
                            if (nested != null) return nested;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        String[] fieldNames = new String[]{
                "postId", "id", "url", "canonicalUrl", "shareUrl", "webUrl", "slug", "post", "article"
        };
        for (String fieldName : fieldNames) {
            try {
                Field f = obj.getClass().getField(fieldName);
                Object val = f.get(obj);
                if (val != null) {
                    if (val instanceof String) {
                        String cleaned = cleanStringCandidate((String) val);
                        if (cleaned != null) return cleaned;
                    } else {
                        String nested = extractIdFromPostObject(val, depth + 1);
                        if (nested != null) return nested;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            String str = obj.toString();
            if (str != null && !str.isEmpty()) {
                Matcher m = MEDIUM_URL_PATTERN.matcher(str);
                if (m.find()) {
                    return m.group(0);
                }
                Matcher idMatcher = TO_STRING_ID_PATTERN.matcher(str);
                if (idMatcher.find()) {
                    String candidate = idMatcher.group(1);
                    if (candidate != null && !candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static String extractPostUrlOrId(Object fragment, Context context) {
        if (fragment == null) return null;

        // Tier 1: Check Fragment arguments (Bundle)
        try {
            Method getArgumentsMethod = fragment.getClass().getMethod("getArguments");
            Object argsObj = getArgumentsMethod.invoke(fragment);
            if (argsObj instanceof Bundle) {
                Bundle args = (Bundle) argsObj;
                String[] candidateKeys = new String[]{
                        "post_id", "postId", "id", "article_id", "articleId", "target_post_id",
                        "post", "url", "post_url", "canonical_url", "uri", "share_url", "web_url", "slug"
                };
                for (String key : candidateKeys) {
                    if (args.containsKey(key)) {
                        Object val = args.get(key);
                        String candidate = extractIdFromPostObject(val);
                        if (candidate != null) {
                            return candidate;
                        }
                    }
                }
                for (String key : args.keySet()) {
                    Object val = args.get(key);
                    String candidate = extractIdFromPostObject(val);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Tier 2: Check parent Activity Intent
        try {
            Activity activity = findActivity(context);
            if (activity == null) {
                try {
                    Method getActivityMethod = fragment.getClass().getMethod("getActivity");
                    Object actObj = getActivityMethod.invoke(fragment);
                    if (actObj instanceof Activity) {
                        activity = (Activity) actObj;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (activity != null) {
                Intent intent = activity.getIntent();
                if (intent != null) {
                    Uri data = intent.getData();
                    if (data != null) {
                        String cleaned = cleanStringCandidate(data.toString());
                        if (cleaned != null) return cleaned;
                    }
                    Bundle extras = intent.getExtras();
                    if (extras != null) {
                        String[] candidateKeys = new String[]{
                                "post_id", "postId", "id", "article_id", "articleId", "target_post_id",
                                "post", "url", "post_url", "canonical_url", "uri", "share_url", "web_url", "slug"
                        };
                        for (String key : candidateKeys) {
                            if (extras.containsKey(key)) {
                                Object val = extras.get(key);
                                String candidate = extractIdFromPostObject(val);
                                if (candidate != null) return candidate;
                            }
                        }
                        for (String key : extras.keySet()) {
                            Object val = extras.get(key);
                            String candidate = extractIdFromPostObject(val);
                            if (candidate != null) return candidate;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // Tier 3: Dynamic reflection fallback across fragment methods & fields (including superclasses)
        try {
            Class<?> curr = fragment.getClass();
            while (curr != null && curr != Object.class) {
                for (Method m : curr.getDeclaredMethods()) {
                    if (m.getParameterTypes().length == 0 && m.getReturnType() != void.class && !m.getReturnType().isPrimitive()) {
                        try {
                            m.setAccessible(true);
                            Object candidate = m.invoke(fragment);
                            if (candidate != null) {
                                String id = extractIdFromPostObject(candidate);
                                if (id != null) return id;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }

                for (Field f : curr.getDeclaredFields()) {
                    if (!f.getType().isPrimitive()) {
                        try {
                            f.setAccessible(true);
                            Object candidate = f.get(fragment);
                            if (candidate != null) {
                                String id = extractIdFromPostObject(candidate);
                                if (id != null) return id;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }

                curr = curr.getSuperclass();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static View wrapSettingsView(View composeView) {
        final Context context = composeView.getContext();
        
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Use same background color as settings screen
        rootLayout.setBackgroundColor(isDarkMode ? Color.BLACK : Color.WHITE);

        // Create the settings row layout
        RelativeLayout settingsRow = new RelativeLayout(context);
        settingsRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 72)
        ));
        settingsRow.setPadding(dpToPx(context, 16), 0, dpToPx(context, 16), 0);

        // Native selectable item background (ripple effect)
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta = context.obtainStyledAttributes(attrs);
        android.graphics.drawable.Drawable ripple = ta.getDrawable(0);
        ta.recycle();
        settingsRow.setBackground(ripple);

        // Title text
        TextView title = new TextView(context);
        title.setText("Freedium Mirror Server");
        title.setTextSize(16);
        title.setTextColor(isDarkMode ? Color.WHITE : Color.BLACK);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        
        RelativeLayout.LayoutParams titleLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        titleLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        titleLp.topMargin = dpToPx(context, 14);
        settingsRow.addView(title, titleLp);

        // Subtitle text (shows current host)
        final TextView subtitle = new TextView(context);
        final SharedPreferences prefs = context.getSharedPreferences("freedium_prefs", Context.MODE_PRIVATE);
        String currentHost = prefs.getString("freedium_host", "freedium-mirror.cfd");
        subtitle.setText("Current: " + currentHost);
        subtitle.setTextSize(13);
        subtitle.setTextColor(isDarkMode ? Color.parseColor("#94A3B8") : Color.parseColor("#64748B"));
        
        RelativeLayout.LayoutParams subtitleLp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        subtitleLp.bottomMargin = dpToPx(context, 14);
        settingsRow.addView(subtitle, subtitleLp);

        settingsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final java.util.ArrayList<String> optionsList = new java.util.ArrayList<String>();
                optionsList.add("freedium-mirror.cfd");
                optionsList.add("freedium.cfd");
                optionsList.add("freedium.net");
                
                String current = prefs.getString("freedium_host", "freedium-mirror.cfd");
                boolean isPreconfigured = false;
                for (String h : optionsList) {
                    if (h.equals(current)) {
                        isPreconfigured = true;
                        break;
                    }
                }
                
                if (!isPreconfigured) {
                    optionsList.add(current);
                }
                
                optionsList.add("Add custom host...");
                
                final String[] items = optionsList.toArray(new String[0]);
                int selectedIdx = -1;
                for (int i = 0; i < items.length - 1; i++) {
                    if (items[i].equals(current)) {
                        selectedIdx = i;
                        break;
                    }
                }
                
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
                builder.setTitle("Select Freedium Host");
                
                builder.setSingleChoiceItems(items, selectedIdx, new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        if (which == items.length - 1) {
                            dialog.dismiss();
                            showCustomHostInputDialog(context, prefs, subtitle);
                        } else {
                            String chosenHost = items[which];
                            prefs.edit().putString("freedium_host", chosenHost).apply();
                            subtitle.setText("Current: " + chosenHost);
                            dialog.dismiss();
                        }
                    }
                });
                
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }
        });

        // Add settings row
        rootLayout.addView(settingsRow);
        
        // Add a small divider matching theme divider color
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(context, 1)
        ));
        divider.setBackgroundColor(isDarkMode ? Color.parseColor("#1E293B") : Color.parseColor("#E2E8F0"));
        rootLayout.addView(divider);

        // Add compose view
        LinearLayout.LayoutParams composeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        rootLayout.addView(composeView, composeLp);

        return rootLayout;
    }

    private static void showCustomHostInputDialog(final Context context, final SharedPreferences prefs, final TextView subtitle) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Enter Custom Host");

        final android.widget.EditText input = new android.widget.EditText(context);
        input.setHint("e.g. custom-mirror.xyz");
        
        FrameLayout container = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = dpToPx(context, 20);
        params.rightMargin = dpToPx(context, 20);
        params.topMargin = dpToPx(context, 10);
        params.bottomMargin = dpToPx(context, 10);
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(android.content.DialogInterface dialog, int which) {
                String val = input.getText().toString().trim();
                if (val.startsWith("https://")) {
                    val = val.substring(8);
                } else if (val.startsWith("http://")) {
                    val = val.substring(7);
                }
                if (val.endsWith("/")) {
                    val = val.substring(0, val.length() - 1);
                }
                
                if (!val.isEmpty()) {
                    prefs.edit().putString("freedium_host", val).apply();
                    subtitle.setText("Current: " + val);
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
