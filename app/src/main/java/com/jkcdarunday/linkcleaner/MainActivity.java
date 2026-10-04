package com.jkcdarunday.linkcleaner;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TextView statusView;
    private ProgressBar progressView;
    private Button shareButton;
    private String cleanUrl;
    private int requestGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onDestroy() {
        requestGeneration++;
        executor.shutdownNow();
        super.onDestroy();
    }

    private View createContentView() {
        int padding = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        root.addView(scrollView, scrollParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        scrollView.addView(content, matchWidthWrapHeight());

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title, matchWidthWrapHeight());

        TextView instructions = new TextView(this);
        instructions.setText(R.string.instructions);
        instructions.setTextSize(16);
        LinearLayout.LayoutParams instructionsParams = matchWidthWrapHeight();
        instructionsParams.setMargins(0, dp(16), 0, dp(24));
        content.addView(instructions, instructionsParams);

        progressView = new ProgressBar(this);
        progressView.setVisibility(View.GONE);
        content.addView(progressView);

        statusView = new TextView(this);
        statusView.setTextSize(16);
        statusView.setTextIsSelectable(true);
        statusView.setMovementMethod(LinkMovementMethod.getInstance());
        LinearLayout.LayoutParams statusParams = matchWidthWrapHeight();
        statusParams.setMargins(0, dp(16), 0, dp(24));
        content.addView(statusView, statusParams);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = matchWidthWrapHeight();
        actionsParams.setMargins(0, dp(16), 0, 0);
        root.addView(actions, actionsParams);

        Button closeButton = new Button(this);
        closeButton.setText(R.string.close);
        closeButton.setOnClickListener(view -> finish());
        LinearLayout.LayoutParams closeButtonParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        closeButtonParams.setMarginEnd(dp(8));
        actions.addView(closeButton, closeButtonParams);

        shareButton = new Button(this);
        shareButton.setText(R.string.share_clean_link);
        shareButton.setVisibility(View.GONE);
        shareButton.setOnClickListener(view -> shareCleanLink());
        actions.addView(
                shareButton,
                new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return root;
    }

    private void handleIntent(Intent intent) {
        if (!Intent.ACTION_SEND.equals(intent.getAction())) {
            showIdle();
            return;
        }

        CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        String url = LinkCleaner.extractUrl(sharedText == null ? null : sharedText.toString());
        if (url == null) {
            showError("No HTTP or HTTPS link was found in the shared text.");
            return;
        }

        cleanUrl = null;
        shareButton.setVisibility(View.GONE);
        progressView.setVisibility(View.VISIBLE);
        statusView.setText(R.string.resolving);
        int generation = ++requestGeneration;
        executor.execute(() -> resolve(url, generation));
    }

    private void resolve(String url, int generation) {
        try {
            String cleanUrl = LinkCleaner.resolveAndClean(url);
            runOnUiThread(() -> {
                if (generation == requestGeneration) {
                    copyAndShow(cleanUrl);
                }
            });
        } catch (IOException | RuntimeException exception) {
            String message = exception.getMessage();
            if (message == null || message.trim().isEmpty()) {
                message = "The link could not be resolved.";
            }
            String finalMessage = message;
            runOnUiThread(() -> {
                if (generation == requestGeneration) {
                    showError(finalMessage);
                }
            });
        }
    }

    private void copyAndShow(String cleanUrl) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(
                ClipData.newPlainText(getString(R.string.clipboard_label), cleanUrl));
        this.cleanUrl = cleanUrl;
        progressView.setVisibility(View.GONE);
        statusView.setText(cleanUrl);
        shareButton.setVisibility(View.VISIBLE);
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    private void showIdle() {
        cleanUrl = null;
        shareButton.setVisibility(View.GONE);
        progressView.setVisibility(View.GONE);
        statusView.setText("");
    }

    private void showError(String message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        cleanUrl = null;
        shareButton.setVisibility(View.GONE);
        progressView.setVisibility(View.GONE);
        statusView.setText("Could not clean link: " + message);
    }

    private void shareCleanLink() {
        if (cleanUrl == null) {
            return;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, cleanUrl);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_clean_link)));
        finish();
    }

    private LinearLayout.LayoutParams matchWidthWrapHeight() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
