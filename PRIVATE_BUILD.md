# 3.7 私人构建说明

只更改应用的 `SignatureGuard.java`：保留调用接口，将应用启动及 Hook 加载时对原证书的限制改为空操作。其他应用源码、Gradle 配置、界面资源、作者信息、头像和包名保持不变。

这只是去掉应用自己的签名限制，**不是生成无签名 APK**。Android 仍会验证安装签名；换了证书就不能直接覆盖已安装的原版。

## 无需原密钥的私人构建

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0，以及可下载 Gradle/AGP 依赖的网络或已有缓存。在项目根目录运行：

```bash
python3 tools/build_private.py --sdk /实际路径/Android/Sdk
```

脚本执行 `:app:assembleDebug`，使用 Android Gradle Plugin 的默认调试签名，不需要填写原发布密码。成功后生成：

- `build/private-deliverables/bubble-3.7-private-debug.apk`
- `build/private-deliverables/bubble-3.7-private-debug.apk.sha256`
- `build/private-deliverables/apk-verification.txt`

版本仍为 3.7（versionCode 28），包名仍为 `com.qiutian.bianpaobubble.v36`。这是 debug 构建，不是混淆的 release 构建；本项目的 release 选项没有被改动。

## 安装与后续更新

不同证书不能覆盖原版。请先备份重要配置；需要覆盖更新时，使用原发布密钥，不要为了测试直接卸载而丢失数据。调试密钥也应固定保存，换环境生成另一把密钥同样会影响后续更新。

使用原来的 `tools/build_release.py` 时仍然要求原证书；该发布保护有意保留。这与应用内已取消的 `SignatureGuard` 是两回事。

## 当前验证边界

私人入口检查和原有逻辑回归、接口桩编译记录见 `tests/private-validation.txt`。**这些不是 Android SDK/DEX/APK 编译，不是 QQ 真机测试。当前没有新的 3.7 APK。**

源码不包含原私钥。用户另有独立备份 ZIP。旧 GitHub 网页构建交接脚本锁定的是另一份源码哈希，不能直接用于私人副本。

取消内部限制后，应用不再拦截重签名包。请只使用可信来源的私人构建，不要公开任何私钥。
