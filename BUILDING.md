# 3.7 构建与签名

## 环境

- JDK 17，含 `java`、`keytool`；Python 3.9+。
- Android SDK platform 35、Build Tools 35.0.0。
- Gradle Wrapper 8.11.1（随源码提供），Android Gradle Plugin 8.7.3，Xposed API 82。
- 首次构建需要访问 Gradle、Google Maven、Maven Central 和 Xposed API 仓库。

保留原 compileSdk/targetSdk 35 与 minSdk 23。构建环境要求依据 [AGP 8.7 文档](https://developer.android.com/build/releases/agp-8-7-0-release-notes)。高版本 Android/不同注入框架的运行结果需要真机验证。

## 本地一条命令生成可安装 APK

```bash
python3 tools/build_release.py --archive /完整路径/气泡3.6源码.zip --sdk /完整路径/Android/Sdk
```

必须使用最初上传、带“签名恢复资料”的外层压缩包。脚本只把 p12 写入临时目录，不复制进项目；从原说明读取别名和密码，先核对证书，再执行回归、Gradle 构建、APK 版本检查和签名验证。

也可以省略 `--sdk`，通过 `ANDROID_HOME`、`ANDROID_SDK_ROOT` 或 `local.properties` 的 `sdk.dir` 指定。没有原压缩包时，设置 `BB_KEYSTORE`、`BB_STORE_PASSWORD`、`BB_KEY_ALIAS`、`BB_KEY_PASSWORD` 四个环境变量；密码不放进命令行参数或源码。

输出：

- `build/deliverables/bubble-3.7.apk`：原发布密钥签名的安装包。
- `build/deliverables/bubble-3.7.apk.sha256`：文件校验值。
- `build/deliverables/apk-verification.txt`：包信息及签名验证结果。

只核对原签名、不编译或联网：

```bash
python3 tools/build_release.py --archive /完整路径/气泡3.6源码.zip --check-signing
```

本次已核对原 p12 公钥证书，SHA-256 为：

```text
d09188089fdf640a439181df1541a6af2276985125d18ba6db7a00892c60dc09
```

应用 ID 与 Provider authority 保留 `com.qiutian.bianpaobubble.v36` 和 `.config`；版本改为 `3.7` / `28`。原 APK 的 versionCode 是 27，旧源码误写成 26。覆盖升级还需框架支持相同签名模块更新。更新后重启 QQ，免 root 框架按其模块加载方式重新载入新包，避免旧 Hook 仍留在进程中。

## GitHub Actions

将本源码上传到自己的 GitHub 仓库。`.github/workflows/android.yml` 已固定官方 Actions 的提交版本。

- 推送到 `main/master`，或手动运行未勾选签名：生成 `bubble-3.7-unsigned.apk`，用于构建检查，不能直接安装。
- 需要可安装 APK：在仓库 Actions Secrets 中配置 `BB_KEYSTORE_BASE64`（原 p12 的单行 Base64）、`BB_STORE_PASSWORD`、`BB_KEY_ALIAS`、`BB_KEY_PASSWORD`，再手动运行并勾选 `sign_release`。
- 成功后从对应运行的 Artifacts 下载 signed 产物。私钥不进入仓库、构建产物或日志。

本次 GitHub 账号没有授权可写仓库，未创建仓库、推送或触发 Actions；这份工作流尚未在 GitHub 实际执行。

## 普通 Gradle 命令

```bash
./gradlew --no-daemon :app:assembleRelease
```

未提供完整签名环境变量时，Gradle 只生成未签名 release。不要改用 debug 签名覆盖原模块：应用原有签名保护仍保留，正式升级需要原发布密钥。

## 验收顺序

1. 原 3.6 用户直接升级，核对 13 个 ID、模式、公告状态和头像。
2. FPA/免 root 框架保持 Provider 不可用，切换/保存/重启 QQ 后核对设置；恢复 Provider 后检查没有回退。
3. 长按普通默认气泡、单独表情、贴纸、转发消息，应不出现模块 ID 入口；长按带真实气泡属性的文字消息，应显示正确 ID。
4. 商城气泡详情仍可选择独立或随机；挂件、头像、主题及冲突参数不触发。
5. 随机池轮内遍历全部 ID；删除当前固定 ID 后关闭独立模式；单 ID 池只能重复该 ID。
6. 开启防撤回后分别测试私聊、群聊、混合同步包、键盘展开、QQ 后台返回；验证普通消息未被吞、保留的消息仍在、底部文字出现。
7. 自检区分“已挂载”和“已触发”；设置页能打开不代表所有功能都成功。

防撤回提示为 QQ 界面底部短时提示，**没有新增永久插入聊天记录的灰条**；旧版删除的消息不会被恢复。自身群撤回等操作主体无法仅靠已知路由字段完全判定，需要实机核对。
