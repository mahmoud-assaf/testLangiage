

class MainActivity : BaseActivity(), ResultDialog.ResultDialogListener,SettingsDialog.SettingsDialogListner {
    override fun onFinished() {
        bar?.visibility= View.INVISIBLE
    }


    override fun onSaveClicked(isClicked:Boolean) {
        if (isClicked)  {
            android.os.Process.killProcess(android.os.Process.myPid())
            var i = getBaseContext().getPackageManager().
            getLaunchIntentForPackage(getBaseContext().getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish()
        }
    }


}
