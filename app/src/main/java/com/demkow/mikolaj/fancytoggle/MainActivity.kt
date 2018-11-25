package com.demkow.mikolaj.fancytoggle

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.demkow.mikolaj.fancytogglelib.FancyToggle
import com.demkow.mikolaj.fancytogglelib.ToggleState
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fancytoggle.mOnStateChangeListener = object : FancyToggle.OnStateChangeListener {
            override fun onStateChange(state: ToggleState) {
                Log.d("state", state.name)
            }

            override fun onColorUpdate(midFillColor: Int, midStrokeColor: Int) {
                color_bar.setBackgroundColor(midFillColor)
            }

        }
    }
}
