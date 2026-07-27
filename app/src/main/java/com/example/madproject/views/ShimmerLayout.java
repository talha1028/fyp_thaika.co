package com.example.madproject.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * Sweeps a soft highlight across its children to indicate loading.
 *
 * Used with placeholder bars (see {@code @drawable/bg_skeleton}) that sit behind empty
 * TextViews until real data arrives, at which point {@link #hideShimmer()} stops the
 * animation and the view draws normally.
 *
 * This replaces com.facebook.shimmer:shimmer-android, which only ever published to
 * JCenter and is no longer resolvable from any active repository.
 */
public class ShimmerLayout extends FrameLayout {

    private static final long SWEEP_MS = 1200L;
    private static final float BAND = 0.45f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private float progress;
    private boolean shimmering = true;

    public ShimmerLayout(Context context) {
        this(context, null);
    }

    public ShimmerLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShimmerLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimator();
        super.onDetachedFromWindow();
    }

    /** Begin (or resume) the sweep. No-op once {@link #hideShimmer()} has been called. */
    public void startShimmer() {
        if (!shimmering || animator != null) {
            return;
        }
        animator = ValueAnimator.ofFloat(-BAND, 1f + BAND);
        animator.setDuration(SWEEP_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    /** Stop the sweep permanently and draw children as-is. Safe to call more than once. */
    public void hideShimmer() {
        shimmering = false;
        stopAnimator();
        invalidate();
    }

    private void stopAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (!shimmering || w == 0 || h == 0) {
            return;
        }
        float centre = progress * w;
        float band = w * BAND;
        paint.setShader(new LinearGradient(
                centre - band, 0f, centre + band, 0f,
                new int[]{0x00FFFFFF, 0x80FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, w, h, paint);
    }
}
