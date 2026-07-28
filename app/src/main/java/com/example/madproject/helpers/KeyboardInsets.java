package com.example.madproject.helpers;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps a bottom-anchored view (a chat input row) above the on-screen keyboard.
 *
 * The app targets SDK 35+, where edge-to-edge is enforced and
 * {@code android:windowSoftInputMode="adjustResize"} no longer resizes the window - the keyboard
 * simply draws over the content. The IME inset has to be consumed manually instead.
 */
public final class KeyboardInsets {

    private KeyboardInsets() {
    }

    /**
     * Adds the keyboard's height to the view's bottom margin whenever the keyboard is open, and
     * the navigation-bar height when it is not, so the view sits directly above whichever is
     * showing.
     *
     * Margin rather than padding because the two chat inputs differ: one is a CardView whose
     * rounded content would be clipped by padding, the other a padded LinearLayout.
     *
     * Call once, from onCreate. The view's authored margin is captured at that moment, so calling
     * it twice on the same view would compound the offset.
     */
    public static void liftAboveKeyboard(View view) {
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }

        final int baseBottomMargin =
                ((ViewGroup.MarginLayoutParams) view.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            lp.bottomMargin = baseBottomMargin + Math.max(ime.bottom, navBars.bottom);
            v.setLayoutParams(lp);

            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
