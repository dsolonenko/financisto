package ru.orangesoftware.financisto.view;

import java.util.List;

import ru.orangesoftware.financisto.R;
import ru.orangesoftware.financisto.graph.Report2DPoint;
import ru.orangesoftware.financisto.model.Currency;
import ru.orangesoftware.financisto.model.PeriodValue;
import ru.orangesoftware.financisto.utils.MyPreferences;
import ru.orangesoftware.financisto.utils.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Report 2D chart view. View to draw dynamic 2D reports.
 *
 * @author Abdsandryk
 */
public class Report2DChartView extends View {

    // background and axis elements
    private ShapeDrawable background;
    private ShapeDrawable[] axis;
    private ShapeDrawable[] pointsShapes;

    // List of points to be drawn
    List<Report2DPoint> points;

    // points to represent as references in the chart
    private Report2DPoint meanPoint;
    private Report2DPoint zeroPoint;

    // statistics data
    double max, min, absMin, absMax = 0;
    double meanNonNull = 0;

    // reference currency
    private Currency currency;

    // Paints
    private Paint labelPaint;
    private Paint gridPaint;
    private Paint currencyPaint;
    private Paint amountPaint;
    private Paint pathPaint;
    private Paint valuesPaint;

    /*
     * True if all the points are positive or negative. In this case, the 
     * chart will reflect all data in modulus.
     * False if there are positive and negative points to be represented.
     */
    private boolean absoluteCalculation = true;

    // Colors
    private int bgColor = 0xFF010101;
    private int bgChartColor = Color.BLACK;
    private int axisColor = 0xFFDEDEDE;
    private int pathColor = Color.YELLOW;
    private int txtColor = 0xFFBBBBBB;
    private int pointColor = Color.YELLOW;
    private int selectedPointPosColor = Color.GREEN;
    private int selectedPointNegColor = Color.RED;
    private int gridColor = 0xFF222222;
    public static final int meanColor = 0xFF206DED;

    // flag to indicate if the view was initialized
    private boolean initialized = false;

    // graphics configuration
    private int padding;
    private int graphPadding;
    private int txtHeight;
    private int selected = -1;

    private int textSize9;
    private int textSize10;
    private int textSize12;

    // space to draw labels vertically
    private int xSpace;
    // space to draw labels and information bellow the chart
    private int ySpace;

    public Report2DChartView(Context context) {
        super(context);
        init();
    }

    public Report2DChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Report2DChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {

        textSize9 = getResources().getDimensionPixelSize(R.dimen.chart_label_text_size_9);
        textSize10 = getResources().getDimensionPixelSize(R.dimen.chart_label_text_size_10);
        textSize12 = getResources().getDimensionPixelSize(R.dimen.chart_label_text_size_12);
        xSpace = getResources().getDimensionPixelSize(R.dimen.chart_label_x_space);
        ySpace = getResources().getDimensionPixelSize(R.dimen.chart_label_y_space);

        graphPadding = textSize10;
        padding = textSize10;
        txtHeight = textSize12;

        labelPaint = new Paint();
        labelPaint.setColor(txtColor);
        labelPaint.setTextSize(textSize10);
        labelPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextAlign(Align.CENTER);
        labelPaint.setAntiAlias(true);

        currencyPaint = new Paint();
        currencyPaint.setAntiAlias(true);
        currencyPaint.setColor(txtColor);
        currencyPaint.setTextAlign(Align.CENTER);
        currencyPaint.setTextSize(textSize12);

        amountPaint = new Paint();
        amountPaint.setAntiAlias(true);
        amountPaint.setColor(txtColor);
        amountPaint.setTextAlign(Align.LEFT);
        amountPaint.setTextSize(textSize12);

        valuesPaint = new Paint();
        valuesPaint.setAntiAlias(true);
        valuesPaint.setTextAlign(Align.CENTER);
        valuesPaint.setTextSize(textSize12);

        pathPaint = new Paint();
        pathPaint.setColor(pathColor);
        pathPaint.setStrokeWidth(0);
        pathPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(gridColor);

        // plot chart background
        background = new ShapeDrawable(new RectShape());
        background = new ShapeDrawable(new RectShape());
        background.getPaint().setColor(bgColor);

        axis = new ShapeDrawable[3];
        // 0 = background
        axis[0] = new ShapeDrawable(new RectShape());
        axis[0].getPaint().setColor(axisColor);
        axis[1] = new ShapeDrawable(new RectShape());
        axis[1].getPaint().setColor(bgChartColor);

    }

