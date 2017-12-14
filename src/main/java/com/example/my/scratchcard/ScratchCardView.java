package com.example.my.scratchcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

public class ScratchCardView extends ImageView {

    /**
     * 绘制线条的Paint,即用户手指绘制Path
     */
    private Paint mOutterPain;
    /**
     * 记录用户绘制的Path
     */
    private Path mPath;
    /**
     * 内存中创建的Canvas
     */
    private Canvas mCanvas;
    /**
     * mCanvas绘制内容在其上
     */
    private Bitmap mBitmap;

    private int mLastX;
    private int mLastY;
    private Paint mBackPint;
    private Rect mTextBound;
    private String mText = "500,0000,000";
    private boolean isComplete = false;
    private boolean isInit = false;

    /**
     * 初始化canvas的绘制用的画笔
     */
    private void setUpBackPaint() {
        mBackPint = new Paint();
        mTextBound = new Rect();
        mBackPint.setStyle(Paint.Style.FILL);
        mBackPint.setTextScaleX(2f);
        mBackPint.setColor(Color.DKGRAY);
        mBackPint.setTextSize(22);
        mBackPint.getTextBounds(mText, 0, mText.length(), mTextBound);
    }


    public ScratchCardView(Context context) {
        this(context, null);

    }

    public ScratchCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public ScratchCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    private void init() {
        mPath = new Path();
        mOutterPain = new Paint();

        /*DST_OUT =》[Da * (1 - Sa), Dc * (1 - Sa)]，S代表即将画上去，D代表背景，
        透明度等于自身透明度乘于1-原颜色透明度（背景透明度），颜色等于自身颜色乘于1-源颜色透明度
        因为画笔透明度为1，那么画上去的部分就变透明了，这样就实现擦除效果*/
        mOutterPain.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        Drawable drawable = getDrawable();
        // 初始化bitmap
        mBitmap = Bitmap.createBitmap(
                getWidth(),
                getHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                        : Bitmap.Config.RGB_565);

        mCanvas = new Canvas(mBitmap);//mBitmap作为canvas绘画的对象
        drawable.setBounds(0, 0, getWidth(), getHeight());
        drawable.draw(mCanvas);//把drawable画进mCanvas
        setUpBackPaint();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isInit) {
            init();
            isInit = true;
        }
        //绘制奖项
        canvas.drawText(mText, getWidth() / 2 - mTextBound.width() / 2,
                getHeight() / 2 + mTextBound.height() / 2, mBackPint);
        if (!isComplete) {
            drawPath();
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }
    }

    /**
     * 绘制线条
     */
    private void drawPath() {
        mCanvas.drawPath(mPath, mOutterPain);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mPath.reset();
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:

                int dx = Math.abs(x - mLastX);
                int dy = Math.abs(y - mLastY);

                if (dx > 3 || dy > 3)
                    mPath.quadTo(mLastX, mLastY, x, y);//quardTo适合画曲线,lineTo适合直线

                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
                new Thread(mRunnable).start();
                break;
        }

        invalidate();
        return true;
    }


    private Runnable mRunnable = new Runnable() {
        private int[] mPixels;

        @Override
        public void run() {
            int w = getWidth();
            int h = getHeight();

            float wipeArea = 0;
            float totalArea = w * h;

            Bitmap bitmap = mBitmap;

            mPixels = new int[w * h];

            /**
             * 拿到所有的像素信息
             */
            bitmap.getPixels(mPixels, 0, w, 0, 0, w, h);

            /**
             * 遍历统计擦除的区域
             */
            for (int i = 0; i < w; i++) {
                for (int j = 0; j < h; j++) {
                    int index = i + j * w;
                    if (mPixels[index] == 0) {
                        wipeArea++;
                    }
                }
            }

            /**
             * 根据所占百分比，进行一些操作
             */
            if (wipeArea > 0 && totalArea > 0) {
                int percent = (int) (wipeArea * 100 / totalArea);
                Log.e("TAG", percent + "");

                if (percent > 70) {
                    isComplete = true;
                    postInvalidate();
                }
            }
        }

    };
}  