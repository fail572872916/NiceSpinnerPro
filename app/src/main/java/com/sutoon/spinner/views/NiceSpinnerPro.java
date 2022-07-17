package com.sutoon.spinner.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.sutoon.spinner.R;

import java.util.Arrays;
import java.util.List;

public class NiceSpinnerPro extends RelativeLayout {
    private static final int MAX_LEVEL = 10000;
    private static final int VERTICAL_OFFSET = 1;
    private static final String INSTANCE_STATE = "instance_state";
    private static final String SELECTED_INDEX = "selected_index";
    private static final String IS_POPUP_SHOWING = "is_popup_showing";
    private static final String IS_ARROW_HIDDEN = "is_arrow_hidden";
    private static final String ARROW_DRAWABLE_RES_ID = "arrow_drawable_res_id";
    private Drawable arrowDrawable;
    private ListPopupWindow popupWindow;
    private NiceSpinnerBaseAdapter adapter;

    private int selectedIndex;
    private AdapterView.OnItemClickListener onItemClickListener;
    private AdapterView.OnItemSelectedListener onItemSelectedListener;
    private OnSpinnerProItemSelectedListener onSpinnerProItemSelectedListener;

    private boolean isArrowHidden;
    private int textColor;
    private int backgroundSelector;
    private int arrowDrawableTint;
    private int displayHeight;
    private int parentVerticalOffset;
    private int dropDownListPaddingBottom;
    private @DrawableRes
    int arrowDrawableResId;
    private SpinnerTextFormatter spinnerTextFormatter = new SimpleSpinnerTextFormatter();
    private SpinnerTextFormatter selectedTextFormatter = new SimpleSpinnerTextFormatter();
    private PopUpTextAlignment horizontalAlignment;

    @Nullable
    private ObjectAnimator arrowAnimator = null;

    public NiceSpinnerPro(Context context) {
        this(context, null);
    }

