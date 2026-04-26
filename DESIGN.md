# MiDB - Android嵌入式数据库服务设计方案

## 一、项目概述

MiDB是一个运行在Android设备上的嵌入式数据库服务应用，通过HTTP协议提供统一的数据访问接口。支持两种主流嵌入式数据库引擎：
- **SQLite3**: 关系型数据库，支持完整SQL语法
- **LevelDB**: Google开发的键值存储引擎，高性能写入

### 核心功能
1. 提供RESTful API接口访问SQLite和LevelDB
2. 内置Web管理端，可通过浏览器管理数据
3. 轻量级HTTP服务器，无需外部依赖

## 二、技术架构

### 2.1 整体架构
```
┌─────────────────────────────────────────────────┐
│              Android Application                │
│  ┌─────────────────────────────────────────┐  │
│  │        NanoHTTPD Server (Port 8080)     │  │
│  │  ┌──────────┬──────────┬─────────────┐ │  │
│  │  │ SQLite   │ LevelDB  │  Admin Web  │ │  │
│  │  │ Handler  │ Handler  │  Interface   │ │  │
│  │  └──────────┴──────────┴─────────────┘ │  │
│  └─────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────┐  │
│  │      Database Layer                      │  │
│  │  ┌──────────────┐  ┌───────────────┐   │  │
│  │  │ SQLite 3     │  │ LevelDB       │   │  │
│  │  │ (Android     │  │ (JNI Wrapper) │   │  │
│  │  │  Built-in)  │  │                │   │  │
│  │  └──────────────┘  └───────────────┘   │  │
│  └─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 2.2 技术栈
| 组件 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| HTTP服务器 | NanoHTTPD | 2.3.1 | 轻量级嵌入式HTTP服务器 |
| 关系型数据库 | SQLite3 | Android内置 | Android系统原生支持 |
| 键值数据库 | LevelDB JNI | 1.19.0 | 通过JNI调用原生LevelDB |
| 开发语言 | Kotlin | 1.9+ | Android首选开发语言 |
| 管理端前端 | HTML5 + Vanilla JS | - | 无框架依赖，直接嵌入assets |

## 三、API接口设计

### 3.1 接口概览
| 接口路径 | 方法 | 功能描述 | 请求体 | 响应格式 |
|----------|------|----------|---------|----------|
| `/sqlite/execute` | POST | 执行SQL语句 | JSON | JSON |
| `/kv/get` | GET | 获取键值 | Query Param | JSON |
| `/kv/set` | POST | 设置键值 | JSON | JSON |

### 3.2 统一响应格式
```json
{
  "success": true,
  "data": null,
  "message": "",
  "rows_affected": 0
}
```

### 3.3 SQLite接口详细设计

#### POST `/sqlite/execute`
执行任意SQL语句（支持参数化查询）

**请求体:**
```json
{
  "sql": "SELECT * FROM users WHERE id = ? AND status = ?",
  "params": [1, "active"]
}
```

**参数说明:**
- `sql` (string, 必填): SQL语句，支持`?`占位符
- `params` (array, 可选): 参数数组，与SQL中的`?`一一对应

**成功响应 (SELECT):**
```json
{
  "success": true,
  "data": [
    {"id": 1, "name": "张三", "status": "active"},
    {"id": 2, "name": "李四", "status": "active"}
  ],
  "rows_affected": 0,
  "message": "Query executed successfully"
}
```

**成功响应 (INSERT/UPDATE/DELETE):**
```json
{
  "success": true,
  "data": null,
  "rows_affected": 1,
  "message": "1 row affected"
}
```

**错误响应:**
```json
{
  "success": false,
  "data": null,
  "rows_affected": 0,
  "message": "near \"SELEC\": syntax error"
}
```

**支持的SQL操作:**
- SELECT: 查询数据
- INSERT: 插入数据
- UPDATE: 更新数据
- DELETE: 删除数据
- CREATE TABLE: 创建表
- DROP TABLE: 删除表
- ALTER TABLE: 修改表结构
- PRAGMA: SQLite特定命令

### 3.4 LevelDB键值接口详细设计

#### GET `/kv/get`
获取指定键的值

**请求参数:**
```
GET /kv/get?key=user:1001
```

**成功响应 (存在):**
```json
{
  "success": true,
  "key": "user:1001",
  "value": "{\"name\":\"张三\",\"age\":25}",
  "exists": true
}
```

**成功响应 (不存在):**
```json
{
  "success": true,
  "key": "user:9999",
  "value": null,
  "exists": false
}
```

**错误响应:**
```json
{
  "success": false,
  "message": "Key cannot be empty"
}
```

#### POST `/kv/set`
设置键值对

**请求体:**
```json
{
  "key": "user:1001",
  "value": "{\"name\":\"张三\",\"age\":25}"
}
```

**参数说明:**
- `key` (string, 必填): 键，不能为空
- `value` (string, 必填): 值，支持任意字符串（建议JSON）

**成功响应:**
```json
{
  "success": true,
  "key": "user:1001",
  "message": "Key set successfully"
}
```

**错误响应:**
```json
{
  "success": false,
  "message": "Key and value are required"
}
```

#### DELETE `/kv/delete` (可选扩展)
删除指定键

**请求参数:**
```
DELETE /kv/delete?key=user:1001
```

**响应:**
```json
{
  "success": true,
  "key": "user:1001",
  "message": "Key deleted successfully"
}
```

## 四、数据库设计

### 4.1 SQLite数据库
- **数据库文件**: `midb.sqlite` (存储在应用私有目录)
- **路径**: `context.getDatabasePath("midb.sqlite")`
- **初始化**: 首次使用时自动创建
- **字符编码**: UTF-8
- **访问方式**: 通过Android SQLiteOpenHelper

**示例初始化表:**
```sql
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 4.2 LevelDB数据库
- **数据库目录**: `leveldb_data` (存储在应用私有目录)
- **路径**: `File(context.filesDir, "leveldb_data")`
- **创建选项**: `createIfMissing = true`
- **压缩**: 默认启用Snappy压缩（若支持）
- **访问方式**: 通过LevelDB JNI wrapper

