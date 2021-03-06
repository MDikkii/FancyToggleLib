package com.mdpandg.fancytoggle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.mdpandg.fancytogglelib.FancyToggle
import com.mdpandg.fancytogglelib.ToggleState
import kotlinx.android.synthetic.main.activity_main.*

/*
 *  Copyright 2018 MDP&G Mikołaj Demków
 *  Licensed under the Apache License, Version 2.0 (see LICENSE.md)
 */

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fancytoggle.mOnStateChangeListener = object : FancyToggle.OnStateChangeListener {
            override fun onStateChange(state: ToggleState) {
                Log.d("State", state.name)
            }

            override fun onColorUpdate(midFillColor: Int, midStrokeColor: Int) {
                color_view.setBackgroundColor(midFillColor)
            }

            override fun onInteractionStart() {
                Log.e("Interaction", "start")
            }

            override fun onInteractionEnd() {
                Log.e("Interaction", "end")
            }
        }
    }
}