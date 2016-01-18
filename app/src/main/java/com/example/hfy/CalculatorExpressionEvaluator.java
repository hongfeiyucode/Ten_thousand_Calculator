

package com.example.hfy;

import android.util.Log;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;
import org.javia.arity.Util;

import java.math.BigDecimal;

public class CalculatorExpressionEvaluator {

    private static final int MAX_DIGITS = 12;
    private static final int ROUNDING_DIGITS = 2;

    private final Symbols mSymbols;
    private final CalculatorExpressionTokenizer mTokenizer;
    public static String resultString="";
    //private final Expression mExpress=null;
    //Interpreter i = new Interpreter();

    public CalculatorExpressionEvaluator(CalculatorExpressionTokenizer tokenizer) {
        mSymbols = new Symbols();
        mTokenizer = tokenizer;
    }

    public void evaluate(CharSequence expr, EvaluateCallback callback) {
        evaluate(expr.toString(), callback);
    }

    public void evaluate(String expr, EvaluateCallback callback) {
        expr = mTokenizer.getNormalizedExpression(expr);

        // remove any trailing operators删除任何尾随操作符
        while (expr.length() > 0 && "+-/*".indexOf(expr.charAt(expr.length() - 1)) != -1) {
            expr = expr.substring(0, expr.length() - 1);
        }

        try {
            if (expr.length() == 0 || Double.valueOf(expr) != null) {
                callback.onEvaluate(expr, null, Calculator.INVALID_RES_ID);
                return;
            }
        } catch (NumberFormatException e) {
            // expr is not a simple number他不是一个简单的数字
        }

        try {
            Log.e("exprout", expr);
            Double DouResult = mSymbols.eval(expr);
            Log.e("exprout", "double结果为" + DouResult.toString());
            //BigDecimal result = new BigDecimal(mSymbols.eval(expr));
            //BigDecimal result = mExpress.calcuThisExpression(expr);
            //Object objResult = i.eval(expr);
            //TODO 警告！！！expr在这里还是计算式的String，还没有转换为单个数值
            // Double.isInfinite(x)判断无穷大
            if (DouResult.isNaN(DouResult)) {
                callback.onEvaluate(expr, null, R.string.error_nan);
                Log.e("exprout","DouResult isNaN");
            } else {
                // The arity library uses floating point arithmetic when evaluating the expression
                // leading to precision errors in the result. The method doubleToString hides these
                // errors; rounding the result by dropping N digits of precision.
                //数库使用浮点运算时，计算表达式
                //导致精度误差的结果。方法double to string隐藏这些
                //误差；通过对精度下降的数字计算。

                //String BigString = result.toString();
                String BigString;
                if(DouResult.isInfinite()){
                    BigString ="Infinity";
                }else if(expr.matches("[.\\d()\\+\\-\\*\\/]*")){
                    try{
                        Expression mExpress = new Expression(expr);
                        BigString = mExpress.calcuCurrentExpression();
                    }catch (Exception e) {
                        callback.onEvaluate(expr, null, R.string.error_syntax);
                        BigDecimal result = new BigDecimal(mSymbols.eval(expr));
                        BigString = result.toString();
                    }
                }else {
                    BigDecimal result = new BigDecimal(mSymbols.eval(expr));
                    BigString = result.toString();
                }
                Log.e("exprout","BigDecimal结果为"+BigString);
                resultString = mTokenizer.getLocalizedExpression(
                        //Util.DoubleToString(result, MAX_DIGITS, ROUNDING_DIGITS));
                        BigString);
                callback.onEvaluate(expr, resultString, Calculator.INVALID_RES_ID);
            }
        } catch (SyntaxException e) {
            callback.onEvaluate(expr, null, R.string.error_syntax);
        }
    }

    public interface EvaluateCallback {
        public void onEvaluate(String expr, String result, int errorResourceId);
    }
}