**键命名规范建议:**
- 使用前缀区分数据类型: `user:1001`, `config:app_name`
- 避免使用特殊字符: `/ \ 0` 等

## 五、管理端Web界面设计

### 5.1 访问方式
```
http://<手机IP地址>:8080/admin/
```

### 5.2 功能模块

#### 5.2.1 顶部导航栏
- **MiDB管理后台** (Logo/标题)
- 连接状态 indicator (绿色=运行中)
- 设备IP地址显示

#### 5.2.2 SQLite管理面板
**SQL执行器:**
- 多行文本框输入SQL
- "执行"按钮
- "清空"按钮
- "常用SQL"下拉菜单 (预置常用语句):
  - `SELECT * FROM users`
  - `CREATE TABLE ...`
  - `SHOW TABLES` (PRAGMA)

**查询结果展示:**
- 表格形式展示SELECT结果
- 显示受影响的行数 (INSERT/UPDATE/DELETE)
- 错误信息红色提示

**快捷操作:**
- 📋 显示所有表
- 🔍 查看表结构
- 📊 表数据量统计

#### 5.2.3 LevelDB管理面板
**键值浏览器:**
- 分页显示所有键值对
- 每页显示20条
- 支持前缀搜索: `user:`

**键值操作:**
- ➕ 添加键值对 (弹出表单)
- ✏️ 编辑值 (点击值可编辑)
- 🗑️ 删除键 (确认对话框)

**搜索功能:**
- 搜索框: 输入前缀或完整键名
- 实时过滤显示

#### 5.2.4 状态监控面板
- **SQLite数据库**:
  - 文件大小
  - 表数量
  - 最后修改时间
- **LevelDB数据库**:
  - 数据目录大小
  - 键值对总数
  - 磁盘使用量
- **HTTP服务**:
  - 启动时间
  - 总请求数
  - 活跃连接数

