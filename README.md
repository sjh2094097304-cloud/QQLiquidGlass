# QQ液态模块

- 包名：`com.moyu.LiquidGlass`
- 版本：`0.0.1.beta1`
- 作用域：`com.tencent.mobileqq`
- 适配：QQ 9.2.75
- 作者：陌语
- 液态玻璃库：QWEA0/Liquid-Glass-Android

模块说明：

> 该模块由陌语制作，适配QQ9.2.75的底栏液态玻璃

## GitHub Actions

推送到 `main` 或手动运行 `Build QQ Liquid Glass` 即可编译 Release APK。

APK：

`app/build/outputs/apk/release/app-release.apk`

## Liquid Glass

本项目使用 QWEA0 的 Android View 液态玻璃库。该库支持 API 24+，API 33+ 使用 AGSL 液态玻璃管线。

官方仓库：
https://github.com/QWEA0/Liquid-Glass-Android

当前依赖：
`com.github.QWEA0:liquidglass:v2.0.5`

## Beta 说明

QQ 9.2.75 的 BottomBar 和设置页面存在版本/混淆差异。当前 Beta1 使用结构特征扫描定位底栏，并在设置 Activity 上记录候选类名。第一次实机运行后，应根据 LSPosed 日志把定位器收紧为 QQ 9.2.75 专用实现。

本项目只进行 UI 渲染修改，不绕过 QQ 的账号、安全、加密或认证机制。
