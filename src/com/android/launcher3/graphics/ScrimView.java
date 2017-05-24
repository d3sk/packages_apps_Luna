/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.graphics.ColorUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;

public class ScrimView extends View implements DeviceProfile.LauncherLayoutChangeListener {

    private static final boolean DEBUG = false;

    private static final int MASK_HEIGHT_DP = 300;
    private static final float MASK_START_LENGTH_FACTOR = 1f;
    private static final float FINAL_ALPHA = 0.87f;
    private static final int SCRIM_COLOR = ColorUtils.setAlphaComponent(
            Color.WHITE, (int) (FINAL_ALPHA * 255));
    private static final boolean APPLY_ALPHA = true;

    private static Bitmap sFinalScrimMask;
    private static Bitmap sAlphaScrimMask;

    private final int mMaskHeight;
    private int mVisibleHeight;
    private final int mHeadStart;

    private final RectF mAlphaMaskRect = new RectF();
    private final RectF mFinalMaskRect = new RectF();
    private final Paint mPaint = new Paint();
    private float mProgress;
    private final Interpolator mAccelerator = new AccelerateInterpolator();
    private final Paint mDebugPaint = DEBUG ? new Paint() : null;

    private int mPaddingLeft;
    private int mPaddingRight;
    private int mAlphaStart;

    public ScrimView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mMaskHeight = Utilities.pxFromDp(MASK_HEIGHT_DP, getResources().getDisplayMetrics());
        mHeadStart = (int) (mMaskHeight * MASK_START_LENGTH_FACTOR);
        mPaint.setColor(SCRIM_COLOR);

        if (sFinalScrimMask == null) {
            sFinalScrimMask = Utilities.convertToAlphaMask(
                    Utilities.createOnePixBitmap(), FINAL_ALPHA);
        }
        if (sAlphaScrimMask == null) {
            Bitmap alphaMaskFromResource = BitmapFactory.decodeResource(getResources(),
                    R.drawable.all_apps_alpha_mask);
            sAlphaScrimMask = Utilities.convertToAlphaMask(alphaMaskFromResource, FINAL_ALPHA);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updatePaddingAndAlphaStart();
        Launcher.getLauncher(getContext()).getDeviceProfile()
                .addLauncherLayoutChangedListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Launcher.getLauncher(getContext()).getDeviceProfile()
                .removeLauncherLayoutChangedListener(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        mVisibleHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, mVisibleHeight * 2);
        setProgress(mProgress);
    }

    public void setProgress(float progress) {
        mProgress = progress;
        float initialY = mVisibleHeight - mHeadStart;
        float fullTranslationY = mVisibleHeight;
        float linTranslationY = initialY - progress * fullTranslationY;
        setTranslationY(linTranslationY);

        if (APPLY_ALPHA) {
            int alpha = mAlphaStart + (int) ((255f - mAlphaStart)
                    * mAccelerator.getInterpolation(progress));
            mPaint.setAlpha(alpha);
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mAlphaMaskRect.set(mPaddingLeft, 0, getWidth() - mPaddingRight, mMaskHeight);
        mFinalMaskRect.set(mPaddingLeft, mMaskHeight, getWidth() - mPaddingRight, getHeight());
        canvas.drawBitmap(sAlphaScrimMask, null, mAlphaMaskRect, mPaint);
        canvas.drawBitmap(sFinalScrimMask, null, mFinalMaskRect, mPaint);

        if (DEBUG) {
            mDebugPaint.setColor(0xFF0000FF);
            canvas.drawLine(0, mAlphaMaskRect.top, getWidth(), mAlphaMaskRect.top, mDebugPaint);
            canvas.drawLine(0, mAlphaMaskRect.bottom, getWidth(), mAlphaMaskRect.bottom, mDebugPaint);
        }
    }

    @Override
    public void onLauncherLayoutChanged() {
        updatePaddingAndAlphaStart();
        invalidate();
    }

    private void updatePaddingAndAlphaStart() {
        DeviceProfile grid = Launcher.getLauncher(getContext()).getDeviceProfile();
        if (grid.isVerticalBarLayout()) {
            int[] padding = grid.getContainerPadding();
            mPaddingLeft = padding[0] + grid.edgeMarginPx;
            mPaddingRight = padding[1] + grid.edgeMarginPx;
            mAlphaStart = 0;
        } else {
            mPaddingLeft = 0;
            mPaddingRight = 0;
            mAlphaStart = 55;
        }
    }
}