### 5.3 界面布局 (ASCII示意)
```
┌─────────────────────────────────────────────────────┐
│  🗄️ MiDB管理后台          [运行中] 192.168.1.100 │
├─────────────────────────────────────────────────────┤
│ [SQLite] [LevelDB] [状态监控]                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│ SQL执行器                              [执行] [清空]│
│ ┌───────────────────────────────────────────────┐  │
│ │ SELECT * FROM users WHERE age > ?             │  │
│ │                                               │  │
│ └───────────────────────────────────────────────┘  │
│ 参数: [25               ]                          │
│                                                     │
│ 查询结果 (3 rows):                                  │
│ ┌────┬──────┬─────────────┬─────────────────┐    │
│ │ id │ name │ email       │ created_at       │    │
│ ├────┼──────┼─────────────┼─────────────────┤    │
│ │ 1  │ 张三 │ z@test.com │ 2026-01-01      │    │
│ │ 2  │ 李四 │ l@test.com │ 2026-01-02      │    │
│ └────┴──────┴─────────────┴─────────────────┘    │
│                                                     │
│ 快捷操作: [显示所有表] [查看表结构] [数据统计]     │
└─────────────────────────────────────────────────────┘
```

### 5.4 技术实现
- **前端技术**: HTML5 + CSS3 + Vanilla JavaScript
- **CSS框架**: 无 (手写简洁样式)
- **AJAX**: 使用Fetch API
- **文件位置**: `app/src/main/assets/admin/`
  - `index.html` - 主页面
  - `css/style.css` - 样式
  - `js/app.js` - 交互逻辑

## 六、项目实施步骤

### 阶段一：基础框架搭建 (1-2小时)
1. 创建Android项目 (Empty Activity, Kotlin)
2. 配置 `build.gradle` 添加依赖
3. 创建包结构:
   - `service` - 数据库服务
   - `server` - HTTP服务器
   - `model` - 数据模型
4. 配置AndroidManifest权限

### 阶段二：数据库服务层 (2-3小时)
1. 实现 `SQLiteService.kt`
   - 初始化数据库
   - 执行SQL方法
   - 参数化查询
2. 实现 `LevelDBService.kt`
   - 打开/关闭数据库
   - get/set/delete方法
   - 遍历键值对
3. 实现 `DatabaseManager.kt`
   - 统一管理两种数据库
   - 提供单例访问

### 阶段三：HTTP服务器 (2-3小时)
1. 实现 `MidbHttpServer.kt`
   - 继承NanoHTTPD
   - 路由分发逻辑
2. 实现API Handler:
   - `SQLiteApiHandler.kt` - 处理 `/sqlite/execute`
   - `KVApiHandler.kt` - 处理 `/kv/*`
3. JSON序列化/反序列化
   - 使用Kotlinx.serialization或手动解析

### 阶段四：管理端前端 (3-4小时)
1. 设计HTML结构
2. 编写CSS样式 (响应式设计)
3. 实现JavaScript交互:
   - SQLite SQL执行
   - LevelDB键值管理
   - 状态监控刷新
4. 将文件放入 `assets/admin/`

### 阶段五：MainActivity集成 (1小时)
1. 设计简单UI:
   - 启动/停止按钮
   - 显示IP地址
   - 打开管理端按钮
2. 实现服务启停逻辑
3. 获取设备IP并显示
4. 考虑使用Foreground Service保活

### 阶段六：测试与优化 (1-2小时)
1. 真机测试所有API接口
2. 浏览器测试管理端功能
3. 错误处理完善
4. 性能优化 (如大量数据查询)

## 七、依赖配置

### 7.1 项目级 `build.gradle`
```gradle
// 无需特殊配置
```

### 7.2 模块级 `build.gradle` (app)
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.midb'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.midb"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildTypes {
        release {
            minifyEnabled false
        }
    }
    
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    
    // NanoHTTPD - 嵌入式HTTP服务器
    implementation 'org.nanohttpd:nanohttpd:2.3.1'
    
    // LevelDB JNI Wrapper (包含原生so库)
    implementation 'com.github.hf:leveldb:1.19.0@aar'
}

