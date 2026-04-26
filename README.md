# MiDB - Android Database Service

[![Build APK](https://github.com/yourusername/midb/actions/workflows/build.yml/badge.svg)](https://github.com/yourusername/midb/actions/workflows/build.yml)

MiDB是一个运行在Android设备上的嵌入式数据库服务应用，通过HTTP协议提供统一的数据访问接口。

## 功能特性

- **SQLite3**: 支持完整SQL语法的关係型数据库
- **LevelDB**: Google开发的高性能键值存储引擎  
- **RESTful API**: 提供标准化的HTTP接口
- **Web管理端**: 通过浏览器管理数据库

## API接口

### SQLite接口
- `POST /sqlite/execute` - 执行SQL语句
```json
{"sql": "SELECT * FROM users WHERE id = ?", "params": ["1"]}
```

### LevelDB接口
- `GET /kv/get?key=<key>` - 获取键值
- `POST /kv/set` - 设置键值
```json
{"key": "user:1001", "value": "data"}
```

## 通过GitHub Actions构建

本项目使用GitHub Actions自动构建APK，无需本地构建环境。

### 构建步骤

1. **Fork或克隆此仓库到你的GitHub账号**

2. **推送代码触发构建**
   ```bash
   git add .
   git commit -m "Initial commit"
   git push origin main
   ```

3. **查看构建状态**
   - 进入GitHub仓库的Actions标签页
   - 查看Build Android APK工作流
   - 等待构建完成（约5-10分钟）

4. **下载构建产物**
   - 构建完成后，在Actions页面底部找到Artifacts
   - 下载 `midb-debug-apk`
   - 解压得到 `app-debug.apk`

5. **安装到设备**
   ```bash
   adb install app-debug.apk
   ```

### 构建配置

- **触发器**: push到main/master分支，或提交PR
- **环境**: Ubuntu Latest + JDK 17
- **输出**: Debug版APK（位于Artifacts）

## 手动测试APK

### 1. 安装应用
将构建好的APK安装到Android设备（需要Android 7.0+）

### 2. 启动服务
- 打开MiDB应用
- 点击"启动服务"按钮
- 查看显示的IP地址

### 3. 访问管理端
在浏览器访问: `http://设备IP:8080/admin/`

### 4. 测试API
```bash
# SQLite查询
curl -X POST http://设备IP:8080/sqlite/execute \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT sqlite_version()"}'

# LevelDB设置
curl -X POST http://设备IP:8080/kv/set \
  -H "Content-Type: application/json" \
  -d '{"key":"test","value":"hello"}'

# LevelDB获取
curl http://设备IP:8080/kv/get?key=test
```

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/midb/
│   │   ├── MainActivity.kt          # 主界面
│   │   ├── service/                # 数据库服务
│   │   └── server/                 # HTTP服务器
│   ├── assets/admin/               # 管理端前端
│   └── AndroidManifest.xml
```

## 技术栈

- **HTTP服务器**: NanoHTTPD 2.3.1
- **关系型数据库**: SQLite3 (Android内置)
- **键值数据库**: LevelDB (JNI)
- **开发语言**: Kotlin 1.9+

## License

MIT License
