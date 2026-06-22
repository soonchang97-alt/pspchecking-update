# Phone Check Android Test App

这是用于真机测试的 Android 原生项目。当前环境没有 Android SDK / Gradle，所以这里提供完整项目源码；在安装 Android Studio 的电脑上打开即可生成 APK。

## 生成 APK

1. 安装 Android Studio。
2. 打开此文件夹：`PhoneCheckAndroid`。
3. 等待 Gradle 同步完成。
4. 点击 `Build > Build Bundle(s) / APK(s) > Build APK(s)`。
5. APK 会生成在：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 安装到手机

方式 1：用 USB 连接手机，Android Studio 点击 Run。

方式 2：把 `app-debug.apk` 发送到手机，允许安装未知来源应用后安装。

## 当前测试版包含

- English / Bahasa Melayu / 华语切换，默认 English。
- 读取品牌、型号、Android 版本、RAM、存储容量。
- 手动确认 RAM / ROM。
- 自动检测：摄像头模块、闪光灯、麦克风音量采样、震动、Wi‑Fi、蓝牙、定位服务、电池、动作传感器。
- 交互检测：触控覆盖测试、屏幕纯色坏点测试、音量键测试、扬声器播放测试。
- 使用内置 `price-data.json` 做 MYR 估价样例。
- 人工复核页面和最终报价免责声明。

## 注意

Android 无法 100% 自动判断所有硬件“是否完好”。例如屏幕裂痕、相机模糊、扬声器破音、隐藏进水仍然需要用户确认或人工复核。这个版本已经把能自动触发和测量的项目尽量自动化。
