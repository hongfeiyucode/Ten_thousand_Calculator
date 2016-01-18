

package com.example.hfy;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

public class CalculatorExpressionBuilder extends SpannableStringBuilder {

    private final CalculatorExpressionTokenizer mTokenizer;
    private boolean mIsEdited;

    public CalculatorExpressionBuilder(
            CharSequence text, CalculatorExpressionTokenizer tokenizer, boolean isEdited) {
        super(text);

        mTokenizer = tokenizer;
        mIsEdited = isEdited;
    }

    @Override
    public SpannableStringBuilder replace(int start, int end, CharSequence tb, int tbstart,
            int tbend) {
        if (start != length() || end != length()) {
            mIsEdited = true;
            return super.replace(start, end, tb, tbstart, tbend);
        }

        String appendExpr =
                mTokenizer.getNormalizedExpression(tb.subSequence(tbstart, tbend).toString());
        if (appendExpr.length() == 1) {
            final String expr = mTokenizer.getNormalizedExpression(toString());
            switch (appendExpr.charAt(0)) {
                case '.':
                    // don't allow two decimals in the same number不允许.在同一个小数
                    final int index = expr.lastIndexOf('.');
                    if (index != -1 && TextUtils.isDigitsOnly(expr.substring(index + 1, start))) {
                        appendExpr = "";
                    }
                    break;
                case '+':
                case '*':
                case '/':
                    // don't allow leading operator不要让领先的操作符
                    if (start == 0) {
                        appendExpr = "";
                        break;
                    }

                    // don't allow multiple successive operators不允许多个连续的操作符
                    while (start > 0 && "+-*/".indexOf(expr.charAt(start - 1)) != -1) {
                        --start;
                    }
                    // fall through通过
                case '-':
                    // don't allow -- or +-
                    if (start > 0 && "+-".indexOf(expr.charAt(start - 1)) != -1) {
                        --start;
                    }

                    // mark as edited since operators can always be appended符号作为编辑，因为操作符可以随时附加
                    mIsEdited = true;
                    break;
                default:
                    break;
            }
        }

        // since this is the first edit replace the entire string因为这是第一次编辑替换整个字符串
        if (!mIsEdited && appendExpr.length() > 0) {
            start = 0;
            mIsEdited = true;
        }

        appendExpr = mTokenizer.getLocalizedExpression(appendExpr);
        return super.replace(start, end, appendExpr, 0, appendExpr.length());
    }
}
