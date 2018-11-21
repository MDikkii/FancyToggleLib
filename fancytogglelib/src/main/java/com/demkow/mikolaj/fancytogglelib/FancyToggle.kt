package com.demkow.mikolaj.fancytogglelib

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.CompoundButton
import kotlin.math.max


class FancyToggle : CompoundButton {
    interface OnStateChangeListener {
        fun onStateChange(state: ToggleState)
    }

    constructor(context: Context?) : super(context) {
        initialization()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialization(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialization(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes) {
        initialization(attrs)
    }

    private lateinit var mLeftTextPaint: Paint
    private lateinit var mRightTextPaint: Paint
    private lateinit var mThumbLeftTextPaint: Paint
    private lateinit var mThumbRightTextPaint: Paint
    private lateinit var mThumbFillPaint: Paint
    private lateinit var mThumbStrokePaint: Paint
    private lateinit var mBackgroundFillPaint: Paint
    private lateinit var mBackgroundStrokePaint: Paint

    private lateinit var mCurrentState: ToggleState
    var mOnStateChangeListener: OnStateChangeListener? = null


    private var mLeftTextColor: Int = DEFAULT_LEFT_TEXT_COLOR
    private var mRightTextColor: Int = DEFAULT_RIGHT_TEXT_COLOR

    private var mLeftText: String = DEFAULT_LEFT_TEXT
    private var mRightText: String = DEFAULT_RIGHT_TEXT

    private var mRightTextMeasuredWidth: Float = 0f
    private var mLeftTextMeasuredWidth: Float = 0f

    private var mDensity: Float = 1f
    private var mTouchSlop: Int = 0
    private var mClickTimeout: Int = 0

    private var mToggleVerticalMargin: Float = 0f
    private var mToggleHorizontalMargin: Float = 0f
    private var mThumbVerticalMargin: Float = 0f

    private var mTextSize: Float = 0f

    private var mStartX: Float = 0f
    private var mStartY: Float = 0f
    private var mLastX: Float = 0f

    private var mProgress: Float = 0f

    private var mMaxTextWidth: Float = 0f

    private var mThumbHorizontalMargin: Float = 0f

    private fun initialization(attrs: AttributeSet? = null) {
        mDensity = context.resources.displayMetrics.density
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mClickTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout()

        mToggleVerticalMargin = getPixelFromDp(10f)
        mToggleHorizontalMargin = getPixelFromDp(10f)

        mThumbVerticalMargin = getPixelFromDp(10f)
        mThumbHorizontalMargin = getPixelFromDp(10f)


        mTextSize = getPixelFromDp(22f)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FancyToggle)

            mLeftTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fancyToggleLeftTextColor, DEFAULT_LEFT_TEXT_COLOR)
            mLeftText = typedArray.getString(R.styleable.FancyToggle_fancyToggleLeftText) ?: DEFAULT_LEFT_TEXT

            mRightTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fancyToggleLeftTextColor, DEFAULT_RIGHT_TEXT_COLOR)
            mRightText = typedArray.getString(R.styleable.FancyToggle_fancyToggleRightText) ?: DEFAULT_RIGHT_TEXT


