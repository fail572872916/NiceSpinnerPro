package com.sutoon.spinner.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sutoon.spinner.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NiceSpinnerPro extends RelativeLayout {

    private static final String INSTANCE_STATE = "instance_state";
    private static final String SELECTED_INDEX = "selected_index";
    private static final String IS_POPUP_SHOWING = "is_popup_showing";
    private static final String IS_ARROW_HIDDEN = "is_arrow_hidden";
    private static final String ARROW_DRAWABLE_RES_ID = "arrow_drawable_res_id";
    private ListPopupWindow popupWindow;
    private NiceSpinnerBaseAdapter adapter;

    private int selectedIndex;
    private AdapterView.OnItemClickListener onItemClickListener;
    private AdapterView.OnItemSelectedListener onItemSelectedListener;
    private OnSpinnerProItemSelectedListener onSpinnerProItemSelectedListener;

    private boolean isArrowHidden;
    private boolean isShowDrawable;
    private boolean isShowColor;
    private int textColor;
    private int backgroundSelector;
    private int arrowDrawableTint;
    private int displayHeight;
    private int parentVerticalOffset;
    private int dropDownListPaddingBottom;
    private @DrawableRes
    int arrowDrawableResId;
    int spinnerShow;
    int spinnerColor;
    private SpinnerTextFormatter spinnerTextFormatter = new SimpleSpinnerTextFormatter();
    private SpinnerTextFormatter selectedTextFormatter = new SimpleSpinnerTextFormatter();
    private PopUpTextAlignment horizontalAlignment;

    String leftTextStr = "";
    String rightTextStr = "";
    String leftTextColor = "";
    String rightTextColor = "";

    int selectGravity = Gravity.LEFT;

    TextView leftText, rightText, centerText;
    ImageView imArrow, imgSpinner;

    List<Integer> res = new ArrayList<>();
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
        leftText = inflater.findViewById(R.id.tv_left_label);
        rightText = inflater.findViewById(R.id.tv_right_label);
        centerText = inflater.findViewById(R.id.tv_select_label);
        imArrow = inflater.findViewById(R.id.img_arrow);
        imgSpinner = inflater.findViewById(R.id.img_spinner);

        Resources resources = getResources();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.NiceSpinner);
        int defaultPadding = resources.getDimensionPixelSize(R.dimen.one_and_a_half_grid_unit);

        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
//        setPadding(resources.getDimensionPixelSize(R.dimen.three_grid_unit), defaultPadding, defaultPadding,
//                defaultPadding);
        setClickable(true);
        backgroundSelector = typedArray.getResourceId(R.styleable.NiceSpinner_backgroundSelector, R.drawable.selector);
//        setBackgroundResource(backgroundSelector);
        textColor = typedArray.getColor(R.styleable.NiceSpinner_textTint, getDefaultTextColor(context));


        popupWindow = new ListPopupWindow(context);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            // 选中的项目不会显示在列表中，因此当选中的位置等于当前选中的项目之一时，它会转移到下一个项目。
