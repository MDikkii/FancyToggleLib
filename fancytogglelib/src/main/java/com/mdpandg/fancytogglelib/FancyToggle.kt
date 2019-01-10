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
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
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

class FancyToggle : AppCompatCheckBox {
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
    private var mContentOffset: Float = 0f
    private var mHorizontalBias: Float = BASE_CONTENT_BIAS

    private var mRightContentMeasuredWidth: Float = 0f
    private var mLeftContentMeasuredWidth: Float = 0f
    private var mMaxContentWidth: Float = 0f
    private var mThumbContentWidth: Float = 0f

    private var mDensity: Float = 1f
    private var mTouchSlop: Int = 0
    private var mTapTimeout: Int = 0

    private var mThumbVerticalMargin: Float = 0f
    private var mThumbHorizontalMargin: Float = 0f
    private var mThumbHorizontalPadding: Float = 0f

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

        //TODO: add possibility to pick one mode (long, double, single)
        mTapTimeout = ViewConfiguration.getPressedStateDuration() + ViewConfiguration.getLongPressTimeout()

        mThumbVerticalMargin = getPixelFromDp(DEFAULT_THUMB_MARGIN)
        mThumbHorizontalMargin = getPixelFromDp(DEFAULT_THUMB_MARGIN)
        mThumbHorizontalPadding = getPixelFromDp(DEFAULT_THUMB_HORIZONTAL_PADDING)
        mIconSize = getPixelFromDp(DEFAULT_ICON_SIZE)
        mTextSize = textSize
        mSelfMargin = getPixelFromDp(DEFAULT_SELF_MARGIN)
        mSelfMarginDoubled = mSelfMargin * 2

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.FancyToggle)

            mLeftTextColor = typedArray.getColor(R.styleable.FancyToggle_fntTextColor, mLeftTextColor)
            mRightTextColor = typedArray.getColor(R.styleable.FancyToggle_fntTextColor, mRightTextColor)
            mLeftTextColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftTextColor, mLeftTextColor)
            mRightTextColor = typedArray.getColor(R.styleable.FancyToggle_fntLeftTextColor, mRightTextColor)
            mRightThumbTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntThumbTextColor, mRightThumbTextColor)
            mLeftThumbTextColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntThumbTextColor, mLeftThumbTextColor)
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
                    typedArray.getColor(R.styleable.FancyToggle_fntThumbColor, mRightThumbColor)
            mLeftThumbColor = typedArray.getColor(R.styleable.FancyToggle_fntThumbColor, mLeftThumbColor)
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
                    typedArray.getColor(R.styleable.FancyToggle_fntThumbBorderColor, mLeftThumbColor)
            mRightThumbBorderColor =
                    typedArray.getColor(R.styleable.FancyToggle_fntThumbBorderColor, mRightThumbColor)
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

            mThumbHorizontalPadding = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbHorizontalPadding,
                mThumbHorizontalPadding.toInt()
            ).toFloat()

            mThumbHorizontalMargin = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbHorizontalMargin,
                mThumbHorizontalMargin.toInt()
            ).toFloat()


            mThumbVerticalMargin = typedArray.getDimensionPixelSize(
                R.styleable.FancyToggle_fntThumbVerticalMargin,
                mThumbVerticalMargin.toInt()
            ).toFloat()

            mHorizontalBias = typedArray.getFloat(
                R.styleable.FancyToggle_fntContentHorizontalBias,
                mHorizontalBias
            )

            mThumbAnimationDuration = typedArray.getInteger(
                R.styleable.FancyToggle_fntAnimationDuration,
                mThumbAnimationDuration.toInt()
            ).toLong()

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
        mThumbContentWidth = mMaxContentWidth + mThumbHorizontalPadding * 2

        mCurrentState = if (isChecked) ToggleState.RIGHT else ToggleState.LEFT
        animateToState(mCurrentState, true)
    }

    private fun setupTypeface() {
        if (!mFontName.isEmpty()) {
            val typefaceFromAssets = Typeface.createFromAsset(context.assets, mFontName)
            typeface = typefaceFromAssets
        }
    }

    override fun onDraw(canvas: Canvas?) {
        drawBackground(canvas)
        drawBackgroundTextAndIcons(canvas)
        drawThumb(canvas)
    }

    private fun drawBackground(canvas: Canvas?) {
        drawToggleBackground(canvas, mBackgroundFillPaint)
        drawToggleBackground(canvas, mBackgroundStrokePaint)
    }

    private fun drawToggleBackground(canvas: Canvas?, paint: Paint) {
        canvas?.drawRoundRect(
            mToggleLeft,
            mToggleTop,
            mToggleRight,
            mToggleBottom,
            (mHeightSubPadding) / 2f,
            (mHeightSubPadding) / 2f,
            paint
        )
    }

    private fun drawThumb(canvas: Canvas?) {
        val thumbLeft = getThumbLeft()
        val thumbRight = getThumbRight(thumbLeft)
        val thumbCenter = getThumbCenter(thumbLeft)

        val midFillColor = calculateMidColor(mLeftThumbColor, mRightThumbColor, mProgress)
        val midStrokeColor = calculateMidColor(mLeftThumbBorderColor, mRightThumbBorderColor, mProgress)
        mThumbFillPaint.color = midFillColor
        mThumbStrokePaint.color = midStrokeColor
        mOnStateChangeListener?.onColorUpdate(midFillColor, midStrokeColor)

        val progressAlpha = getAlphaFromProgress()
        val reverseProgressAlpha = getAlphaFromReverseProgress()

        val cornerRadius = mHeightSubPadding / 2f

        canvas?.drawRoundRect(
            thumbLeft,
            mThumbTop,
            thumbRight,
            mThumbBottom,
            cornerRadius,
            cornerRadius,
            mThumbFillPaint
        )
        canvas?.drawRoundRect(
            thumbLeft,
            mThumbTop,
            thumbRight,
            mThumbBottom,
            cornerRadius,
            cornerRadius,
            mThumbStrokePaint
        )


        val iconPositionOffset = (mThumbBottom - mThumbTop - mIconSize) / 2
        mThumbRightTextPaint.alpha = progressAlpha
        mThumbLeftTextPaint.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.setBounds(
            (thumbCenter - mLeftContentMeasuredWidth / 2 + mContentOffset).toInt(),
            (mThumbTop + iconPositionOffset).toInt(),
            (thumbCenter - mLeftContentMeasuredWidth / 2 + mContentOffset + mIconSize).toInt(),
            (mThumbBottom - iconPositionOffset).toInt()
        )
        mLeftThumbDrawable?.alpha = reverseProgressAlpha
        mLeftThumbDrawable?.draw(canvas!!)

        mRightThumbDrawable?.setBounds(
            (thumbCenter - mRightContentMeasuredWidth / 2 + mContentOffset).toInt(),
            (mThumbTop + iconPositionOffset).toInt(),
            (thumbCenter - mRightContentMeasuredWidth / 2 + mContentOffset + mIconSize).toInt(),
            (mThumbBottom - iconPositionOffset).toInt()
        )
        mRightThumbDrawable?.alpha = progressAlpha
        mRightThumbDrawable?.draw(canvas!!)

        // thumb text
        canvas?.drawText(
            mLeftText,
            thumbCenter - mLeftContentMeasuredWidth / 2 + mContentOffset + mIconSize + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mThumbLeftTextPaint
        )


        canvas?.drawText(
            mRightText,
            thumbCenter - mRightContentMeasuredWidth / 2 + mContentOffset + mIconSize + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mThumbRightTextPaint
        )
    }

    private fun drawBackgroundTextAndIcons(canvas: Canvas?) {
        val progressAlpha = getAlphaFromProgress()
        val reverseProgressAlpha = getAlphaFromReverseProgress()

        mLeftTextPaint.alpha = progressAlpha
        mRightTextPaint.alpha = reverseProgressAlpha

        val iconPositionOffset = (mThumbBottom - mThumbTop - mIconSize) / 2

        // background text
        mLeftDrawable?.setBounds(
            (mStartThumbPosition + mThumbWidth / 2 - mLeftContentMeasuredWidth / 2 + mContentOffset).toInt(),
            (mThumbTop + iconPositionOffset).toInt(),
            (mStartThumbPosition + mThumbWidth / 2 - mLeftContentMeasuredWidth / 2 + mContentOffset + mIconSize).toInt(),
            (mThumbBottom - iconPositionOffset).toInt()
        )
        mLeftDrawable?.alpha = progressAlpha
        mLeftDrawable?.draw(canvas!!)

        mRightDrawable?.setBounds(
            (mEndThumbPosition - mThumbWidth + mThumbWidth / 2 - mRightContentMeasuredWidth / 2 + mContentOffset).toInt(),
            (mThumbTop + iconPositionOffset).toInt(),
            (mEndThumbPosition - mThumbWidth + mThumbWidth / 2 - mRightContentMeasuredWidth / 2 + mContentOffset + mIconSize).toInt(),
            (mThumbBottom - iconPositionOffset).toInt()
        )
        mRightDrawable?.alpha = reverseProgressAlpha
        mRightDrawable?.draw(canvas!!)

        canvas?.drawText(
            mLeftText,
            mStartThumbPosition + mThumbWidth / 2 - mLeftContentMeasuredWidth / 2 + mContentOffset + mIconSize + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mLeftTextPaint
        )
        canvas?.drawText(
            mRightText,
            mEndThumbPosition - mThumbWidth + mThumbWidth / 2 - mRightContentMeasuredWidth / 2 + mContentOffset + mIconSize + mTextToIconMargin,
            mHeightSubPadding / 2 + paddingTop + mSelfMargin + mTextSize / 2.5f,
            mRightTextPaint
        )
    }

    private fun getAlphaFromProgress() = (mProgress * 255).toInt()
    private fun getAlphaFromReverseProgress() = 255 - getAlphaFromProgress()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val width =
            paddingStart + paddingEnd + mThumbContentWidth * 2 + mThumbHorizontalMargin * 4 + mSelfMarginDoubled
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

        mStartThumbPosition = startPadding + mThumbHorizontalMargin + mSelfMargin
        mEndThumbPosition = w - endPadding - mThumbHorizontalMargin - mSelfMargin

        mHeightSubPadding = h - paddingBottom - paddingTop - mSelfMarginDoubled.toInt()
        mWidthSubPadding = w - paddingStart - paddingEnd - mSelfMarginDoubled.toInt()

        mThumbWidth = max(
            mThumbContentWidth,
            mWidthSubPadding / 2.0f - getPixelFromDp(10f)
        )
        mThumbOffset = w - startPadding - endPadding - 2 * mThumbHorizontalMargin - mThumbWidth - mSelfMarginDoubled
        mContentOffset = (mThumbWidth - mMaxContentWidth) * (mHorizontalBias - BASE_CONTENT_BIAS)


        mToggleTop = paddingTop + mSelfMargin
        mToggleBottom = h - paddingBottom - mSelfMargin
        mToggleLeft = paddingStart.toFloat() + mSelfMargin
        mToggleRight = w - paddingEnd.toFloat() - mSelfMargin

        mThumbTop = mToggleTop + mThumbVerticalMargin
        mThumbBottom = mToggleBottom - mThumbVerticalMargin
    }

    private var isOppositeClick = false
    private var startedInThumb: Boolean = false

    @SuppressLint("ClickableViewAccessibility")
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
                    clicked()
                } else {
                    touchEnded()
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
        val centerX = mWidthSubPadding / 2 + paddingStart + mSelfMargin
        val isXOnOppositeSide =
            (mCurrentState == ToggleState.LEFT && mStartX > centerX && mStartX < mToggleRight) ||
                    (mCurrentState == ToggleState.RIGHT && mStartX < centerX && mStartX > mToggleLeft)


        return isXOnOppositeSide && mStartY > mToggleTop && mStartY < mToggleBottom
    }

    private fun isEventInThumb(): Boolean {
        val thumbLeft = getThumbLeft()
        val thumbRight = getThumbRight(thumbLeft)

        return mStartY > mThumbTop && mStartY < mThumbBottom && mStartX > thumbLeft && mStartX < thumbRight
    }

    private fun getProgressOffset() = mThumbOffset * mProgress
    private fun getThumbLeft() = mStartThumbPosition + getProgressOffset()
    private fun getThumbRight(thumbLeft: Float) = thumbLeft + mThumbWidth
    private fun getThumbCenter(thumbLeft: Float) = thumbLeft + mThumbWidth / 2f

    private fun touchEnded() {
        mCurrentState = when {
            mProgress <= 0 -> ToggleState.LEFT
            mProgress > 0 && mProgress <= 0.5f -> ToggleState.RIGHT_TO_LEFT
            mProgress < 1 && mProgress > 0.5f -> ToggleState.LEFT_TO_RIGHT
            mProgress >= 1 -> ToggleState.RIGHT
            else -> mCurrentState
        }

        animateToState(mCurrentState)
    }

    fun clicked() {
        when (mCurrentState) {
            ToggleState.LEFT, ToggleState.LEFT_TO_RIGHT -> animateToState(ToggleState.RIGHT)
            ToggleState.RIGHT, ToggleState.RIGHT_TO_LEFT -> animateToState(ToggleState.LEFT)
        }
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
        } else if (tempProgress <= 0f) {
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
        private const val DEFAULT_THUMB_HORIZONTAL_PADDING = 16f
        private const val DEFAULT_ICON_SIZE = 48f
        private const val DEFAULT_SELF_MARGIN = 2f
        private const val DEFAULT_HEIGHT_RATIO = 4.5f
        private const val BASE_CONTENT_BIAS = 0.5f
    }
}