package com.mahmoud.kozbara

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.settings_dialog.*

class SettingsDialog : DialogFragment(){
var needRestart:Boolean=false
   //lateinit var sharedPreferences:SharedPreferences
    val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(activity)
    }
    var language="en"
    var withSound:Boolean=true

    override fun onCreate(savedInstanceState: Bundle?) {
        // setAppLocale("ar") set
        super.onCreate(savedInstanceState)
        language = sharedPreferences.getString("language", "ar")!!
        withSound=sharedPreferences.getBoolean("withSound",true)


    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.setCanceledOnTouchOutside(false);
        return inflater.inflate(com.mahmoud.kozbara.R.layout.settings_dialog, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


       switch_sound.isChecked= if (withSound) true else  false

        switch_sound.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { switch, isChecked->
                if (isChecked){
                    sharedPreferences.edit().putBoolean("withSound",true).apply()
                }else{

                    sharedPreferences.edit().putBoolean("withSound",false).apply()

                }
            //Toast.makeText(activity,"Changes will be applied next time",Toast.LENGTH_LONG).show()

        })
        if (language.equals("en")){
            english_radio.isChecked=true
        }else{
            arabic_radio.isChecked=true
        }
        language_rg.setOnCheckedChangeListener { group, checkedId ->
          var locale=""
            locale=if (checkedId==R.id.english_radio) "en" else "ar"
            sharedPreferences.edit().putString("language",locale).apply()
            needRestart=true
            Toast.makeText(activity,getString(R.string.changes),Toast.LENGTH_LONG).show()

        }
    }


    override fun getTheme(): Int {
        return com.mahmoud.kozbara.R.style.MyCustomTheme
    }
    override fun onResume() {
        super.onResume()
        val params = dialog?.window?.attributes
        params?.width = resources.getDimensionPixelSize(R.dimen.dialogwidthsettings)//ViewGroup.LayoutParams.MATCH_PARENT;

        params?.height = ViewGroup.LayoutParams.WRAP_CONTENT // resources.getDimensionPixelSize(R.dimen.dialogheight)//ViewGroup.LayoutParams.WRAP_CONTENT;
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setGravity(Gravity.BOTTOM)
       //  dialog?.window?.attributes=params
    }



    override fun onDestroy() {
        super.onDestroy()
        (activity as SettingsDialogListner).onSaveClicked(needRestart)

    }
    interface SettingsDialogListner{
        fun onSaveClicked( isClicked:Boolean)
    }
}

