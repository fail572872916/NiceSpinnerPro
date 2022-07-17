package com.sutoon.spinner.views;

import android.text.Spannable;
import android.text.SpannableString;

public class SimpleSpinnerTextFormatter implements SpinnerTextFormatter {

    @Override
    public Spannable format(Object item) {
        return new SpannableString(item.toString());
    }
}
