package com.pyler.betterbatterysaver.hooks;


import android.app.Activity;

import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ActivityController {
    public static final String TAG = "ActivityController";
    public static final String KEY = "disable_activities_run";
    public static final String CLASS_CONTEXT_IMPL = "android.app.ContextImpl";

    public static void init(XSharedPreferences prefs, XC_LoadPackage.LoadPackageParam lpparam) {
        if (!new Utils().shouldHook(prefs, lpparam, KEY)) return;

        try {
            XposedBridge.hookAllMethods(Activity.class, "startActivity", XC_MethodReplacement.returnConstant(null));
            XposedBridge.hookAllMethods(XposedHelpers.findClass(CLASS_CONTEXT_IMPL, lpparam.classLoader), "startActivity", XC_MethodReplacement.returnConstant(null));
        } catch (Throwable t) {
            Logger.i(TAG, t.getMessage());
        }
    }
}

