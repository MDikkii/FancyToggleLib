package com.mdpandg.fancytogglelib

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
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
import kotlin.math.abs
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

    private var mLeftIconColor: Int = DEFAULT_ICON_COLOR
    private var mRightIconColor: Int = DEFAULT_ICON_COLOR
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

    private var mThumbVerticalMargin: Float = 0f
    private var mThumbHorizontalMargin: Float = 0f
    private var mThumbStartPadding: Float = 0f
    private var mThumbEndPadding: Float = 0f

    private var mThumbTop: Float = 0f
    private var mThumbBottom: Float = 0f
    private var mToggleTop: Float = 0f
    private var mToggleBottom: Float = 0f
    private var mToggleLeft: Float = 0f
    private var mToggleRight: Float = 0f

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

    private var mSelfMarginDoubled: Float = 0f
    private var mSelfMargin: Float = 0f

    private fun initialization(attrs: AttributeSet? = null) {
        mDensity = context.resources.displayMetrics.density
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mTapTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getTapTimeout()
        mTapTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getLongPressTimeout()

        mThumbVerticalMargin = getPixelFromDp(DEFAULT_THUMB_MARGIN)
        mThumbHorizontalMargin = getPixelFromDp(DEFAULT_THUMB_MARGIN)
        mThumbStartPadding = getPixelFromDp(DEFAULT_THUMB_START_PADDING)
        mThumbEndPadding = getPixelFromDp(DEFAULT_THUMB_END_PADDING)
        mIconSize = getPixelFromDp(DEFAULT_ICON_SIZE)
        mTextSize = textSize
        mSelfMargin = getPixelFromDp(DEFAULT_SELF_MARGIN)
        mSelfMarginDoubled = mSelfMargin * 2

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FancyToggle)

            mLeftTextColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftTextColor, mLeftTextColor)
            mRightTextColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftTextColor, mRightTextColor)
            mRightThumbTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbTextColor, mRightThumbTextColor)
            mLeftThumbTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbTextColor, mLeftThumbTextColor)
            mLeftIconColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftIconColor, mLeftIconColor)
            mRightIconColor = typedArray.getColor(R.styleable.FancyToggle_fntRightIconColor, mRightIconColor)
            mRightThumbIconColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbIconColor, mRightThumbIconColor)
            mLeftThumbIconColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbIconColor, mLeftThumbIconColor)
            mRightThumbColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntRightThumbColor, mRightThumbColor)
            mLeftThumbColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftThumbColor, mLeftThumbColor)
            mToggleBackgroundColor =
                    typedArray.getColor(
                        R.styleable.FancyToggle_fntToggleBackgroundColor,
                        mToggleBackgroundColor
                    )
            mToggleBorderColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntToggleBorderColor, mToggleBorderColor)
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

            mFontName = typedArray.getString(R.styleable.FancyToggle_fntFontAssetsPath) ?: mFontName

            mTextToIconMargin =
                    typedArray.getDimensionPixelSize(
                        R.styleable.FancyToggle_fntTextIconMargin,
                        mTextToIconMargin.toInt()
                    ).toFloat()

            mThumbStartPadding = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbStartPadding,
                mThumbStartPadding.toInt()
            ).toFloat()
            mThumbEndPadding = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbEndPadding,
                mThumbEndPadding.toInt()
            ).toFloat()

            mThumbHorizontalMargin = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbHorizontalMargin,
                mThumbHorizontalMargin.toInt()
            ).toFloat()


            mThumbVerticalMargin = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbVerticalMargin,
                mThumbVerticalMargin.toInt()
            ).toFloat()

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
        mLeftContentMeasuredWidth += mLeftTextPaint.measureText(mLeftText)
        mRightContentMeasuredWidth += mRightTextPaint.measureText(mRightText)
        mMaxContentWidth = max(mLeftContentMeasuredWidth, mRightContentMeasuredWidth)

        mFontHeight = -mLeftTextPaint.fontMetrics.top + mLeftTextPaint.fontMetrics.bottom

        mCurrentState = if (isChecked) ToggleState.RIGHT else ToggleState.LEFT
    }

    private fun setupTypeface() {
        if (!mFontName.isEmpty()) {
            val typefaceFromAssets = Typeface.createFromAsset(context.assets, mFontName)
            typeface = typefaceFromAssets
        }
    }

    private fun drawBackground(canvas: Canvas?, toggleTop: Float, toggleBottom: Float) {
        drawToggleBackground(canvas, toggleTop, toggleBottom, mBackgroundFillPaint)
        drawToggleBackground(canvas, toggleTop, toggleBottom, mBackgroundStrokePaint)
    }


    private fun drawToggleBackground(canvas: Canvas?, toggleTop: Float, toggleBottom: Float, paint: Paint) {
        canvas?.drawRoundRect(
            paddingStart.toFloat() + mSelfMargin,
            toggleTop,
            width - paddingEnd.toFloat() - mSelfMargin,
            toggleBottom,
            (mHeightSubPadding) / 2f,
            (mHeightSubPadding) / 2f,
            paint
        )
    }


    override fun onDraw(canvas: Canvas?) {

        mHeightSubPadding = height - paddingBottom - paddingTop - mSelfMarginDoubled.toInt()
        mWidthSubPadding = width - paddingStart - paddingLeft - mSelfMarginDoubled.toInt()

        val thumbTop = mHeightSubPadding / 2 + paddingTop + mSelfMargin - mTextSize * 1.8f
        val thumbBottom = mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize * 1.8f

        val progressOffset = mThumbOffset * mProgress
        val thumbLeft = mStartThumbPosition + mSelfMargin + progressOffset
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

        val cornerRadius = (mHeightSubPadding) / 2f

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


        val iconPositionOffset = (thumbBottom - thumbTop - mIconSize) / 2
        mThumbRightTextPaint.alpha = progressAlpha
        mThumbLeftTextPaint.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.setBounds(
            (thumbLeft + mThumbStartPadding).toInt(),
            (thumbTop + iconPositionOffset).toInt(),
            (thumbLeft + mThumbStartPadding + mIconSize).toInt(),
            (thumbBottom - iconPositionOffset).toInt()
        )
        mLeftThumbDrawable?.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.draw(canvas!!)

        mRightThumbDrawable?.setBounds(
            (thumbLeft + mThumbStartPadding).toInt(),
            (thumbTop + iconPositionOffset).toInt(),
            (thumbLeft + mThumbStartPadding + mIconSize).toInt(),
            (thumbBottom - iconPositionOffset).toInt()
        )
        mRightThumbDrawable?.alpha = progressAlpha
        mRightThumbDrawable?.draw(canvas!!)

        // thumb text
        canvas?.drawText(
            mLeftText,
            thumbLeft + mIconSize + mThumbStartPadding + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mThumbLeftTextPaint
        )


        canvas?.drawText(
            mRightText,
            thumbLeft + mIconSize + mThumbStartPadding + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
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

        val iconPositionOffset = (thumbBottom - thumbTop - mIconSize) / 2

        // background text
        mLeftDrawable?.setBounds(
            (mStartThumbPosition + mThumbStartPadding).toInt(),
            (thumbTop + iconPositionOffset).toInt(),
            (mStartThumbPosition + mThumbStartPadding + mIconSize).toInt(),
            (thumbBottom - iconPositionOffset).toInt()
        )
        mLeftDrawable?.alpha = progressAlpha
        mLeftDrawable?.draw(canvas!!)

        mRightDrawable?.setBounds(
            (mEndThumbPosition - mRightContentMeasuredWidth - mThumbEndPadding).toInt(),
            (thumbTop + iconPositionOffset).toInt(),
            (mEndThumbPosition - mRightContentMeasuredWidth - mThumbEndPadding + mIconSize).toInt(),
            (thumbBottom - iconPositionOffset).toInt()
        )
        mRightDrawable?.alpha = reverseProgressAlpha
        mRightDrawable?.draw(canvas!!)

        canvas?.drawText(
            mLeftText,
            mStartThumbPosition + mIconSize + mTextToIconMargin + mThumbStartPadding,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mLeftTextPaint
        )
        canvas?.drawText(
            mRightText,
            mEndThumbPosition - mRightContentMeasuredWidth - mThumbEndPadding + mTextToIconMargin + mIconSize,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mRightTextPaint
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = paddingStart + paddingEnd + mMaxContentWidth * 4 + mThumbHorizontalMargin * 2 + mSelfMarginDoubled
        val height =
            DEFAULT_HEIGHT_RATIO * mTextSize + 2 * mThumbVerticalMargin + paddingTop + paddingBottom + mSelfMarginDoubled

        val measuredWidth = resolveSize(width.toInt(), widthMeasureSpec)
        val measuredHeight = resolveSize(height.toInt(), heightMeasureSpec)

        setMeasuredDimension(
            measuredWidth,
            measuredHeight
        )

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val startPadding = paddingStart.toFloat()
        val endPadding = paddingEnd.toFloat()

        mStartThumbPosition = startPadding + mThumbHorizontalMargin
        mEndThumbPosition = w - endPadding - mThumbHorizontalMargin

        mHeightSubPadding = h - paddingBottom - paddingTop - mSelfMarginDoubled.toInt()
        mWidthSubPadding = w - paddingStart - paddingEnd - mSelfMarginDoubled.toInt()

        mThumbWidth = mMaxContentWidth + mThumbStartPadding + mThumbEndPadding
        mThumbOffset = w - startPadding - endPadding - 2 * mThumbHorizontalMargin - mThumbWidth - mSelfMarginDoubled

        mThumbTop = mHeightSubPadding / 2 + paddingTop + mSelfMargin - mTextSize * DEFAULT_HEIGHT_RATIO / 2
        mThumbBottom = mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize * DEFAULT_HEIGHT_RATIO / 2

        mToggleTop = mThumbTop - mThumbVerticalMargin
        mToggleBottom = mThumbBottom + mThumbVerticalMargin
        mToggleLeft = paddingStart.toFloat() + mSelfMargin
        mToggleRight = w - paddingEnd.toFloat() - mSelfMargin
    }

    private var isOppositeClick = false
    private var startedInThumb: Boolean = false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isEnabled) {
            return false
        }

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)

                mStartX = event.x
                mStartY = event.y
                mLastX = mStartX

                isOppositeClick = isEventOnOppositeSide()
                startedInThumb = isEventInThumb()

            }
            MotionEvent.ACTION_MOVE -> {
                if (startedInThumb) {
                    val x = event.x
                    setProgress(
                        mProgress + (x - mLastX) / mThumbOffset,
                        true
                    )

                    mLastX = x
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                val deltaX = abs(event.x - mStartX)
                val deltaY = abs(event.y - mStartY)
                val deltaTime = event.eventTime - event.downTime

                if (deltaX < mTouchSlop && deltaY < mTouchSlop && deltaTime < mTapTimeout && isOppositeClick) {
                    performClick()
                } else {
                    performTouchEnd()
                }

                resetTouch()
            }
            else -> {
                Log.d("FancyToggle", "Not supported action!")
                resetTouch()
                return false
            }
        }

        return true
    }

    private fun resetTouch() {
        isOppositeClick = false
        startedInThumb = false

        parent.requestDisallowInterceptTouchEvent(false)
    }

    private fun isEventOnOppositeSide(): Boolean {
        val centerX = mWidthSubPadding / 2 + paddingStart
        val isXOnOppositeSide =
            (mCurrentState == ToggleState.LEFT && mStartX > centerX && mStartX < mToggleRight) ||
                    (mCurrentState == ToggleState.RIGHT && mStartX < centerX && mStartX > mToggleLeft)


        return isXOnOppositeSide && mStartY > mThumbTop && mStartY < mThumbBottom
    }

    private fun isEventInThumb(): Boolean {

        val progressOffset = mThumbOffset * mProgress
        val thumbLeft = mStartThumbPosition + mSelfMargin + progressOffset
        val thumbRight = thumbLeft + mThumbWidth


        return mStartY > mThumbTop && mStartY < mThumbBottom && mStartX > thumbLeft && mStartX < thumbRight
    }

    private fun performTouchEnd() {
        mCurrentState = when {
            mProgress <= 0 -> ToggleState.LEFT
            mProgress > 0 && mProgress <= 0.5f -> ToggleState.RIGHT_TO_LEFT
            mProgress < 1 && mProgress > 0.5f -> ToggleState.LEFT_TO_RIGHT
            mProgress >= 1 -> ToggleState.RIGHT
            else -> mCurrentState
        }

        animateToState(mCurrentState)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun performClick(): Boolean {
        when (mCurrentState) {
            ToggleState.LEFT, ToggleState.LEFT_TO_RIGHT -> animateToState(ToggleState.RIGHT)
            ToggleState.RIGHT, ToggleState.RIGHT_TO_LEFT -> animateToState(ToggleState.LEFT)
        }
        return false
    }

    fun animateToState(state: ToggleState, reset: Boolean = false) {
        if (mProgressAnimator?.isRunning == true) {
            return
        }

        setProgressAnimator(state, reset)
        mProgressAnimator?.start()
    }

    private fun setProgressAnimator(state: ToggleState, reset: Boolean) {
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
            }
        })
        mProgressAnimator?.interpolator = AccelerateDecelerateInterpolator()

        if (reset || mProgress == endValue) {
            mProgressAnimator?.duration = 0L
        } else {
            mProgressAnimator?.duration = when (state) {
                ToggleState.LEFT, ToggleState.RIGHT -> mThumbAnimationDuration
                ToggleState.LEFT_TO_RIGHT -> (mThumbAnimationDuration * (1 - mProgress)).toLong()
                ToggleState.RIGHT_TO_LEFT -> (mThumbAnimationDuration * mProgress).toLong()
            }
        }
    }

    private fun setProgress(progress: Float, shouldCallListener: Boolean) {
        var tempProgress = progress
        val tempState = mCurrentState

        if (tempProgress >= 1f) {
            tempProgress = 1f
            mCurrentState = ToggleState.RIGHT
        } else if (tempProgress <= 0) {
            tempProgress = 0f
            mCurrentState = ToggleState.LEFT
        } else {
            if (tempProgress - mProgress > 0) {
                mCurrentState = ToggleState.LEFT_TO_RIGHT
            } else if (tempProgress - mProgress < 0) {
                mCurrentState = ToggleState.RIGHT_TO_LEFT
            }
        }

        if (shouldCallListener && tempState != mCurrentState) {
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

    private fun calculateMidColor(leftColor: Int, rightColor: Int, progress: Float): Int {
        return Color.argb(
            Color.alpha(leftColor) + ((Color.alpha(rightColor) - Color.alpha(leftColor)) * progress).toInt(),
            Color.red(leftColor) + ((Color.red(rightColor) - Color.red(leftColor)) * progress).toInt(),
            Color.green(leftColor) + ((Color.green(rightColor) - Color.green(leftColor)) * progress).toInt(),
            Color.blue(leftColor) + ((Color.blue(rightColor) - Color.blue(leftColor)) * progress).toInt()
        )

    }

    private fun Paint.initTextPaint(color: Int): Paint {
        isAntiAlias = true
        this.color = color
        this.textSize = mTextSize
        this.typeface = this@FancyToggle.typeface
        return this
    }

    private fun Paint.initShapePaint(color: Int, style: Paint.Style, strokeWidth: Float = 0f): Paint {
        isAntiAlias = true
        this.color = color
        this.style = style
        this.strokeWidth = strokeWidth
        return this
    }

    private fun Paint.initShapeStrokePaint(color: Int = Color.BLACK, strokeWidth: Float): Paint {
        return this.initShapePaint(color, Paint.Style.STROKE, strokeWidth)
    }

    private fun Paint.initShapeFillPaint(color: Int = Color.BLACK): Paint {
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
        private const val DEFAULT_THUMB_MARGIN = 6f
        private const val DEFAULT_THUMB_START_PADDING = 10f
        private const val DEFAULT_THUMB_END_PADDING = 20f
        private const val DEFAULT_ICON_SIZE = 48f
        private const val DEFAULT_SELF_MARGIN = 2f
        private const val DEFAULT_HEIGHT_RATIO = 3.6f
    }
}