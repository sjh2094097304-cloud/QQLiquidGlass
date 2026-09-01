# 百变气泡 3.7 修复源码

基于上传的 3.6 源码修复。作者与原移植说明、头像和界面样式保留；应用及 QQ 内设置新增 3.7 修复日志。

**当前交付为源码，尚未生成或验证 3.7 APK。** 本次环境没有 Android SDK/Gradle 构建缓存，工具下载请求受审批策略限制。已完成可离线运行的逻辑回归、Java 接口桩编译和原发布证书核对。QQ/Android/FPA 真机行为仍需构建后验收。

- [更新日志](RELEASE_NOTES.md)
- [构建及签名](BUILDING.md)
- [修复与验证记录](VERIFICATION.md)
- [回归测试说明](tests/README.md)

有 Android SDK 的环境可以直接使用原压缩包中的签名：

```bash
python3 tools/build_release.py --archive /完整路径/气泡3.6源码.zip --sdk /完整路径/Android/Sdk
```

成功后产出 `build/deliverables/bubble-3.7.apk`，脚本会核对包名、版本和原签名证书。源码中已准备 GitHub Actions 工作流；本次没有可写仓库授权，未发布到 GitHub，也未运行云端构建。

适用传统 Xposed API 的框架；QQ 最低声明版本保留 9.2.75。增加了免 root 隔离环境的本地配置、延迟注入及错误降级逻辑，但不宣称“所有框架/所有后续 QQ 版本均已验证”。
