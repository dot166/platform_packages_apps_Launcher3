package com.android.launcher3.j;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.launcher3.R;

public class Smartspace extends FrameLayout {

    public Smartspace(Context context) {
        this(context, null);
    }

    public Smartspace(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.smartspacer, this);
    }
}