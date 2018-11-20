package com.demkow.mikolaj.fancytogglelib

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
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
    private lateinit var mThumbFillPaint: Paint
    private lateinit var mThumbStrokePaint: Paint
    private lateinit var mBackgroundFillPaint: Paint
    private lateinit var mBackgroundStrokePaint: Paint

    private lateinit var mCurrentState: ToggleState
    private var mOnStateChangeListener: OnStateChangeListener? = null


    private var mLeftTextColor: Int = DEFAULT_LEFT_TEXT_COLOR
    private var mRightTextColor: Int = DEFAULT_RIGHT_TEXT_COLOR

    private var mLeftText: String = DEFAULT_LEFT_TEXT
    private var mRightText: String = DEFAULT_RIGHT_TEXT

    private var mRightTextMeasuredWidth: Float = 0f
    private var mLeftTextMeasuredWidth: Float = 0f

    private var mDensity: Float = 1f
    private var mTouchSlop: Int = 0
    private var mClickTimeout: Int = 0

    private var mToggleVerticalMargin: Float = getPixelFromDp(10f)
    private var mToggleHorizontalMargin: Float = getPixelFromDp(30f)

    private fun initialization(attrs: AttributeSet? = null) {
        mDensity = context.resources.displayMetrics.density
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mClickTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout()

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

        mLeftTextPaint = Paint()
        mLeftTextPaint.color = mLeftTextColor
        mLeftTextPaint.textSize = getPixelFromDp(20f)
        mLeftTextMeasuredWidth = mLeftTextPaint.measureText(mLeftText)

        mRightTextPaint = Paint()
        mRightTextPaint.color = mRightTextColor
        mRightTextPaint.textSize = getPixelFromDp(20f)
        mRightTextMeasuredWidth = mRightTextPaint.measureText(mRightText)

        mBackgroundFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBackgroundFillPaint.color = Color.WHITE
        mBackgroundFillPaint.style = Paint.Style.FILL

        mBackgroundStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mBackgroundStrokePaint.color = Color.GREEN
        mBackgroundStrokePaint.strokeWidth = getPixelFromDp(5f)
        mBackgroundStrokePaint.style = Paint.Style.STROKE

        mThumbFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mThumbFillPaint.color = Color.YELLOW
        mThumbFillPaint.style = Paint.Style.FILL


        mThumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mThumbStrokePaint.color = Color.RED
        mThumbStrokePaint.strokeWidth = getPixelFromDp(5f)
        mThumbStrokePaint.style = Paint.Style.STROKE

        mToggleVerticalMargin = getPixelFromDp(10f)
        mToggleHorizontalMargin = getPixelFromDp(30f)

        mCurrentState = ToggleState.LEFT
    }

    override fun onDraw(canvas: Canvas?) {

        // background
        canvas?.drawRoundRect(
            mToggleHorizontalMargin,
            mToggleVerticalMargin,
            width - mToggleHorizontalMargin,
            height - mToggleVerticalMargin,
            (2) / 2f,
            (2) / 2f,
            mBackgroundFillPaint
        )
        canvas?.drawRoundRect(
            mToggleHorizontalMargin,
            mToggleVerticalMargin,
            width - mToggleHorizontalMargin,
            height - mToggleVerticalMargin,
            (2) / 2f,
            (2) / 2f,
            mBackgroundStrokePaint
        )

        // thumb
        val maxTextWidth = max(mRightTextMeasuredWidth, mLeftTextMeasuredWidth)
        canvas?.drawRoundRect(
            45f + 10f + width * mProgress,
            getPixelFromDp(10f),
            maxTextWidth + 45f + 40f + width * mProgress,
            height - getPixelFromDp(10f),
            (height - getPixelFromDp(20f)) / 2f,
            (height - getPixelFromDp(20f)) / 2f,
            mThumbFillPaint
        )
        canvas?.drawRoundRect(
            45f + 10f + width * mProgress,
            getPixelFromDp(10f),
            maxTextWidth + 45f + 40f + width * mProgress,
            height - getPixelFromDp(10f),
            (height - getPixelFromDp(20f)) / 2f,
            (height - getPixelFromDp(20f)) / 2f,
            mThumbStrokePaint
        )


        // text
        canvas?.drawText(mLeftText, 45f + 20, height / 2f, mLeftTextPaint)
        canvas?.drawText(mRightText, width - mRightTextMeasuredWidth - 45f - 20, height / 2f, mRightTextPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val specWidth = MeasureSpec.getSize(widthMeasureSpec)
        val specWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        val specHeight = MeasureSpec.getSize(widthMeasureSpec)
        val specHeightMode = MeasureSpec.getMode(widthMeasureSpec)

        var width = max(mRightTextMeasuredWidth, mLeftTextMeasuredWidth) * 2 + mToggleHorizontalMargin * 2 + getPixelFromDp(30f)
        var height = getPixelFromDp(30f)

        setMeasuredDimension(
            resolveSize(width.toInt(), widthMeasureSpec),
            resolveSize(height.toInt(), heightMeasureSpec)
        )

    }

    private var mStartX: Float = 0f
    private var mStartY: Float = 0f
    private var mLastX: Float = 0f

    private var mProgress: Float = 0f

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
                setProgress(mProgress + (x - mLastX) / width, true)

                mLastX = x
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                val deltaX = event.x - mStartX
                val deltaY = event.y - mStartY
                val deltaTime = event.eventTime - event.downTime

                if (deltaX < mTouchSlop && deltaY < mTouchSlop && deltaTime < mClickTimeout) {
                    performClick()
                } else {
                    //move to one side or another
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

        if (tempProgress >= 1) {
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

        if(shouldCallListener) {
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