package com.gwell.iotvideodemo.kt.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
import androidx.appcompat.app.AppCompatActivity
import com.gwell.iotvideodemo.R
import com.gwell.iotvideodemo.kt.utils.StatusBarUtils
import com.gwell.iotvideodemo.kt.utils.ViewUtils
import com.gwell.iotvideodemo.kt.widget.dialog.LoadingDialog
import com.gwell.iotvideodemo.rxbus2.RxBus
import com.tbruyelle.rxpermissions2.RxPermissions

abstract class BaseActivity<P : IBasePresenter> : AppCompatActivity(), IBaseView {
    protected val TAG = javaClass.simpleName

    private val activityConfig = ActivityConfig()

    lateinit var mBasePresenter: P

    var mRxPermission: RxPermissions? = null

    val loadingDialog: LoadingDialog by lazy {
        LoadingDialog(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initActivityConfig(activityConfig)

        if (activityConfig.isHideNavigation) {
            hideNavigationBar()
        }

        setContentView(getResId())

        if (activityConfig.isUseStatusBar) {
            StatusBarUtils.setColor(this, getStatusBarColor(), 0)
        }

        //保持屏幕亮
        if (activityConfig.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        if (activityConfig.isApplyRxBus) {
            RxBus.getDefault().register(this)
        }

        if (activityConfig.isPortraitOrientation) {
            val lp = window.attributes
            //修复BUG：  Only fullscreen activities can request orientation
            if (lp.flags == FLAG_FULLSCREEN) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        //状态栏颜色在透明时设置为深色
        if (activityConfig.isStatusBarTransparent) {
            setStatusBarDarkMode()
        }

        // 布局延伸到状态栏
        if (activityConfig.isSteep) {
            StatusBarUtils.setTransparentForImageView(this, null)
            setStatusBarDarkMode()
        }

        try {
            //关闭自动旋转
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
        } catch (e: Exception) {
            e.printStackTrace()
        }

        init(savedInstanceState)
    }

    /**
     * 配置选项
     */
    protected open fun initActivityConfig(activityConfig: ActivityConfig) {

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun hideLoadingDialog() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    override fun showLoadingDialog(msg: String?, cancelCancel: Boolean) {
        if (isFinishing) {
            return
        }
        loadingDialog.setCancelable(cancelCancel)
        if (msg.isNullOrEmpty()) {
            loadingDialog.show()
        } else {
            loadingDialog.show(msg)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus and activityConfig.isHideNavigation) hideNavigationBar()
    }


    override fun onDestroy() {
        super.onDestroy()
        hideLoadingDialog()
        if (::mBasePresenter.isInitialized) {
            mBasePresenter.dispose()
        }
        if (activityConfig.isApplyRxBus) RxBus.getDefault().unregister(this)
    }

    /**
     * 页面名称
     */
    open val pageName: String
        get() = javaClass.simpleName

    /**
     * 获取状态栏颜色
     */
    open fun getStatusBarColor(): Int = ViewUtils.getColor(R.color.colorPrimary)

    /**
     * 设置状态栏Dark模式
     */
    fun setStatusBarDarkMode() {
        StatusBarUtils.setStatusBarDark(this, true)
    }

    /**
     * 设置状态栏Light模式
     */
    fun setStatusBarLightMode() {
        StatusBarUtils.setStatusBarDark(this, false)
    }

    /**
     * 是否是刘海屏
     */
    fun hasNotchInScreen(): Boolean {
        var res = false
        try {
            res = hasNotchInHW() || hasNotchInOppo() || hasNotchInVivo()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    /**
     * 隐藏导航栏
     */
    fun hideNavigationBar() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
        decorView.systemUiVisibility = uiOptions
    }

    /**
     * 显示导航栏
     */
    fun showNavigationBar() {
        val decorView = window.decorView
        val uiOptions = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                )
        decorView.systemUiVisibility = uiOptions
    }

    /**
     * 隐藏状态栏
     */
    fun hideStatusBar() {
        val decorView = window.decorView
        var systemUiVisibility = decorView.systemUiVisibility
        val uiOptions = (systemUiVisibility
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE)
        decorView.systemUiVisibility = uiOptions
    }

    /**
     * 显示状态栏
     */
    fun showStatusBar() {
        val decorView = window.decorView
        var systemUiVisibility = decorView.systemUiVisibility
        val uiOptions = (systemUiVisibility and (View.SYSTEM_UI_FLAG_FULLSCREEN.inv()))
        decorView.systemUiVisibility = uiOptions
    }

    @Volatile
    private var mHasCheckFullScreen: Boolean = false
    @Volatile
    private var mIsFullScreenDevice: Boolean = false

    /**
     * 判断是否是全面屏
     */
    fun isFullScreenDevice(context: Context): Boolean {
        if (mHasCheckFullScreen) {
            return mIsFullScreenDevice
        }
        mHasCheckFullScreen = true
        mIsFullScreenDevice = false
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val point = Point()
        display.getRealSize(point)
        val width: Float
        val height: Float
        if (point.x < point.y) {
            width = point.x.toFloat()
            height = point.y.toFloat()
        } else {
            width = point.y.toFloat()
            height = point.x.toFloat()
        }
        if (height / width >= 1.97f) {
            mIsFullScreenDevice = true
        }

        return mIsFullScreenDevice
    }

    private fun hasNotchInHW(): Boolean {
        var ret = false
        try {
            val cl = classLoader
            val HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil")
            val get = HwNotchSizeUtil.getMethod("hasNotchInScreen")
            ret = get.invoke(HwNotchSizeUtil) as Boolean
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            return ret
        }
    }

    private fun hasNotchInOppo(): Boolean {
        return getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism")
    }

    @SuppressLint("PrivateApi")
    private fun hasNotchInVivo(): Boolean {
        var hasNotch = false
        try {
            val cl = classLoader
            val ftFeature = cl.loadClass("android.util.FtFeature")
            val methods = ftFeature.getDeclaredMethods()
            if (methods != null) {
                for (i in methods.indices) {
                    val method = methods[i]
                    if (method.name.equals("isFeatureSupport", true)) {
                        hasNotch = method.invoke(ftFeature, 0x00000020) as Boolean
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            hasNotch = false
        }
        return hasNotch
    }


    protected class ActivityConfig : AppConfig() {
        /**
         * 状态栏透明
         */
        var isStatusBarTransparent = false

        /**
         * 竖屏
         */
        var isPortraitOrientation = true

        /**
         * 隐藏导航栏
         */
        var isHideNavigation = false

        /**
         * 使用状态栏
         */
        var isUseStatusBar = true

        /**
         * 屏幕常亮
         */
        var keepScreenOn = false

        /**
         * 布局延伸到状态栏
         */
        var isSteep = false
    }
}