package com.melnykov.fab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.ImageButton;

import com.melnykov.floatingactionbutton.R;

/**
 * Android Google+ like floating action button which reacts on the attached list view scrolling events.
 *
 * @author Oleksandr Melnykov
 */
public class FloatingActionButton extends ImageButton {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_MINI = 1;

    private final Interpolator mInterpolator = new AccelerateDecelerateInterpolator();

    private AbsListView mListView;

    private int mScrollY;
    private boolean mVisible;

    private int mColorNormal;
    private int mColorPressed;
    private boolean mShadow;
    private int mType;

    private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray ta = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FloatingActionButton, 0, 0);
        mVisible = true;
        mColorNormal = ta.getColor(R.styleable.FloatingActionButton_fab_colorNormal,
                getColor(R.color.holo_blue_dark));
        mColorPressed = ta.getColor(R.styleable.FloatingActionButton_fab_colorPressed,
                getColor(R.color.holo_blue_light));
        mShadow = ta.getBoolean(R.styleable.FloatingActionButton_fab_shadow, true);
        mType = ta.getInt(R.styleable.FloatingActionButton_type, TYPE_NORMAL);
        ta.recycle();

        updateBackground();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = getDimension(
                mType == TYPE_NORMAL ? R.dimen.fab_size_normal : R.dimen.fab_size_mini);
        if (mShadow) {
            int shadowSize = getDimension(R.dimen.fab_shadow_size);
            size += shadowSize * 2;
        }
        setMeasuredDimension(size, size);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mScrollY = mScrollY;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mScrollY = savedState.mScrollY;
            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    private void updateBackground() {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{android.R.attr.state_pressed}, createDrawable(mColorPressed));
        drawable.addState(new int[]{}, createDrawable(mColorNormal));
        setBackgroundCompat(drawable);
    }

    private Drawable createDrawable(int color) {
        Shape shape;
        if (isInEditMode()) {
            shape = new RectShape();
        } else {
            shape = new OvalShape();
        }
        ShapeDrawable shapeDrawable = new ShapeDrawable(shape);
        shapeDrawable.getPaint().setColor(color);

        if (mShadow) {
            LayerDrawable layerDrawable = new LayerDrawable(
                    new Drawable[]{getResources().getDrawable(R.drawable.shadow),
                            shapeDrawable});
            int shadowSize = getDimension(
                    mType == TYPE_NORMAL ? R.dimen.fab_shadow_size : R.dimen.fab_mini_shadow_size);
            layerDrawable.setLayerInset(1, shadowSize, shadowSize, shadowSize, shadowSize);
            return layerDrawable;
        } else {
            return shapeDrawable;
        }
    }

    private int getColor(int id) {
        return getResources().getColor(id);
    }

    private int getDimension(int id) {
        return getResources().getDimensionPixelSize(id);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void setBackgroundCompat(Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }

    private int getListViewScrollY() {
        View topChild = mListView.getChildAt(0);
        return topChild == null ? 0 : mListView.getFirstVisiblePosition() * topChild.getHeight() -
                topChild.getTop();
    }

    private class ScrollSettleHandler extends Handler {
        private static final int SETTLE_DELAY_MILLIS = 100;
        private static final int TRANSLATE_DURATION_MILLIS = 500;

        private int mSettledScrollY;

        public void onScroll(int scrollY) {
            if (mSettledScrollY != scrollY) {
                mSettledScrollY = scrollY;
                removeMessages(0);
                // Clear any pending messages and post delayed
                sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            animate()
                    .setInterpolator(mInterpolator)
                    .setDuration(TRANSLATE_DURATION_MILLIS)
                    .translationY(mSettledScrollY);
        }
    }

    public void setColorNormal(int color) {
        if (color != mColorNormal) {
            mColorNormal = color;
            updateBackground();
        }
    }

    public int getColorNormal() {
        return mColorNormal;
    }

    public void setColorPressed(int color) {
        if (color != mColorPressed) {
            mColorPressed = color;
            updateBackground();
        }
    }

    public void setShadow(boolean shadow) {
        if (shadow != mShadow) {
            mShadow = shadow;
            updateBackground();
        }
    }

    public boolean hasShadow() {
        return mShadow;
    }

    public int getColorPressed() {
        return mColorPressed;
    }

    public void attachToListView(AbsListView listView) {
        mListView = listView;
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                int newScrollY = getListViewScrollY();
                if (newScrollY == mScrollY) {
                    return;
                }

                if (newScrollY > mScrollY && mVisible) {
                    // Scrolling up
                    mVisible = false;
                    mScrollSettleHandler.onScroll(getTop());
                } else if (newScrollY < mScrollY && !mVisible) {
                    // Scrolling down
                    mVisible = true;
                    mScrollSettleHandler.onScroll(0);
                }
                mScrollY = newScrollY;
            }
        });
    }

    /**
     * A {@link android.os.Parcelable} representing the {@link com.melnykov.fab.FloatingActionButton}'s
     * state.
     */
    public static class SavedState extends BaseSavedState {

        private int mScrollY;

        public SavedState(Parcelable parcel) {
            super(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            mScrollY = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mScrollY);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