    public NiceSpinnerPro(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NiceSpinnerPro(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putInt(SELECTED_INDEX, selectedIndex);
        bundle.putBoolean(IS_ARROW_HIDDEN, isArrowHidden);
        bundle.putInt(ARROW_DRAWABLE_RES_ID, arrowDrawableResId);
        if (popupWindow != null) {
            bundle.putBoolean(IS_POPUP_SHOWING, popupWindow.isShowing());
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable savedState) {
        if (savedState instanceof Bundle) {
            Bundle bundle = (Bundle) savedState;
            selectedIndex = bundle.getInt(SELECTED_INDEX);
            if (adapter != null) {
                setTextInternal(selectedTextFormatter.format(adapter.getItemInDataset(selectedIndex)).toString());
                adapter.setSelectedIndex(selectedIndex);
            }

            if (bundle.getBoolean(IS_POPUP_SHOWING)) {
                if (popupWindow != null) {
                    // Post the show request into the looper to avoid bad token exception
                    post(this::showDropDown);
                }
            }
            isArrowHidden = bundle.getBoolean(IS_ARROW_HIDDEN, false);
            arrowDrawableResId = bundle.getInt(ARROW_DRAWABLE_RES_ID);
            savedState = bundle.getParcelable(INSTANCE_STATE);
        }
        super.onRestoreInstanceState(savedState);
    }

    public void initView(Context context, AttributeSet attrs) {

        View inflater = LayoutInflater.from(getContext()).inflate(R.layout.dropsy_layout_drop_down, this, false);
        addView(inflater);

        Resources resources = getResources();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.NiceSpinner);
        int defaultPadding = resources.getDimensionPixelSize(R.dimen.one_and_a_half_grid_unit);

        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setPadding(resources.getDimensionPixelSize(R.dimen.three_grid_unit), defaultPadding, defaultPadding,
                defaultPadding);
        setClickable(true);
        backgroundSelector = typedArray.getResourceId(R.styleable.NiceSpinner_backgroundSelector, R.drawable.selector);
        setBackgroundResource(backgroundSelector);
        textColor = typedArray.getColor(R.styleable.NiceSpinner_textTint, getDefaultTextColor(context));

        popupWindow = new ListPopupWindow(context);
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            // The selected item is not displayed within the list, so when the selected position is equal to
            // the one of the currently selected item it gets shifted to the next item.
            if (position >= selectedIndex && position < adapter.getCount()) {
                position++;
            }
            selectedIndex = position;

            if (onSpinnerProItemSelectedListener != null) {
                onSpinnerProItemSelectedListener.onItemSelected(this, view, position, id);
            }

            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(parent, view, position, id);
            }

            if (onItemSelectedListener != null) {
                onItemSelectedListener.onItemSelected(parent, view, position, id);
            }

            adapter.setSelectedIndex(position);

            setTextInternal(adapter.getItemInDataset(position));

            dismissDropDown();
        });

        popupWindow.setModal(true);

        popupWindow.setOnDismissListener(() -> {
            if (!isArrowHidden) {
                animateArrow(false);
            }
        });

        isArrowHidden = typedArray.getBoolean(R.styleable.NiceSpinner_hideArrow, false);
        arrowDrawableTint = typedArray.getColor(R.styleable.NiceSpinner_arrowTint, getResources().getColor(android.R.color.black));
        arrowDrawableResId = typedArray.getResourceId(R.styleable.NiceSpinner_arrowDrawable, R.drawable.ic_baseline_arrow_drop_down_24);
        dropDownListPaddingBottom =
                typedArray.getDimensionPixelSize(R.styleable.NiceSpinner_dropDownListPaddingBottom, 0);
        horizontalAlignment = PopUpTextAlignment.fromId(
                typedArray.getInt(R.styleable.NiceSpinner_popupTextAlignment, PopUpTextAlignment.CENTER.ordinal())
        );
        CharSequence[] entries = typedArray.getTextArray(R.styleable.NiceSpinner_entries);
        if (entries != null) {
            attachDataSource(Arrays.asList(entries));
        }
        typedArray.recycle();

        measureDisplayHeight();

    }
    private void measureDisplayHeight() {
        displayHeight = getContext().getResources().getDisplayMetrics().heightPixels;
    }

    private void setTextInternal(Object item) {
        if (selectedTextFormatter != null) {
//            setText(selectedTextFormatter.format(item));
        } else {
//            setText(item.toString());
        }
    }


    private void animateArrow(boolean shouldRotateUp) {
        int start = shouldRotateUp ? 0 : MAX_LEVEL;
        int end = shouldRotateUp ? MAX_LEVEL : 0;
        arrowAnimator = ObjectAnimator.ofInt(arrowDrawable, "level", start, end);
        arrowAnimator.setInterpolator(new LinearOutSlowInInterpolator());
        arrowAnimator.start();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isEnabled() && event.getAction() == MotionEvent.ACTION_UP) {
            if (!popupWindow.isShowing() && adapter.getCount() > 0) {
                showDropDown();
            } else {
                dismissDropDown();
            }
        }
        return super.onTouchEvent(event);
    }

    private <T> void setAdapterInternal(NiceSpinnerBaseAdapter<T> adapter) {
        if (adapter.getCount() >= 0) {
            // 如果需要重新设置适配器，请确保也重置所选索引
            selectedIndex = 0;
            popupWindow.setAdapter(adapter);
            setTextInternal(adapter.getItemInDataset(selectedIndex));
        }
    }

    public <T> void attachDataSource(@NonNull List<T> list) {
        adapter = new NiceSpinnerAdapter<>(getContext(), list, textColor, backgroundSelector, spinnerTextFormatter, horizontalAlignment);
        setAdapterInternal(adapter);
    }

    public void setAdapter(ListAdapter adapter) {
        this.adapter = new NiceSpinnerAdapterWrapper(getContext(), adapter, textColor, backgroundSelector,
                spinnerTextFormatter, horizontalAlignment);
        setAdapterInternal(this.adapter);
    }


    public void showDropDown() {
        if (!isArrowHidden) {
            animateArrow(true);
        }
        popupWindow.setAnchorView(this);
        popupWindow.show();
        final ListView listView = popupWindow.getListView();
        if(listView != null) {
            listView.setVerticalScrollBarEnabled(false);
            listView.setHorizontalScrollBarEnabled(false);
            listView.setVerticalFadingEdgeEnabled(false);
            listView.setHorizontalFadingEdgeEnabled(false);
        }
    }
    public void dismissDropDown() {
        if (!isArrowHidden) {
            animateArrow(false);
        }
        popupWindow.dismiss();
    }



    private int getDefaultTextColor(Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme()
                .resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        TypedArray typedArray = context.obtainStyledAttributes(typedValue.data,
                new int[]{android.R.attr.textColorPrimary});
        int defaultTextColor = typedArray.getColor(0, Color.BLACK);
        typedArray.recycle();
        return defaultTextColor;
    }
}
