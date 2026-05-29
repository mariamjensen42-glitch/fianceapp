<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# AI记账助手

一款基于Gemini AI的智能记账应用，支持AI智能记账、AI生成图表和AI生成报表。

## 运行项目

**环境要求:** [Android Studio](https://developer.android.com/studio) / JDK 21

### Android Studio

1. 用Android Studio打开项目
2. 等待Gradle同步完成
3. 运行到模拟器或真机

### 命令行

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 发布版本

推送tag自动触发GitHub Actions构建：

```bash
git tag v1.0.0
git push origin v1.0.0
```

构建产物会作为GitHub Release附件自动发布。

## 功能特性

- AI智能记账：描述消费内容，自动识别类别
- AI图表生成：智能生成消费统计图表
- AI报表生成：自动生成月度消费分析报告