// 如果需要从jitpack加载LevelDB
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url 'https://dl.bintray.com/stojan/android' }
    }
}
```

### 7.3 权限配置 `AndroidManifest.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- 网络访问权限 (用于HTTP服务器) -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <!-- 获取WiFi状态 (用于显示IP地址) -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    
    <!-- 前台服务权限 (Android 9+) -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    
    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.MiDB">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- 可选: 前台服务用于保活 -->
        <service
            android:name=".service.DatabaseService"
            android:enabled="true"
            android:exported="false" />
            
    </application>
</manifest>
```

## 八、关键代码结构

### 8.1 数据模型 `model/ApiResponse.kt`
```kotlin
data class ApiResponse(
    val success: Boolean,
    val data: Any? = null,
    val message: String = "",
    val rows_affected: Int = 0
)

data class SQLiteRequest(
    val sql: String,
    val params: List<Any>? = null
)

data class KVRequest(
    val key: String,
    val value: String
)
```

### 8.2 SQLite服务 `service/SQLiteService.kt`
```kotlin
class SQLiteService(context: Context) {
    private val dbHelper = object : SQLiteOpenHelper(context, "midb.sqlite", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            // 可选: 创建默认表
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
    
    fun execute(sql: String, params: Array<Any?>? = null): ApiResponse {
        return try {
            val db = dbHelper.writableDatabase
            // 判断SQL类型 (SELECT vs 其他)
            if (sql.trim().uppercase().startsWith("SELECT")) {
                // 查询操作
                val cursor = if (params != null) {
                    db.rawQuery(sql, params.map { it?.toString() ?: "" }.toTypedArray())
                } else {
                    db.rawQuery(sql, null)
                }
                // 解析cursor为List<Map>
                // ...
            } else {
                // 更新操作
                if (params != null) {
                    db.execSQL(sql, params)
                } else {
                    db.execSQL(sql)
                }
                // 返回受影响的行数
            }
        } catch (e: Exception) {
            ApiResponse(false, message = e.message ?: "Unknown error")
        }
    }
}
```

### 8.3 LevelDB服务 `service/LevelDBService.kt`
```kotlin
class LevelDBService(context: Context) {
    private val dbPath = File(context.filesDir, "leveldb_data").absolutePath
    private var levelDB: LevelDB? = null
    
    fun open() {
        levelDB = LevelDB.open(dbPath, LevelDB.configure().createIfMissing(true))
    }
    
    fun get(key: String): String? {
        return levelDB?.get(key.toByteArray())?.let { String(it) }
    }
    
    fun set(key: String, value: String) {
        levelDB?.put(key.toByteArray(), value.toByteArray())
    }
    
    fun close() {
        levelDB?.close()
    }
}
```

### 8.4 HTTP服务器路由 `server/MidbHttpServer.kt`
```kotlin
class MidbHttpServer(port: Int, private val context: Context) : NanoHTTPD(port) {
    
    private val sqliteService = SQLiteService(context)
    private val levelDBService = LevelDBService(context).apply { open() }
    