    /**
     * Refresh the view. Call onDraw(canvas).
     */
    public void refresh() {
        // call onDraw to refresh view chart
        this.invalidate();
    }

    /**
     * Set data to plot.
     *
     * @param points      Points representing the data to plot.
     * @param max         Maximum value.
     * @param min         Minimum value.
     * @param absMax      Maximum value in modulus.
     * @param absMin      Minimum value in modulus.
     * @param meanNonNull Mean value excluding null values.
     */
    public void setDataToPlot(List<Report2DPoint> points, double max, double min,
                              double absMax, double absMin,
                              double meanNonNull) {
        this.points = points;
        this.max = max;
        this.min = min;
        this.absMax = absMax;
        this.absMin = absMin;
        this.meanNonNull = meanNonNull;
        selected = -1;
        // decide if the path will be represented in modulus or not.
        // all points negative or positive
        // negative and positive points will be represented.
        absoluteCalculation = (min * max >= 0);

        // call onDraw to refresh view chart
        this.invalidate();
    }

    /**
     * Called to refresh data in chart.
     */
    protected void onDraw(Canvas canvas) {

        if (points != null) {
            calculatePointsPosition();
        }
        if (initialized) {
            drawChartAxis(canvas);
            drawPath(canvas);
            drawPoints(canvas);
            if (selected >= 0) {
                drawSelectedPoint(canvas);
            }
            drawLabels(canvas);
        }

    }

    /**
     * Draw the background and the grid.
     *
     * @param canvas Canvas to draw the chart.
     */
    private void drawChartAxis(Canvas canvas) {
        int w = this.getWidth();
        int h = this.getHeight();
        background.setBounds(0, 0, w, h);

        axis[0].setBounds(xSpace + padding, padding, w - padding, h - padding - ySpace);
        axis[1].setBounds(xSpace + padding + 1, padding, w - padding, h - padding - ySpace - 1);

        background.draw(canvas);
        axis[0].draw(canvas);
        axis[1].draw(canvas);

        if (points != null && points.size() > 0) {
            // draw grid lines
            for (int i = 1; i < points.size(); i++) {
                canvas.drawLine(points.get(i).getX(), padding, points.get(i).getX(), getHeight() - padding - ySpace - 1, gridPaint);
            }

            // draw month labels
            if (points.size() <= 12) {
                for (int i = 0; i < points.size(); i++) {
                    labelPaint.setTextSize(textSize10);
                    canvas.drawText(points.get(i).getMonthShortString(this.getContext()), points.get(i).getX(), getHeight() - ySpace - padding + txtHeight, labelPaint);
                    labelPaint.setTextSize(textSize9);
                    canvas.drawText(points.get(i).getYearString(), points.get(i).getX(), getHeight() - ySpace - padding + 2 * txtHeight - 1, labelPaint);
                }
            } else {
                labelPaint.setTextSize(textSize10);
                canvas.drawText(points.get(0).getMonthShortString(this.getContext()), points.get(0).getX(), getHeight() - ySpace - padding + txtHeight, labelPaint);
                canvas.drawText(points.get(points.size() - 1).getMonthShortString(this.getContext()), points.get(points.size() - 1).getX(), getHeight() - ySpace - padding + txtHeight, labelPaint);

                labelPaint.setTextSize(textSize9);
                canvas.drawText(points.get(0).getYearString(), points.get(0).getX(), getHeight() - ySpace - padding + 2 * txtHeight - 1, labelPaint);
                canvas.drawText(points.get(points.size() - 1).getYearString(), points.get(points.size() - 1).getX(), getHeight() - ySpace - padding + 2 * txtHeight - 1, labelPaint);

                labelPaint.setTextSize(textSize12);
                canvas.drawText(getResources().getString(R.string.period), padding + xSpace + (getWidth() - xSpace - 2 * padding) / 2, getHeight() - ySpace - padding / 2 + txtHeight, labelPaint);
            }
        }
        canvas.drawLine(padding + xSpace + 1, padding, getWidth() - padding, padding, gridPaint);

        // draw mean
        gridPaint.setColor(meanColor);
        canvas.drawLine(padding + xSpace + 1, meanPoint.getY(), getWidth() - padding, meanPoint.getY(), gridPaint);
        gridPaint.setColor(gridColor);

        if (zeroPoint != null) {
            canvas.drawLine(padding + xSpace + 1, zeroPoint.getY(), getWidth() - padding, zeroPoint.getY(), gridPaint);
        }
    }