//            if (position >= selectedIndex && position < adapter.getCount()-1) {
//                position++;
//            }
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
            showRes(isShowDrawable, isShowColor, res);

            dismissDropDown();
        });

        popupWindow.setModal(true);

        popupWindow.setOnDismissListener(() -> {
            if (!isArrowHidden) {
                animateArrow(false);
            }
        });
        leftTextStr = typedArray.getString(R.styleable.NiceSpinner_leftHint);
        rightTextStr = typedArray.getString(R.styleable.NiceSpinner_rightHint);
        isArrowHidden = typedArray.getBoolean(R.styleable.NiceSpinner_hideArrow, false);
        isShowDrawable = typedArray.getBoolean(R.styleable.NiceSpinner_isShowDrawable, false);
        isShowColor = typedArray.getBoolean(R.styleable.NiceSpinner_isShowColor, false);
        arrowDrawableTint = typedArray.getColor(R.styleable.NiceSpinner_arrowTint, getResources().getColor(android.R.color.black));
        arrowDrawableResId = typedArray.getResourceId(R.styleable.NiceSpinner_arrowDrawable, R.drawable.ic_baseline_arrow_drop_down_24);

        spinnerShow = typedArray.getResourceId(R.styleable.NiceSpinner_arrowDrawable, R.drawable.ic_baseline_arrow_drop_down_24);
        spinnerColor = typedArray.getColor(R.styleable.NiceSpinner_showColor, getContext().getColor(R.color.white));

        dropDownListPaddingBottom =
                typedArray.getDimensionPixelSize(R.styleable.NiceSpinner_dropDownListPaddingBottom, 0);
        horizontalAlignment = PopUpTextAlignment.fromId(
                typedArray.getInt(R.styleable.NiceSpinner_popupTextAlignment, PopUpTextAlignment.CENTER.ordinal())
        );
        selectGravity =
                typedArray.getInt(R.styleable.NiceSpinner_showGravity, Gravity.LEFT);

        if (TextUtils.isEmpty(leftTextStr)) {

            leftText.setVisibility(View.GONE);
        } else {
            leftText.setVisibility(View.VISIBLE);
            leftText.setText(leftTextStr);
        }

        if (TextUtils.isEmpty(rightTextStr)) {

            rightText.setVisibility(View.GONE);
        } else {
            rightText.setVisibility(View.VISIBLE);
            rightText.setText(rightTextStr);
        }
        centerText.setGravity(selectGravity);

        if (isShowDrawable || isShowColor) {

            imgSpinner.setVisibility(View.VISIBLE);
            if (isShowDrawable) {
                imgSpinner.setImageResource(spinnerShow);
            }
            if (isShowColor) {
                imgSpinner.setBackgroundColor(spinnerColor);
            }

        } else {
            imgSpinner.setVisibility(View.GONE);
        }


        CharSequence[] entries = typedArray.getTextArray(R.styleable.NiceSpinner_entries);
        if (entries != null) {
            attachDataSource(Arrays.asList(entries), new ArrayList(), isShowDrawable, isShowColor);
        }
        typedArray.recycle();

        measureDisplayHeight();

    }

    /**
     *
     */
    private void showRes(boolean isShowDrawable, boolean isShowColor, List<Integer> integers) {
        if (integers.size() == 0) return;
        if (isShowDrawable || isShowColor) {

            imgSpinner.setVisibility(View.VISIBLE);
            if (isShowDrawable) {
                imgSpinner.setImageResource(integers.get(selectedIndex));
            }
            if (isShowColor) {
                imgSpinner.setBackgroundColor(getContext().getColor(integers.get(selectedIndex)));
            }

        } else {
            imgSpinner.setVisibility(View.GONE);
        }
    }

    private void measureDisplayHeight() {
        displayHeight = getContext().getResources().getDisplayMetrics().heightPixels;
    }

    private void setTextInternal(Object item) {
        if (selectedTextFormatter != null) {
            centerText.setText(selectedTextFormatter.format(item));
        } else {
            centerText.setText(item.toString());
        }
    }


    private void animateArrow(boolean shouldRotateUp) {

        if (shouldRotateUp) {
            imArrow.animate().rotation(180f).setDuration(200).start();
        } else {
            imArrow.animate().rotation(0f).setDuration(200).start();
        }

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

    public <T> void attachDataSource(@NonNull List<T> list, List<Integer> integers, Boolean isShowDrawable, Boolean isShowColor) {
        this.res = integers;
        this.isShowDrawable = isShowDrawable;
        this.isShowColor = isShowColor;
        showRes(isShowDrawable, isShowColor, res);
        adapter = new NiceSpinnerAdapter<>(getContext(), list, res, isShowDrawable, isShowColor, textColor, backgroundSelector, spinnerTextFormatter, horizontalAlignment);
        setAdapterInternal(adapter);
    }

//    public void setAdapter(ListAdapter adapter) {
//        this.adapter = new NiceSpinnerAdapterWrapper(getContext(), adapter, textColor, backgroundSelector,
//                spinnerTextFormatter, horizontalAlignment);
//        setAdapterInternal(this.adapter);
//    }


    public void showDropDown() {
        if (!isArrowHidden) {
            animateArrow(true);
        }
        popupWindow.setAnchorView(this);
        popupWindow.show();
        final ListView listView = popupWindow.getListView();
        if (listView != null) {
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
