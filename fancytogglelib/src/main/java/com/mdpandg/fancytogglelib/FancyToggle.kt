package com.mdpandg.fancytogglelib

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
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


/*
 *  Copyright 2018 MDP&G Mikołaj Demków
 *  Copyright 2016/5/10 Weiping
 *  Licensed under the Apache License, Version 2.0 (see LICENSE.md)
 *
 *  FancyToggleLib uses some part from: JellyToggleButton
 *  JellyToggleButton is an open source, you can check it out here: https://github.com/Nightonke/JellyToggleButton
 *
 *  As JellyToggleButton is opened under Apache 2.0 license there is change log:
 *  Removed whole code except listed below:
 *  calculateMidColor method from ToggleUtil.java - translated to Kotlin, moved to FancyToggle.kt
 *  onTouchEvent - translated to Kotlin, changed to FancyToggleLib needs
 *  setProgress - translated to Kotlin, changed to FancyToggleLib needs
 *
 */

class FancyToggle : CompoundButton {
    interface OnStateChangeListener {
        fun onStateChange(state: ToggleState)
        fun onColorUpdate(midFillColor: Int, midStrokeColor: Int)
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

    private var mLeftTextColor: Int = DEFAULT_TEXT_COLOR
    private var mRightTextColor: Int = DEFAULT_TEXT_COLOR
    private var mLeftThumbTextColor: Int = DEFAULT_THUMB_TEXT_COLOR
    private var mRightThumbTextColor: Int = DEFAULT_THUMB_TEXT_COLOR

    //TODO:
    private var mLeftIconColor: Int = DEFAULT_ICON_COLOR
    private var mRightIconColor: Int = DEFAULT_ICON_COLOR
    //TODO:
    private var mLeftThumbIconColor: Int = DEFAULT_THUMB_ICON_COLOR
    private var mRightThumbIconColor: Int = DEFAULT_THUMB_ICON_COLOR

    private var mToggleBackgroundColor: Int = DEFAULT_TOGGLE_BACKGROUND_COLOR
    private var mToggleBorderColor: Int = DEFAULT_TOGGLE_BORDER_COLOR
    private var mLeftThumbColor: Int = DEFAULT_LEFT_THUMB_COLOR
    private var mRightThumbColor: Int = DEFAULT_RIGHT_THUMB_COLOR
    private var mLeftThumbBorderColor: Int = mLeftThumbColor
    private var mRightThumbBorderColor: Int = mRightThumbColor

    private var mTextSize: Float = 0f

    private var mRightContentMeasuredWidth: Float = 0f
    private var mLeftContentMeasuredWidth: Float = 0f
    private var mMaxContentWidth: Float = 0f

    private var mDensity: Float = 1f
    private var mTouchSlop: Int = 0
    private var mTapTimeout: Int = 0

    private var mToggleTopPadding: Float = 0f
    private var mToggleBottomPadding: Float = 0f
    private var mToggleEndPadding: Float = 0f
    private var mToggleStartPadding: Float = 0f

    private var mThumbVerticalMargin: Float = 0f
    private var mThumbHorizontalMargin: Float = 0f
    private var mThumbAnimationDuration: Long = DEFAULT_THUMB_ANIMATION_TIME

    private var mStartX: Float = 0f
    private var mStartY: Float = 0f
    private var mLastX: Float = 0f

    private var mProgress: Float = 0f
    private var mFontHeight: Float = 0f

    private var mThumbOffset: Float = 0f

    private var mHeightSubPadding: Int = 0
    private var mWidthSubPadding: Int = 0

    private var mThumbWidth: Float = 0f


    private var mStartThumbPosition: Float = 0f
    private var mEndThumbPosition: Float = 0f

    private var mTextToIconMargin: Float = 0f

    private var mIconSize: Float = 0f

    private var mFontName: String = ""

    private fun initialization(attrs: AttributeSet? = null) {
        mDensity = context.resources.displayMetrics.density
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mTapTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout()
        mTapTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getLongPressTimeout()

        mThumbVerticalMargin = getPixelFromDp(10f)
        mThumbHorizontalMargin = getPixelFromDp(10f)

        mIconSize = getPixelFromDp(48f)
        mTextToIconMargin = getPixelFromDp(10f)

        mTextSize = getPixelFromSp(16f)

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FancyToggle)

            mLeftTextColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftTextColor, DEFAULT_TEXT_COLOR)
            mRightTextColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftTextColor, DEFAULT_TEXT_COLOR)
            mRightThumbTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbTextColor, DEFAULT_THUMB_TEXT_COLOR)
            mLeftThumbTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbTextColor, DEFAULT_THUMB_TEXT_COLOR)
            mLeftIconColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftIconColor, DEFAULT_ICON_COLOR)
            mRightIconColor = typedArray.getColor(R.styleable.FancyToggle_fntRightIconColor, DEFAULT_ICON_COLOR)
            mRightThumbIconColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbIconColor, DEFAULT_THUMB_ICON_COLOR)
            mLeftThumbIconColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbIconColor, DEFAULT_THUMB_ICON_COLOR)
            mRightThumbColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbColor, DEFAULT_RIGHT_THUMB_COLOR)
            mLeftThumbColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbColor, DEFAULT_LEFT_THUMB_COLOR)
            mToggleBackgroundColor =
                    typedArray.getColor(
                        R.styleable.FancyToggle_fntToggleBackgroundColor,
                        DEFAULT_TOGGLE_BACKGROUND_COLOR
                    )
            mToggleBorderColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntToggleBorderColor, DEFAULT_TOGGLE_BORDER_COLOR)
            mLeftThumbBorderColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbBorderColor, mLeftThumbColor)
            mRightThumbBorderColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbBorderColor, mRightThumbColor)
            mLeftText = typedArray.getString(R.styleable.FancyToggle_fntLeftText) ?: DEFAULT_LEFT_TEXT
            mRightText = typedArray.getString(R.styleable.FancyToggle_fntRightText) ?: DEFAULT_RIGHT_TEXT


            mRightThumbDrawable =
                    typedArray.getDrawable(R.styleable.FancyToggle_fntRightThumbIcon) ?:
                    ContextCompat.getDrawable(context, R.drawable.ic_favorite_border_white)
            mRightDrawable =
                    typedArray.getDrawable(R.styleable.FancyToggle_fntRightIcon) ?:
                    ContextCompat.getDrawable(context, R.drawable.ic_favorite_border)
            mLeftThumbDrawable =
                    typedArray.getDrawable(R.styleable.FancyToggle_fntLeftThumbIcon) ?:
                    ContextCompat.getDrawable(context, R.drawable.ic_favorite_white)
            mLeftDrawable =
                    typedArray.getDrawable(R.styleable.FancyToggle_fntLeftIcon) ?:
                    ContextCompat.getDrawable(context, R.drawable.ic_favorite)

            mFontName = typedArray.getString(R.styleable.FancyToggle_fntFontFace) ?: mFontName

            typedArray.recycle()
        }

        setupTypeface()

        mLeftTextPaint = Paint().initTextPaint(mLeftTextColor)
        mRightTextPaint = Paint().initTextPaint(mRightTextColor)
        mThumbLeftTextPaint = Paint().initTextPaint(mLeftThumbTextColor)
        mThumbRightTextPaint = Paint().initTextPaint(mRightThumbTextColor)

        mBackgroundFillPaint = Paint().initShapeFillPaint(mToggleBackgroundColor)
        mBackgroundStrokePaint = Paint().initShapeStrokePaint(mToggleBorderColor, getPixelFromDp(1f))
        mThumbFillPaint = Paint().initShapeFillPaint()
        mThumbStrokePaint = Paint().initShapeStrokePaint(strokeWidth = getPixelFromDp(1f))

        mLeftContentMeasuredWidth += mTextToIconMargin + mIconSize
        mRightContentMeasuredWidth += mTextToIconMargin + mIconSize
        mLeftContentMeasuredWidth += mLeftTextPaint.measureText(mLeftText) + mTextToIconMargin
        mRightContentMeasuredWidth += mRightTextPaint.measureText(mRightText) + mTextToIconMargin
        mMaxContentWidth = max(mLeftContentMeasuredWidth, mRightContentMeasuredWidth)

        mFontHeight = -mLeftTextPaint.fontMetrics.top + mLeftTextPaint.fontMetrics.bottom

        mCurrentState = if(isChecked) ToggleState.RIGHT else ToggleState.LEFT
    }

    private fun setupTypeface() {
        if (!mFontName.isEmpty()) {
            val typefaceFromAssets = Typeface.createFromAsset(context.assets, mFontName)
            typeface = typefaceFromAssets
        }
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
            (mHeightSubPadding) / 2f,
            (mHeightSubPadding) / 2f,
            paint
        )
    }

    override fun onDraw(canvas: Canvas?) {

        mHeightSubPadding = height - paddingBottom - paddingTop
        mWidthSubPadding = width - paddingStart - paddingLeft

        val thumbTop = mHeightSubPadding / 2 + paddingTop - mTextSize * 1.5f
        val thumbBottom = mHeightSubPadding / 2 + paddingTop + mTextSize * 1.5f

        val progressOffset = mThumbOffset * mProgress
        val thumbLeft = mStartThumbPosition + progressOffset
        val thumbRight = thumbLeft + mThumbWidth

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
        val midFillColor = calculateMidColor(mLeftThumbColor, mRightThumbColor, mProgress)
        val midStrokeColor = calculateMidColor(mLeftThumbBorderColor, mRightThumbBorderColor, mProgress)
        mThumbFillPaint.color = midFillColor
        mThumbStrokePaint.color = midStrokeColor
        mOnStateChangeListener?.onColorUpdate(midFillColor, midStrokeColor)

        val progressAlpha = getAlphaFromProgress()
        val reverseProgressAlpha = getAlphaFromReverseProgress()

        val cornerRadius = (mHeightSubPadding - mTextSize) / 2f

        canvas?.drawRoundRect(
            thumbLeft,
            thumbTop,
            thumbRight,
            thumbBottom,
            cornerRadius,
            cornerRadius,
            mThumbFillPaint
        )
        canvas?.drawRoundRect(
            thumbLeft,
            thumbTop,
            thumbRight,
            thumbBottom,
            cornerRadius,
            cornerRadius,
            mThumbStrokePaint
        )


        mThumbRightTextPaint.alpha = progressAlpha
        mThumbLeftTextPaint.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.setBounds(
            (thumbLeft + mThumbHorizontalMargin).toInt(),
            thumbTop.toInt(),
            (thumbLeft + mThumbHorizontalMargin + mIconSize).toInt(),
            thumbBottom.toInt()
        )
        mLeftThumbDrawable?.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.draw(canvas!!)

        mRightThumbDrawable?.setBounds(
            (thumbLeft + mThumbHorizontalMargin).toInt(),
            thumbTop.toInt(),
            (thumbLeft + mThumbHorizontalMargin + mIconSize).toInt(),
            thumbBottom.toInt()
        )
        mRightThumbDrawable?.alpha = progressAlpha
        mRightThumbDrawable?.draw(canvas!!)

        // thumb text
        canvas?.drawText(
            mLeftText,
            thumbLeft + mIconSize + mThumbHorizontalMargin + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mTextSize / 2.5f,
            mThumbLeftTextPaint
        )

        canvas?.drawText(
            mRightText,
            thumbLeft + mIconSize + mThumbHorizontalMargin + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mTextSize / 2.5f,
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
            mStartThumbPosition.toInt(),
            thumbTop.toInt(),
            (mStartThumbPosition + mIconSize).toInt(),
            thumbBottom.toInt()
        )
        mLeftDrawable?.alpha = progressAlpha
        mLeftDrawable?.draw(canvas!!)

        mRightDrawable?.setBounds(
            (mEndThumbPosition - mRightContentMeasuredWidth - mTextToIconMargin).toInt(),
            thumbTop.toInt(),
            (mEndThumbPosition - mRightContentMeasuredWidth - mTextToIconMargin + mIconSize).toInt(),
            thumbBottom.toInt()
        )
        mRightDrawable?.alpha = reverseProgressAlpha
        mRightDrawable?.draw(canvas!!)

        canvas?.drawText(
            mLeftText,
            mStartThumbPosition + mIconSize + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mTextSize / 2.5f,
            mLeftTextPaint
        )
        canvas?.drawText(
            mRightText,
            mEndThumbPosition - mRightContentMeasuredWidth + mIconSize,
            mHeightSubPadding / 2 + paddingTop + mTextSize / 2.5f,
            mRightTextPaint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = paddingStart + paddingEnd + mMaxContentWidth * 4 + mThumbHorizontalMargin * 2
        val height = 3 * mTextSize + 2 * mThumbVerticalMargin + paddingTop + paddingBottom

        val measuredWidth = resolveSize(width.toInt(), widthMeasureSpec)
        val measuredHeight = resolveSize(height.toInt(), heightMeasureSpec)

        setMeasuredDimension(
            measuredWidth,
            measuredHeight
        )

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mToggleTopPadding = paddingTop.toFloat()
        mToggleBottomPadding = paddingBottom.toFloat()
        mToggleStartPadding = paddingStart.toFloat()
        mToggleEndPadding = paddingEnd.toFloat()

        mStartThumbPosition = mToggleStartPadding + mThumbHorizontalMargin
        mEndThumbPosition = w - mToggleEndPadding - mThumbHorizontalMargin

        mThumbWidth = 3 * mTextToIconMargin + mMaxContentWidth
        mThumbOffset = w - mToggleStartPadding - mToggleEndPadding - 2 * mThumbHorizontalMargin -
                mThumbWidth

    }

    // from JellyLib !
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) {
            return false
        }

        parent.requestDisallowInterceptTouchEvent(true)

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mStartX = event.x
                mStartY = event.y
                mLastX = mStartX
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                //set process
                setProgress(
                    mProgress + (x - mLastX) / mThumbOffset,
                    true
                )

                mLastX = x
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                val deltaX = event.x - mStartX
                val deltaY = event.y - mStartY
                val deltaTime = event.eventTime - event.downTime

                if (deltaX < mTouchSlop && deltaY < mTouchSlop && deltaTime < mTapTimeout) {
                    performClick()
                } else {
                    mCurrentState = when {
                        mProgress <= 0 -> ToggleState.LEFT
                        mProgress > 0 && mProgress <= 0.5f -> ToggleState.RIGHT_TO_LEFT
                        mProgress < 1 && mProgress > 0.5f -> ToggleState.LEFT_TO_RIGHT
                        mProgress >= 1 -> ToggleState.RIGHT
                        else -> ToggleState.LEFT
                    }

                    animateToggle(mCurrentState)
                    parent.requestDisallowInterceptTouchEvent(false)
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

        animateToggle(mCurrentState)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    fun animateToggle(state: ToggleState) {

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
    }

    // from JellyLib !
    private fun setProgress(progress: Float, shouldCallListener: Boolean) {
        var tempProgress = progress

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
    private fun calculateMidColor(leftColor: Int, rightColor: Int, progress: Float): Int {
        return Color.argb(
            Color.alpha(leftColor) + ((Color.alpha(rightColor) - Color.alpha(leftColor)) * progress).toInt(),
            Color.red(leftColor) + ((Color.red(rightColor) - Color.red(leftColor)) * progress).toInt(),
            Color.green(leftColor) + ((Color.green(rightColor) - Color.green(leftColor)) * progress).toInt(),
            Color.blue(leftColor) + ((Color.blue(rightColor) - Color.blue(leftColor)) * progress).toInt()
        )

    }

    private fun Paint.initTextPaint(color: Int) : Paint {
        isAntiAlias = true
        this.color = color
        this.textSize = mTextSize
        this.typeface = typeface
        return this
    }

    private fun Paint.initShapePaint(color: Int, style: Paint.Style, strokeWidth: Float = 0f) : Paint {
        isAntiAlias = true
        this.color = color
        this.style = style
        this.strokeWidth = strokeWidth
        return this
    }

    private fun Paint.initShapeStrokePaint(color: Int = Color.BLACK, strokeWidth: Float) : Paint {
        return this.initShapePaint(color, Paint.Style.STROKE, strokeWidth)
    }

    private fun Paint.initShapeFillPaint(color: Int = Color.BLACK) : Paint {
        return this.initShapePaint(color, Paint.Style.FILL)
    }

    companion object {
        private const val DEFAULT_TEXT_COLOR: Int = Color.BLACK
        private const val DEFAULT_THUMB_TEXT_COLOR: Int = Color.WHITE
        private const val DEFAULT_ICON_COLOR: Int = Color.BLACK
        private const val DEFAULT_THUMB_ICON_COLOR: Int = Color.WHITE
        private const val DEFAULT_LEFT_THUMB_COLOR: Int = Color.RED
        private const val DEFAULT_RIGHT_THUMB_COLOR: Int = Color.BLUE
        private const val DEFAULT_TOGGLE_BACKGROUND_COLOR: Int = Color.WHITE
        private const val DEFAULT_TOGGLE_BORDER_COLOR: Int = Color.LTGRAY
        private const val DEFAULT_RIGHT_TEXT = "Online"
        private const val DEFAULT_LEFT_TEXT = "Offline"
        private const val DEFAULT_THUMB_ANIMATION_TIME = 600L
    }
}