    /**
     * Draw axis labels.
     *
     * @param canvas Canvas to draw the chart.
     */
    private void drawLabels(Canvas canvas) {

        Rect currencyBounds = new Rect();
        currencyPaint.getTextBounds(currency.symbol, 0, currency.symbol.length(), currencyBounds);

        canvas.drawText(currency.symbol, padding + xSpace - currencyBounds.width() - 5, padding + currencyBounds.height(), currencyPaint);

        // Draw point coordinates
        currencyPaint.setTextAlign(Align.LEFT);
        currencyPaint.setTextSize(textSize12);

        canvas.drawText("x:", padding, getHeight() - padding, currencyPaint);
        canvas.drawText("y:", padding + getWidth() / 2, getHeight() - padding, currencyPaint);

        // set desired drawing location
        int amountX = 0;
        int amountY = 0;

        String amount = getResources().getString(R.string.amount);

        // draw bounding rectangle before rotating text
        Rect amountBounds = new Rect();

        amountPaint.getTextBounds(amount, 0, amount.length(), amountBounds);

        if (amountBounds.width() > 0) {
            amountY = amountBounds.width() + (getHeight() - ySpace - amountBounds.width()) / 2;
        }

        if (amountBounds.height() > 0) {
            amountX = amountBounds.height() + (padding + xSpace - amountBounds.height()) / 2;
        }

        canvas.save();
        try {
            // rotate the canvas on center of the text to draw
            canvas.rotate(-90, amountX, amountY);
            // draw the rotated text
            canvas.drawText(amount, amountX, amountY, amountPaint);
        } finally {
            // undo the rotate
            canvas.restore();
        }
    }

    /**
     * Draw evolution path.
     *
     * @param canvas Canvas to draw the chart.
     */
    private void drawPath(Canvas canvas) {

        for (int i = 0; i < points.size() - 1; i++) {
            canvas.drawLine(points.get(i).getX(), points.get(i).getY(),
                    points.get(i + 1).getX(), points.get(i + 1).getY(), pathPaint);
        }

    }

    /**
     * Draw points in chart.
     *
     * @param canvas Canvas to draw the chart.
     */
    private void drawPoints(Canvas canvas) {
        for (ShapeDrawable pointsShape : pointsShapes) {
            pointsShape.draw(canvas);
        }
    }

    /**
     * Highlight selected point.
     *
     * @param canvas Canvas to draw the chart.
     */
    private void drawSelectedPoint(Canvas canvas) {
        if (points.get(selected).isNegative()) {
            valuesPaint.setColor(selectedPointNegColor);
        } else {
            valuesPaint.setColor(selectedPointPosColor);
        }
        String x = points.get(selected).getMonthLongString(this.getContext()) + " " + points.get(selected).getYearString();

        canvas.drawText(x, 30 + (getWidth() / 2 - 30) / 2, getHeight() - padding, valuesPaint);

        String value;
        if (absoluteCalculation) {
            long v = (long) points.get(selected).getPointData().getValue();
            value = Utils.amountToString(currency, v > 0 ? v : -v);
        } else {
            value = Utils.amountToString(currency, (long) points.get(selected).getPointData().getValue());
        }

        canvas.drawText(value, getWidth() / 2 + 30 + (getWidth() / 2 - 30) / 2, getHeight() - padding, valuesPaint);
    }

