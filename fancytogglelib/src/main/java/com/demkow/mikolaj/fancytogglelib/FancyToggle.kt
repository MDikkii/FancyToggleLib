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
import android.util.TypedValue
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

    var mOnStateChangeListener: OnStateChangeListener? = null

    private var mLeftDrawable: Drawable? = null
    private var mRightDrawable: Drawable? = null
    private var mLeftThumbDrawable: Drawable? = null
    private var mRightThumbDrawable: Drawable? = null

    private var mProgressAnimator: ValueAnimator? = null

    private lateinit var mCurrentState: ToggleState

    private var mLeftText: String = DEFAULT_LEFT_TEXT
    private var mRightText: String = DEFAULT_RIGHT_TEXT
    private var mLeftTextColor: Int = DEFAULT_LEFT_TEXT_COLOR
    private var mRightTextColor: Int = DEFAULT_RIGHT_TEXT_COLOR
    private var mTextSize: Float = 0f

    private var mRightTextMeasuredWidth: Float = 0f
    private var mLeftTextMeasuredWidth: Float = 0f
    private var mMaxTextWidth: Float = 0f

    private var mDensity: Float = 1f
    private var mTouchSlop: Int = 0
    private var mClickTimeout: Int = 0

    private var mToggleVerticalMargin: Float = 0f
    private var mToggleHorizontalMargin: Float = 0f

    private var mThumbVerticalMargin: Float = 0f
    private var mThumbHorizontalMargin: Float = 0f
    private var mThumbAnimationDuration: Long = DEFAULT_THUMB_ANIMATION_TIME
    private var mLeftThumbColor: Int = Color.RED
    private var mRightThumbColor: Int = Color.GREEN

    private var mStartX: Float = 0f
    private var mStartY: Float = 0f
    private var mLastX: Float = 0f

    private var mProgress: Float = 0f
    private var mFontHeight: Float = 0f

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

        mTextSize = getPixelFromSp(16f)

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

        mFontHeight = -mLeftTextPaint.fontMetrics.top + mLeftTextPaint.fontMetrics.bottom

        mCurrentState = ToggleState.LEFT
    }


    private fun drawBackground(canvas: Canvas?, toggleTop: Float, toggleBottom: Float) {
        // background can be drawn only once per size change / maybe on bitmap?
        drawToggleBackground(canvas, toggleTop, toggleBottom, mBackgroundFillPaint)
        drawToggleBackground(canvas, toggleTop, toggleBottom, mBackgroundStrokePaint)
    }

    private fun drawToggleBackground(canvas: Canvas?, toggleTop: Float, toggleBottom: Float, paint: Paint) {
        canvas?.drawRoundRect(
            paddingStart.toFloat(),
            toggleTop,
            width - paddingEnd.toFloat(),
            toggleBottom,
            (mHeightWithPadding - 2 * mToggleVerticalMargin) / 2f,
            (mHeightWithPadding - 2 * mToggleVerticalMargin) / 2f,
            paint
        )
    }

    private var mHeightWithPadding: Int = 0
    private var mWidthWithPadding: Int = 0

    override fun onDraw(canvas: Canvas?) {

        mHeightWithPadding = height - paddingBottom - paddingTop
        mWidthWithPadding = width - paddingStart - paddingLeft

        val thumbTop = mHeightWithPadding / 2 - mTextSize * 1.5f
        val thumbBottom = mHeightWithPadding / 2 + mTextSize * 1.5f
        val thumbWidth = 2.2f * mMaxTextWidth
        val progressOffset = (mWidthWithPadding - 2 * mToggleHorizontalMargin - 2 * mThumbHorizontalMargin - thumbWidth) * mProgress
        val thumbRight = mToggleHorizontalMargin + mThumbHorizontalMargin + thumbWidth + progressOffset
        val thumbLeft = mToggleHorizontalMargin + mThumbHorizontalMargin + progressOffset

        val toggleTop = thumbTop - mThumbVerticalMargin
        val toggleBottom = thumbBottom + mThumbVerticalMargin

        drawBackground(canvas, toggleTop, toggleBottom)
        drawBackgroundTextAndIcons(thumbTop, thumbBottom, canvas)
        drawThumb(canvas, thumbLeft, thumbTop, thumbRight, thumbBottom)
    }

    private fun getAlphaFromProgress() = (mProgress * 255).toInt()
    private fun getAlphaFromReverseProgress() = 255 - getAlphaFromProgress()

    private fun drawThumb(
        canvas: Canvas?,
        thumbLeft: Float,
        thumbTop: Float,
        thumbRight: Float,
        thumbBottom: Float
    ) {
        val midColor = calculateMidColor(mLeftThumbColor, mRightThumbColor, mProgress)
        mThumbFillPaint.color = midColor
        mThumbStrokePaint.color = midColor
        mOnStateChangeListener?.onColorUpdate(midColor)

        val progressAlpha = getAlphaFromProgress()
        val reverseProgressAlpha = getAlphaFromReverseProgress()

        canvas?.drawRoundRect(
            thumbLeft,
            thumbTop,
            thumbRight,
            thumbBottom,
            (mHeightWithPadding - mTextSize) / 2f,
            (mHeightWithPadding - mTextSize) / 2f,
            mThumbFillPaint
        )
        canvas?.drawRoundRect(
            thumbLeft,
            thumbTop,
            thumbRight,
            thumbBottom,
            (mHeightWithPadding - mTextSize) / 2f,
            (mHeightWithPadding - mTextSize) / 2f,
            mThumbStrokePaint
        )


        mThumbRightTextPaint.alpha = progressAlpha
        mThumbLeftTextPaint.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.setBounds(
            (thumbLeft + mMaxTextWidth - getPixelFromDp(48f)).toInt(),
            thumbTop.toInt(),
            (thumbLeft + mMaxTextWidth).toInt(),
            thumbBottom.toInt()
        )
        mLeftThumbDrawable?.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.draw(canvas!!)

        mRightThumbDrawable?.setBounds(
            (thumbLeft + mMaxTextWidth - getPixelFromDp(48f)).toInt(),
            thumbTop.toInt(),
            (thumbLeft + mMaxTextWidth).toInt(),
            thumbBottom.toInt()
        )
        mRightThumbDrawable?.alpha = progressAlpha
        mRightThumbDrawable?.draw(canvas!!)

        // thumb text
        canvas?.drawText(
            mLeftText,
            thumbLeft + mMaxTextWidth,
            mHeightWithPadding / 2f + mFontHeight / 2,
            mThumbLeftTextPaint
        )

        canvas?.drawText(
            mRightText,
            thumbLeft + mMaxTextWidth,
            mHeightWithPadding / 2f + mFontHeight / 2,
            mThumbRightTextPaint
        )
    }

    private fun drawBackgroundTextAndIcons(
        thumbTop: Float,
        thumbBottom: Float,
        canvas: Canvas?
    ) {
        val progressAlpha = getAlphaFromProgress()
        val reverseProgressAlpha = getAlphaFromReverseProgress()

        mLeftTextPaint.alpha = progressAlpha
        mRightTextPaint.alpha = reverseProgressAlpha

        // background text

        mLeftDrawable?.setBounds(
            (mToggleHorizontalMargin + mThumbHorizontalMargin * 2 + mLeftTextMeasuredWidth - getPixelFromDp(
                48f
            )).toInt(),
            thumbTop.toInt(),
            (mToggleHorizontalMargin + mThumbHorizontalMargin * 2 + mLeftTextMeasuredWidth).toInt(),
            thumbBottom.toInt()
        )
        mLeftDrawable?.alpha = progressAlpha
        mLeftDrawable?.draw(canvas!!)

        mRightDrawable?.setBounds(
            (mWidthWithPadding -  mRightTextMeasuredWidth - mToggleHorizontalMargin - mThumbHorizontalMargin * 2 - getPixelFromDp(
                48f
            )).toInt(),
            thumbTop.toInt(),
            (mWidthWithPadding -  mRightTextMeasuredWidth - mToggleHorizontalMargin - mThumbHorizontalMargin * 2).toInt(),
            thumbBottom.toInt()
        )
        mRightDrawable?.alpha = reverseProgressAlpha
        mRightDrawable?.draw(canvas!!)

        canvas?.drawText(
            mLeftText,
            mToggleHorizontalMargin + mThumbHorizontalMargin * 2 + mLeftTextMeasuredWidth,
            height / 2f + mFontHeight / 3,
            mLeftTextPaint
        )
        canvas?.drawText(
            mRightText,
            width - mRightTextMeasuredWidth - mToggleHorizontalMargin - mThumbHorizontalMargin * 2,
            height / 2f + mFontHeight / 3,
            mRightTextPaint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width =
            paddingStart + paddingEnd  +  mMaxTextWidth * 5  + mThumbHorizontalMargin * 2
        val height =   paddingTop + paddingBottom

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
                    mProgress + (x - mLastX) / (mWidthWithPadding - 2 * mToggleHorizontalMargin - 2 * mThumbHorizontalMargin - 3 * mMaxTextWidth),
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
                    mCurrentState = when {
                        mProgress <= 0 -> ToggleState.LEFT
                        mProgress > 0 && mProgress <= 0.5f -> ToggleState.RIGHT_TO_LEFT
                        mProgress < 1 && mProgress > 0.5f -> ToggleState.LEFT_TO_RIGHT
                        mProgress >= 1 -> ToggleState.RIGHT
                        else -> ToggleState.LEFT
                    }

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

    override fun setChecked(checked: Boolean) {
        if (!::mCurrentState.isInitialized) {
            mCurrentState = ToggleState.LEFT
        }

        if (mProgressAnimator?.isRunning == true) {
            return
        }
        super.setChecked(checked)

        mCurrentState = if (checked) {
            ToggleState.RIGHT
        } else {
            ToggleState.LEFT
        }

        animateToggle(mCurrentState, true)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun animateToggle(state: ToggleState, reset: Boolean) {

        if (mProgressAnimator?.isRunning == true) {
            return
        }

        setProgressAnimator(state)
        mProgressAnimator?.start()
    }

    private fun setProgressAnimator(state: ToggleState) {
        val endValue = when (state) {
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
                val checked = when (mProgress) {
                    0f -> false
                    1f -> true
                    else -> false
                }
                super@FancyToggle.setChecked(checked)
                super.onAnimationEnd(animation)
            }
        })
        mProgressAnimator?.interpolator = AccelerateDecelerateInterpolator()
        mProgressAnimator?.duration = when (state) {
            ToggleState.LEFT, ToggleState.RIGHT -> mThumbAnimationDuration
            ToggleState.LEFT_TO_RIGHT -> (mThumbAnimationDuration * (1 - mProgress)).toLong()
            ToggleState.RIGHT_TO_LEFT -> (mThumbAnimationDuration * mProgress).toLong()
        }

        Log.d("status and duration", state.name + " " + mProgressAnimator?.duration)
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

    private fun getPixelFromSp(spToConvert: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spToConvert, context.resources.displayMetrics)
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
        private const val DEFAULT_RIGHT_TEXT = "Gebt acht"
        private const val DEFAULT_LEFT_TEXT = "Online"
        private const val DEFAULT_THUMB_ANIMATION_TIME = 600L
    }
}