# AndroidPaint

专业插画应用 for Android

## 功能特性

### 画布
- 默认尺寸适配设备屏幕
- 双指缩放（0.1x - 10x）
- 双指旋转画布
- 纯白背景

### 笔刷（3种）
1. **铅笔（Pencil）**：细线硬边，带抖动效果
2. **钢笔（Ink Pen）**：平滑曲线，线条稳定
3. **橡皮擦（Eraser）**：使用 DestinationOut 混合模式擦除

### 笔刷参数（4项可调）
- 大小（1-100）
- 透明度（1%-100%）
- 间距（1%-100%）
- 抖动（0%-100%，橡皮擦除外）

### 撤销/重做
- 50步上限
- 整笔撤销

### 导出
- PNG 格式
- JPEG 格式
- 保存到相册

### 颜色选择器
- HSV 色轮
- 明度滑块
- 最近使用颜色（20个）

## 技术架构

### 图层优先渲染
从第一天起就按图层渲染。每层独立 Bitmap，从底到顶合成。

### 渲染抽象层
定义 DrawEngine 接口，当前用 Canvas 2D 实现，将来可替换为 GPU 实现。

### 笔画数据驱动
每个笔画存储完整数据（点列表 + 笔刷参数 + 颜色），支持可靠的撤销/重做和项目保存。

### 命令模式撤销
每个操作都是可逆的 Command 对象。

## 开发环境

- Kotlin 2.0.0
- Jetpack Compose
- minSdk 26, targetSdk 34
- compileSdk 34
- JVM target 17

## 构建

```bash
./gradlew assembleDebug
```

## 项目结构

```
app/src/main/java/com/jiafenbu/androidpaint/
├── MainActivity.kt
├── engine/           # 渲染引擎
├── brush/           # 笔刷定义和渲染
├── command/         # 命令模式实现
├── model/           # 数据模型
├── ui/              # UI 组件
│   ├── canvas/      # 画布相关
│   ├── toolbar/      # 工具栏
│   └── colorpicker/ # 颜色选择器
└── export/          # 导出功能
```

## License

MIT