    override fun serve(session: IHTTPSession): Response {
        return when {
            session.uri.startsWith("/sqlite/execute") -> handleSQLite(session)
            session.uri.startsWith("/kv/get") -> handleKVGet(session)
            session.uri.startsWith("/kv/set") -> handleKVSet(session)
            session.uri.startsWith("/admin/") -> serveAdminPage(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found")
        }
    }
    
    private fun handleSQLite(session: IHTTPSession): Response {
        // 解析POST body
        // 调用sqliteService.execute()
        // 返回JSON响应
    }
    
    private fun handleKVGet(session: IHTTPSession): Response {
        // 从Query参数获取key
        // 调用levelDBService.get()
        // 返回JSON响应
    }
    
    private fun handleKVSet(session: IHTTPSession): Response {
        // 解析POST body
        // 调用levelDBService.set()
        // 返回JSON响应
    }
    
    private fun serveAdminPage(session: IHTTPSession): Response {
        // 从assets读取HTML文件
        // 返回text/html响应
    }
}
```

## 九、安全性考虑

### 9.1 当前版本 (本地使用)
- ✅ 仅在本地网络使用
- ✅ 无身份验证 (后续可添加)
- ✅ 数据存储在应用私有目录

### 9.2 建议的安全措施
1. **添加Token验证** (可选):
   - 所有API请求需携带token参数
   - 管理端登录页面

2. **SQL注入防护**:
   - 使用参数化查询 (`?` 占位符)
   - 禁止字符串拼接SQL

3. **输入验证**:
   - 限制SQL长度 (防止DoS)
   - 检查key/value长度
   - 过滤危险SQL (如`DROP DATABASE`)

4. **网络隔离**:
   - 默认只监听127.0.0.1 (仅本机访问)
   - 可选监听0.0.0.0 (局域网访问)

## 十、测试计划

### 10.1 单元测试
- SQLiteService: SQL执行、参数化查询
- LevelDBService: get/set/delete操作
- API Handler: JSON解析、响应格式

### 10.2 集成测试
1. **API测试** (使用curl或Postman):
   ```bash
   # SQLite查询
   curl -X POST http://192.168.1.100:8080/sqlite/execute \
     -H "Content-Type: application/json" \
     -d '{"sql":"SELECT * FROM users"}'
   
   # LevelDB获取
   curl http://192.168.1.100:8080/kv/get?key=test
   
   # LevelDB设置
   curl -X POST http://192.168.1.100:8080/kv/set \
     -H "Content-Type: application/json" \
     -d '{"key":"test","value":"hello"}'
   ```

2. **管理端测试**:
   - 浏览器访问 `http://192.168.1.100:8080/admin/`
   - 测试SQL执行器
   - 测试键值对增删改查
   - 测试响应式布局

### 10.3 兼容性测试
- Android 7.0 (API 24) - 最低支持版本
- Android 14 (API 34) - 最新版本
- 不同厂商设备 (华为、小米、三星等)

## 十一、后续扩展方向

### 11.1 功能扩展
- [ ] 文件上传/下载接口
- [ ] WebSocket实时数据推送
- [ ] 数据库备份/恢复
- [ ] 多数据库实例支持
- [ ] SQL执行历史记录

### 11.2 性能优化
- [ ] 连接池管理
- [ ] 查询缓存
- [ ] 大数据分页查询
- [ ] LevelDB批量写入

### 11.3 安全增强
- [ ] Token/Bearer认证
- [ ] HTTPS支持 (SSL/TLS)
- [ ] IP白名单
- [ ] 操作日志记录

### 11.4 管理端增强
- [ ] 数据可视化图表
- [ ] SQL语法高亮
- [ ] 导出CSV/JSON
- [ ] 数据库性能监控

## 十二、注意事项

1. **LevelDB NDK兼容性**:
   - 确保目标设备支持armeabi-v7a或arm64-v8a
   - x86模拟器可能需要额外配置

2. **SQLite版本**:
   - Android系统内置SQLite版本可能较旧
   - 如需新特性，可使用SQLite Android Bindings

3. **后台服务保活**:
   - Android 8+限制后台服务
   - 建议使用Foreground Service + 通知

4. **文件权限**:
   - 应用私有目录无需额外权限
   - 如需导入导出到SD卡，需申请存储权限

5. **端口占用**:
   - 默认使用8080端口
   - 如被占用，需动态选择可用端口

---

## 附录：快速启动检查清单

- [ ] 创建Android项目 (Kotlin)
- [ ] 添加NanoHTTPD依赖
- [ ] 添加LevelDB依赖
- [ ] 配置网络权限
- [ ] 实现SQLiteService
- [ ] 实现LevelDBService
- [ ] 实现HTTP服务器路由
- [ ] 实现3个API接口
- [ ] 创建管理端HTML页面
- [ ] 测试API接口
- [ ] 测试管理端功能
- [ ] 真机部署验证

---

**文档版本**: v1.0  
**创建日期**: 2026-04-27  
**最后更新**: 2026-04-27  
**作者**: MiDB Design Team
