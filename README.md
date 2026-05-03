# AndroidPaint - 专业插画App

Procreate 风格的专业绘画应用，使用 Kotlin + Jetpack Compose 开发。

## 功能特性

- 🎨 **核心绘图** - Canvas 2D 引擎，支持多图层
- 🖌️ **笔刷系统** - 铅笔、钢笔、水彩、蜡笔等
- 📁 **文件管理** - ORA/PSD 支持
- ☁️ **云同步** - 跨设备同步作品
- ✍️ **文字工具** - 添加文字水印
- 🔲 **选区工具** - 自由选区、变换

## 技术栈

- Kotlin 1.9+
- Jetpack Compose
- MVVM 架构
- Canvas 2D

## 项目结构

```
app/
├── src/main/java/com/jiafenbu/androidpaint/
│   ├── brush/      # 笔刷系统
│   ├── command/    # 命令模式
│   ├── engine/     # 绘图引擎
│   ├── model/      # 数据模型
│   ├── ui/         # 界面组件
│   └── sync/       # 云同步
backend/            # 云端同步服务
```

## 许可证

MIT License
