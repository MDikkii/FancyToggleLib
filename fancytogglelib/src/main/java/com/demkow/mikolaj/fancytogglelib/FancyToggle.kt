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
        fun onColorUpdate(midColor: Int)
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

    private var mLeftThumbColor: Int = Color.rgb(130, 195, 49)
    private var mRightThumbColor: Int = Color.rgb(255, 160, 0)

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

    private var mLeftDrawable: Drawable? = null
    private var mRightDrawable: Drawable? = null
    private var mLeftThumbDrawable: Drawable? = null
    private var mRightThumbDrawable: Drawable? = null


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

        mLeftDrawable = ContextCompat.getDrawable(context, R.drawable.ic_favorite)
        mRightDrawable = ContextCompat.getDrawable(context, R.drawable.ic_favorite_border)

        mLeftThumbDrawable = ContextCompat.getDrawable(context, R.drawable.ic_favorite_white)
        mRightThumbDrawable = ContextCompat.getDrawable(context, R.drawable.ic_favorite_border_white)


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

        mLeftDrawable?.setBounds(
            (mToggleHorizontalMargin + mThumbHorizontalMargin + mLeftTextMeasuredWidth - getPixelFromDp(
                48f
            )).toInt(),
            thumbTop.toInt(),
            (mToggleHorizontalMargin + mThumbHorizontalMargin + mLeftTextMeasuredWidth).toInt(),
            thumbBottom.toInt()
        )
        mLeftDrawable?.alpha = (mProgress * 255).toInt()
        mLeftDrawable?.draw(canvas!!)

        mRightDrawable?.setBounds(
            (width - 2 * mRightTextMeasuredWidth - mToggleHorizontalMargin - mThumbHorizontalMargin - getPixelFromDp(
                48f
            )).toInt(),
            thumbTop.toInt(),
            (width - 2 * mRightTextMeasuredWidth - mToggleHorizontalMargin - mThumbHorizontalMargin ).toInt(),
            thumbBottom.toInt()
        )
        mRightDrawable?.alpha = ((1 - mProgress) * 255).toInt()
        mRightDrawable?.draw(canvas!!)

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


        val midColor = calculateMidColor(mLeftThumbColor, mRightThumbColor, mProgress)
        mThumbFillPaint.color = midColor
        mThumbStrokePaint.color = midColor
        mOnStateChangeListener?.onColorUpdate(midColor)

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
//
        mLeftThumbDrawable?.setBounds(
            (thumbLeft + mMaxTextWidth - getPixelFromDp(48f)).toInt(),
            thumbTop.toInt(),
            (thumbLeft + mMaxTextWidth).toInt(),
            thumbBottom.toInt()
        )
        mLeftThumbDrawable?.alpha = ((1f - mProgress) * 255).toInt()
        mLeftThumbDrawable?.draw(canvas!!)

        mRightThumbDrawable?.setBounds(
            (thumbLeft + mMaxTextWidth - getPixelFromDp(48f)).toInt(),
            thumbTop.toInt(),
            (thumbLeft + mMaxTextWidth).toInt(),
            thumbBottom.toInt()
        )
        mRightThumbDrawable?.alpha = ((mProgress) * 255).toInt()
        mRightThumbDrawable?.draw(canvas!!)

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
        val specWidth = MeasureSpec.getSize(widthMeasureSpec)
        val specWidthMode = MeasureSpec.getMode(widthMeasureSpec)
        val specHeight = MeasureSpec.getSize(widthMeasureSpec)
        val specHeightMode = MeasureSpec.getMode(widthMeasureSpec)

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
                    animateToggle(mCurrentState, true)
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

    private  var mProgressAnimator: ValueAnimator? = null

    fun animateToggle(state: ToggleState, reset: Boolean){

        if(mProgressAnimator?.isRunning == true) {
            return
        }

        setProgressAnimator(state)

        mProgressAnimator?.duration = 1000L

        mProgressAnimator?.start()
    }

    private fun setProgressAnimator(state: ToggleState) {
        val endValue = when(state) {
            ToggleState.LEFT, ToggleState.RIGHT_TO_LEFT -> 0f
            ToggleState.RIGHT, ToggleState.LEFT_TO_RIGHT -> 1f
        }
        mProgressAnimator = ValueAnimator.ofFloat(mProgress, endValue)
        mProgressAnimator?.addUpdateListener { animation ->
            setProgress(
                animation.animatedValue as Float,
                true
            )
        }
        mProgressAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isChecked = when(mProgress) {
                    0f -> true
                    1f -> false
                    else -> false
                }
                super.onAnimationEnd(animation)
            }
        })
        mProgressAnimator?.interpolator = AccelerateDecelerateInterpolator()
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

    // from JellyLib !
    private fun calculateMidColor(leftColor: Int, rightColor: Int, progress: Float): Int {
        return Color.argb(
            Color.alpha(leftColor) + ((Color.alpha(rightColor) - Color.alpha(leftColor)) * progress).toInt(),
            Color.red(leftColor) + ((Color.red(rightColor) - Color.red(leftColor)) * progress).toInt(),
            Color.green(leftColor) + ((Color.green(rightColor) - Color.green(leftColor)) * progress).toInt(),
            Color.blue(leftColor) + ((Color.blue(rightColor) - Color.blue(leftColor)) * progress).toInt()
        )

    }

    companion object {
        private const val DEFAULT_LEFT_TEXT_COLOR: Int = Color.BLACK
        private const val DEFAULT_RIGHT_TEXT_COLOR: Int = Color.BLACK
        private const val DEFAULT_RIGHT_TEXT = "Offline"
        private const val DEFAULT_LEFT_TEXT = "Online"
    }
}