    /**
     * Calculate the position of points in the chart.
     */
    private void calculatePointsPosition() {
        int w = this.getWidth();
        int h = this.getHeight();

        pointsShapes = new ShapeDrawable[2 * points.size()];
        int x;
        Double y;
        double value;

        double mean = 0;
        for (int i = 0; i < points.size(); i++) {
            if (absoluteCalculation) {
                value = Math.abs(points.get(i).getPointData().getValue());
                y = h - ySpace - padding - graphPadding - (value - absMin) * (h - ySpace - 2 * padding - 2 * graphPadding) / (absMax - absMin);
            } else {
                value = points.get(i).getPointData().getValue();
                y = h - ySpace - padding - graphPadding - (value - min) * (h - ySpace - 2 * padding - 2 * graphPadding) / (max - min);
            }
            x = xSpace + padding + (w - xSpace - 2 * padding) / (points.size() - 1) * i;

            mean += value;

            points.get(i).setX(x);
            points.get(i).setY((int) Math.round(y));

            pointsShapes[i] = new ShapeDrawable(new OvalShape());
            if (selected == i) {
                if (points.get(i).isNegative()) {
                    pointsShapes[i].getPaint().setColor(selectedPointNegColor);
                } else {
                    pointsShapes[i].getPaint().setColor(selectedPointPosColor);
                }
            } else {
                pointsShapes[i].getPaint().setColor(pointColor);
            }

            pointsShapes[i].setBounds((int) points.get(i).getX() - 4, (int) points.get(i).getY() - 4, (int) points.get(i).getX() + 4, (int) points.get(i).getY() + 4);

            pointsShapes[i + points.size()] = new ShapeDrawable(new OvalShape());
            pointsShapes[i + points.size()].getPaint().setColor(Color.BLACK);
            pointsShapes[i + points.size()].setBounds((int) points.get(i).getX() - 2, (int) points.get(i).getY() - 2, (int) points.get(i).getX() + 2, (int) points.get(i).getY() + 2);

        }

        boolean considerNulls = MyPreferences.considerNullResultsInReport(this.getContext());
        if (considerNulls) {
            mean = mean / points.size();
        } else {
            mean = meanNonNull;
        }
        meanPoint = new Report2DPoint(new PeriodValue(null, 0));
        if (absoluteCalculation) {
            mean = Math.abs(mean);
            meanPoint.setY((int) (h - ySpace - padding - graphPadding - (mean - absMin) * (h - ySpace - 2 * padding - 2 * graphPadding) / (absMax - absMin)));
            if (absMin <= 0 && absMax >= 0) {
                zeroPoint = new Report2DPoint(new PeriodValue(null, 0));
                zeroPoint.setY((int) (h - ySpace - padding - graphPadding - (-absMin) * (h - ySpace - 2 * padding - 2 * graphPadding) / (absMax - absMin)));
            } else {
                zeroPoint = null;
            }
        } else {
            meanPoint.setY((int) (h - ySpace - padding - graphPadding - (mean - min) * (h - ySpace - 2 * padding - 2 * graphPadding) / (max - min)));
            if (absMin <= 0 && absMax >= 0) {
                zeroPoint = new Report2DPoint(new PeriodValue(null, 0));
                zeroPoint.setY((int) (h - ySpace - padding - graphPadding - (-min) * (h - ySpace - 2 * padding - 2 * graphPadding) / (max - min)));
            } else {
                zeroPoint = null;
            }
        }
        initialized = true;
    }

    /**
     * Set the chart reference currency.
     *
     * @param currency Reference currency.
     */
    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // get point to highlight as selection
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN
                || action == MotionEvent.ACTION_MOVE) {
            if (event.getY() > padding && event.getY() < getHeight() - padding - ySpace) {
                float dmin = getWidth();
                float d;
                int sel = -1;
                for (int i = 0; i < points.size(); i++) {
                    d = Math.abs(points.get(i).getX() - event.getX());
                    if (d < dmin) {
                        dmin = d;
                        sel = i;
                    }
                }
                if (sel >= 0) {
                    selected = sel;
                    invalidate();
                }
            }
        }
        return true;
    }


}