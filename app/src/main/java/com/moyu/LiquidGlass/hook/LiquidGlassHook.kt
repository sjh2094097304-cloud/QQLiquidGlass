package com.moyu.LiquidGlass.hook

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.example.liquidglass.GlassMaterial
import com.example.liquidglass.LiquidGlassView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

class LiquidGlassHook : IXposedHookLoadPackage {
    companion object {
        private const val QQ = "com.tencent.mobileqq"
        private const val TAG = "QQLiquidGlass"
    }

    override fun handleLoadPackage(lpparam: IXposedHookLoadPackage.LoadPackageParam) {
        if (lpparam.packageName != QQ) return
        XposedBridge.log("$TAG: QQ 9.2.75 hook loaded")
        hookApplication()
        hookActivityLifecycle()
        hookQQSettings()
    }

    private fun hookApplication() {
        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    XposedBridge.log("$TAG: Application.onCreate")
                }
            }
        )
    }

    private fun hookActivityLifecycle() {
        XposedHelpers.findAndHookMethod(
            Activity::class.java,
            "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    if (activity.packageName != QQ) return
                    GlassInjector.scan(activity)
                }
            }
        )
    }

    private fun hookQQSettings() {
        XposedHelpers.findAndHookMethod(
            Activity::class.java,
            "onCreate",
            Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    if (activity.packageName != QQ) return
                    QQSettingsEntry.inspect(activity)
                }
            }
        )
    }
}

private object GlassInjector {
    private const val MARK = 0x4C4751

    fun scan(activity: Activity) {
        if (!Prefs.enabled(activity)) return
        val root = activity.window?.decorView ?: return
        val candidates = ArrayList<ViewGroup>()
        collect(root, candidates, 0)

        candidates.asSequence()
            .filter { looksLikeBottomBar(it, activity) }
            .firstOrNull()
            ?.let { applyGlass(activity, it) }
    }

    private fun collect(v: View, out: MutableList<ViewGroup>, depth: Int) {
        if (depth > 18) return
        val group = v as? ViewGroup ?: return
        out += group
        for (i in 0 until group.childCount) {
            collect(group.getChildAt(i), out, depth + 1)
        }
    }

    private fun looksLikeBottomBar(g: ViewGroup, activity: Activity): Boolean {
        if (g.getTag(MARK) == true) return false
        if (g.childCount !in 2..12) return false
        if (g.width <= 0 || g.height <= 0) return false

        val loc = IntArray(2)
        g.getLocationOnScreen(loc)
        val screenHeight = activity.resources.displayMetrics.heightPixels
        val bottom = loc[1] + g.height

        if (bottom < screenHeight * 0.78f) return false
        if (g.height > screenHeight * 0.28f) return false

        var interactive = 0
        for (i in 0 until g.childCount) {
            val child = g.getChildAt(i)
            if (child.visibility == View.VISIBLE && (child.isClickable || child.isFocusable)) {
                interactive++
            }
        }
        return interactive >= 2
    }

    private fun applyGlass(activity: Activity, original: ViewGroup) {
        original.setTag(MARK, true)
        val parent = original.parent as? ViewGroup ?: return
        val index = parent.indexOfChild(original)
        val lp = original.layoutParams

        val host = FrameLayout(activity)
        host.layoutParams = lp

        parent.removeViewAt(index)
        parent.addView(host, index)

        val glass = LiquidGlassView(activity)
        glass.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        glass.material = GlassMaterial.REGULAR
        glass.enableDynamicBackground = true
        glass.refractionHeight = Prefs.refraction(activity)
        glass.dispersionStrength = Prefs.dispersion(activity) / 100f
        glass.cornerRadius = Prefs.cornerRadius(activity)

        host.addView(glass)
        host.addView(
            original,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        XposedBridge.log("$TAG: LiquidGlass attached to ${original.javaClass.name}")
    }
}

private object QQSettingsEntry {
    fun inspect(activity: Activity) {
        val title = activity.title?.toString()?.lowercase() ?: ""
        val className = activity.javaClass.name.lowercase()
        val likelySettings =
            title.contains("设置") || title.contains("settings") ||
            className.contains("setting") || className.contains("config")

        if (likelySettings) {
            XposedBridge.log(
                "QQLiquidGlass: QQ settings candidate = ${activity.javaClass.name}"
            )
        }
    }
}
