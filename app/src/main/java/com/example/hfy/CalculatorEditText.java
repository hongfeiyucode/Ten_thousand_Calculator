

package com.example.hfy;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Rect;
import android.os.Parcelable;
import android.text.method.ScrollingMovementMethod;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.TextView;

public class CalculatorEditText extends EditText {

    private final static ActionMode.Callback NO_SELECTION_ACTION_MODE_CALLBACK =
            new ActionMode.Callback() {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Prevents the selection action mode on double tap.防止双抽头选择动作模式。
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    private final float mMaximumTextSize;
    private final float mMinimumTextSize;
    private final float mStepTextSize;

    // Temporary objects for use in layout methods.布局方法中使用的临时对象。
    private final Paint mTempPaint = new TextPaint();
    private final Rect mTempRect = new Rect();

    private int mWidthConstraint = -1;
    private OnTextSizeChangeListener mOnTextSizeChangeListener;

    public CalculatorEditText(Context context) {
        this(context, null);
    }

    public CalculatorEditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CalculatorEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CalculatorEditText, defStyle, 0);
        mMaximumTextSize = a.getDimension(
                R.styleable.CalculatorEditText_maxTextSize, getTextSize());
        mMinimumTextSize = a.getDimension(
                R.styleable.CalculatorEditText_minTextSize, getTextSize());
        mStepTextSize = a.getDimension(R.styleable.CalculatorEditText_stepTextSize,
                (mMaximumTextSize - mMinimumTextSize) / 3);

        a.recycle();

        setCustomSelectionActionModeCallback(NO_SELECTION_ACTION_MODE_CALLBACK);
        if (isFocusable()) {
            setMovementMethod(ScrollingMovementMethod.getInstance());
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, mMaximumTextSize);
        setMinHeight(getLineHeight() + getCompoundPaddingBottom() + getCompoundPaddingTop());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // Hack to prevent keyboard and insertion handle from showing.破解以防止键盘和插入手柄显示。
            cancelLongPress();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidthConstraint =
                MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getVariableTextSize(getText().toString()));
    }

    @Override
    public Parcelable onSaveInstanceState() {
        super.onSaveInstanceState();

        // EditText will freeze any text with a selection regardless of getFreezesText() ->
        // return null to prevent any state from being preserved at the instance level.
        // 编辑将冻结任何文本与选择无论getFreezesText() ->
        // 为防止任何状态被保存在实例级而返回空。
        return null;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        final int textLength = text.length();
        if (getSelectionStart() != textLength || getSelectionEnd() != textLength) {
            // Pin the selection to the end of the current text.将选择引脚到当前文本的结尾。
            setSelection(textLength);
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getVariableTextSize(text.toString()));
    }

    @Override
    public void setTextSize(int unit, float size) {
        final float oldTextSize = getTextSize();
        super.setTextSize(unit, size);

        if (mOnTextSizeChangeListener != null && getTextSize() != oldTextSize) {
            mOnTextSizeChangeListener.onTextSizeChanged(this, oldTextSize);
        }
    }

    public void setOnTextSizeChangeListener(OnTextSizeChangeListener listener) {
        mOnTextSizeChangeListener = listener;
    }

    public float getVariableTextSize(String text) {
        if (mWidthConstraint < 0 || mMaximumTextSize <= mMinimumTextSize) {
            // Not measured, bail early.不测，早保释。
            return getTextSize();
        }

        // Capture current paint state.捕捉当前状态。
        mTempPaint.set(getPaint());

        // Step through increasing text sizes until the text would no longer fit.通过增加文本大小，直到文本不再适合。
        float lastFitTextSize = mMinimumTextSize;
        while (lastFitTextSize < mMaximumTextSize) {
            final float nextSize = Math.min(lastFitTextSize + mStepTextSize, mMaximumTextSize);
            mTempPaint.setTextSize(nextSize);
            if (mTempPaint.measureText(text) > mWidthConstraint) {
                break;
            } else {
                lastFitTextSize = nextSize;
            }
        }

        return lastFitTextSize;
    }

    @Override
    public int getCompoundPaddingTop() {
        // Measure the top padding from the capital letter height of the text instead of the top,
        // but don't remove more than the available top padding otherwise clipping may occur.
        //用大写字母的高度来衡量顶部填充，而不是顶部，
        //但不要删除更多的可用顶部填充，否则可能会出现裁剪。
        getPaint().getTextBounds("H", 0, 1, mTempRect);

        final FontMetricsInt fontMetrics = getPaint().getFontMetricsInt();
        final int paddingOffset = -(fontMetrics.ascent + mTempRect.height());
        return super.getCompoundPaddingTop() - Math.min(getPaddingTop(), paddingOffset);
    }

    @Override
    public int getCompoundPaddingBottom() {
        // Measure the bottom padding from the baseline of the text instead of the bottom, but don't
        // remove more than the available bottom padding otherwise clipping may occur.
        // 测量底部填充的文本，而不是底部，但不
        // 除可获得的底部填充物，否则可能发生剪切。
        final FontMetricsInt fontMetrics = getPaint().getFontMetricsInt();
        return super.getCompoundPaddingBottom() - Math.min(getPaddingBottom(), fontMetrics.descent);
    }

    public interface OnTextSizeChangeListener {
        void onTextSizeChanged(TextView textView, float oldSize);
    }
}
