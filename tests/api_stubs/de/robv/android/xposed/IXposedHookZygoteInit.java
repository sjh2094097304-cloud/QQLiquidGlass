package de.robv.android.xposed; public interface IXposedHookZygoteInit {void initZygote(StartupParam p)throws Throwable; class StartupParam { public String modulePath; }}
