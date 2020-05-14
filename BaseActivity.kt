

open class BaseActivity : AppCompatActivity() {

    companion object {
        public var dLocale: Locale? = null
    }

    init {
        updateConfig(this)
    }

    fun updateConfig(wrapper: android.view.ContextThemeWrapper) {
        if(dLocale==Locale("") ) // Do nothing if dLocale is null
            return


        ///////////////////
        Locale.setDefault(dLocale)
        val configuration = Configuration()
        configuration.setLocale(dLocale)
        wrapper.applyOverrideConfiguration(configuration)
    }

}