            typedArray.recycle()
        }

        mLeftTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mLeftTextPaint.color = mLeftTextColor
        mLeftTextPaint.textSize = mTextSize
        mLeftTextMeasuredWidth = mLeftTextPaint.measureText(mLeftText)

        mRightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mRightTextPaint.color = mRightTextColor
        mRightTextPaint.textSize = mTextSize
        mRightTextMeasuredWidth = mRightTextPaint.measureText(mRightText)

        mThumbLeftTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mThumbLeftTextPaint.color = Color.WHITE
        mThumbLeftTextPaint.textSize = mTextSize

        mThumbRightTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mThumbRightTextPaint.color = Color.WHITE
        mThumbRightTextPaint.textSize = mTextSize

        mMaxTextWidth = max(mLeftTextMeasuredWidth, mRightTextMeasuredWidth)

        mBackgroundFillPaint = Paint()
        mBackgroundFillPaint.isAntiAlias = true
        mBackgroundFillPaint.color = Color.WHITE
        mBackgroundFillPaint.style = Paint.Style.FILL

        mBackgroundStrokePaint = Paint()
        mBackgroundStrokePaint.isAntiAlias = true
        mBackgroundStrokePaint.color = Color.GRAY
        mBackgroundStrokePaint.strokeWidth = getPixelFromDp(1f)
        mBackgroundStrokePaint.style = Paint.Style.STROKE

        mThumbFillPaint = Paint()
        mThumbFillPaint.isAntiAlias = true
        mThumbFillPaint.color = Color.DKGRAY
        mThumbFillPaint.style = Paint.Style.FILL

        mThumbStrokePaint = Paint()
        mThumbStrokePaint.isAntiAlias = true
        mThumbStrokePaint.color = Color.WHITE
        mThumbStrokePaint.strokeWidth = getPixelFromDp(1f)
        mThumbStrokePaint.style = Paint.Style.STROKE

        mCurrentState = ToggleState.LEFT
    }


    override fun onDraw(canvas: Canvas?) {

        val thumbTop = height / 2 - mTextSize * 1.5f
        val thumbBottom = height / 2 + mTextSize * 1.5f

        val toggleTop = thumbTop - mThumbVerticalMargin
        val toggleBottom = thumbBottom + mThumbVerticalMargin

        val thumbWidth = 3 * mMaxTextWidth
        val progressOffset = (width - 2 * mToggleHorizontalMargin - 2 * mThumbHorizontalMargin - thumbWidth) * mProgress

        val thumbRight = mToggleHorizontalMargin + mThumbHorizontalMargin + thumbWidth + progressOffset
        val thumbLeft = mToggleHorizontalMargin + mThumbHorizontalMargin + progressOffset


        // background can be drawn only once per size change / maybe on bitmap?
        canvas?.drawRoundRect(
            mToggleHorizontalMargin,
            toggleTop,
            width - mToggleHorizontalMargin,
            toggleBottom,
            (height - 2 * mToggleVerticalMargin) / 2f,
            (height - 2 * mToggleVerticalMargin) / 2f,
            mBackgroundFillPaint
        )
        canvas?.drawRoundRect(
            mToggleHorizontalMargin,
            toggleTop,
            width - mToggleHorizontalMargin,
            toggleBottom,
            (height - 2 * mToggleVerticalMargin) / 2f,
            (height - 2 * mToggleVerticalMargin) / 2f,
            mBackgroundStrokePaint
        )

        mLeftTextPaint.alpha = (mProgress * 255).toInt()
        mRightTextPaint.alpha = ((1 - mProgress) * 255).toInt()

        // background text
        canvas?.drawText(
            mLeftText,
            mToggleHorizontalMargin + mThumbHorizontalMargin + mLeftTextMeasuredWidth,
            height / 2f + mTextSize / 2,
            mLeftTextPaint
        )
        canvas?.drawText(
            mRightText,
            width - 2 * mRightTextMeasuredWidth - mToggleHorizontalMargin - mThumbHorizontalMargin,
            height / 2f + mTextSize / 2,
            mRightTextPaint
        )

        // thumb
        canvas?.drawRoundRect(
            thumbLeft,
            thumbTop,
            thumbRight,
            thumbBottom,
            (height - mTextSize) / 2f,
            (height - mTextSize) / 2f,
            mThumbFillPaint
        )
        canvas?.drawRoundRect(
            thumbLeft,
            thumbTop,
            thumbRight,
            thumbBottom,
            (height - mTextSize) / 2f,
            (height - mTextSize) / 2f,
            mThumbStrokePaint
        )


        mThumbRightTextPaint.alpha = (mProgress * 255).toInt()
        mThumbLeftTextPaint.alpha = ((1f - mProgress) * 255).toInt()

        // thumb text
        canvas?.drawText(
            mLeftText,
            thumbLeft + mMaxTextWidth,
            height / 2f + mTextSize / 2,
            mThumbLeftTextPaint
        )

        canvas?.drawText(
            mRightText,
            thumbLeft + mMaxTextWidth,
            height / 2f + mTextSize / 2,
            mThumbRightTextPaint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width =
            mMaxTextWidth * 2 + mToggleHorizontalMargin * 2 + mThumbHorizontalMargin * 2 + getPixelFromDp(30f)
        var height = getPixelFromDp(30f)

        setMeasuredDimension(
            resolveSize(width.toInt(), widthMeasureSpec),
            resolveSize(height.toInt(), heightMeasureSpec)
        )

    }

    // from JellyLib !
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) {
            return false
        }

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                catchView()
                mStartX = event.x
                mStartY = event.y
                mLastX = mStartX
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                //set process
                setProgress(
                    mProgress + (x - mLastX) / (width - 2 * mToggleHorizontalMargin - 2 * mThumbHorizontalMargin - 3 * mMaxTextWidth),
                    true
                )

                mLastX = x
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                val deltaX = event.x - mStartX
                val deltaY = event.y - mStartY
                val deltaTime = event.eventTime - event.downTime

                if (deltaX < mTouchSlop && deltaY < mTouchSlop && deltaTime < mClickTimeout) {
                    performClick()
                } else {
 //animate
                }
            }
            else -> {
                Log.d("FancyToggle", "Not supported action!")
                return false
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    // from JellyLib !
    private fun setProgress(progress: Float, shouldCallListener: Boolean) {
        var tempProgress = progress
        Log.d("progress", progress.toString())

        if (tempProgress >= 1f) {
            tempProgress = 1f
            mCurrentState = ToggleState.RIGHT
        } else if (tempProgress <= 0) {
            tempProgress = 0f
            mCurrentState = ToggleState.LEFT
        } else {
            if (mCurrentState == ToggleState.LEFT) {
                mCurrentState = ToggleState.LEFT_TO_RIGHT
            } else if (mCurrentState == ToggleState.RIGHT) {
                mCurrentState = ToggleState.RIGHT_TO_LEFT
            }
        }

        if (shouldCallListener) {
            mOnStateChangeListener?.onStateChange(mCurrentState)
        }

        mProgress = tempProgress
        invalidate()
    }

    private fun getPixelFromDp(dpToConvert: Float): Float {
        return dpToConvert * mDensity
    }

    // from JellyLib !
    private fun catchView() {
        val parent = parent
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    companion object {
        private const val DEFAULT_LEFT_TEXT_COLOR: Int = Color.BLACK
        private const val DEFAULT_RIGHT_TEXT_COLOR: Int = Color.BLACK
        private const val DEFAULT_RIGHT_TEXT = "Offline"
        private const val DEFAULT_LEFT_TEXT = "Online"
    }
}