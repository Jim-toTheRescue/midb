# MiDB 测试验证指南

## 实施完成清单

### ✅ 已创建文件

#### 项目配置
- [x] `build.gradle` (项目级)
- [x] `app/build.gradle` (模块级)
- [x] `settings.gradle`
- [x] `gradle/wrapper/gradle-wrapper.properties`

#### 源代码
- [x] `MainActivity.kt` - 主界面
- [x] `model/ApiResponse.kt` - 数据模型
- [x] `service/DatabaseManager.kt` - 数据库管理
- [x] `service/SQLiteService.kt` - SQLite服务
- [x] `service/LevelDBService.kt` - LevelDB服务
- [x] `server/MidbHttpServer.kt` - HTTP服务器

#### 管理端前端
- [x] `assets/admin/index.html` - 主页面
- [x] `assets/admin/css/style.css` - 样式
- [x] `assets/admin/js/app.js` - 交互逻辑

#### 资源文件
- [x] `res/layout/activity_main.xml` - 主界面布局
- [x] `res/values/strings.xml` - 字符串资源
- [x] `res/values/styles.xml` - 主题样式

## 测试步骤

### 1. 在Android Studio中打开项目
```bash
# 进入项目目录
cd /Users/junfengyang/Documents/desk/midb

# 使用Android Studio打开此目录
```

### 2. 同步Gradle
- 等待Gradle同步完成
- 检查是否有依赖下载错误
- **注意**: LevelDB依赖包可能需要额外配置

### 3. 运行应用
1. 连接Android设备或启动模拟器
2. 点击Run按钮
3. 应用启动后，点击"启动服务"按钮
4. 查看显示的IP地址

### 4. 测试API接口

#### 测试SQLite接口
```bash
# 创建测试表
curl -X POST http://设备IP:8080/sqlite/execute \
  -H "Content-Type: application/json" \
  -d '{"sql":"CREATE TABLE IF NOT EXISTS test (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)"}'

# 插入数据
curl -X POST http://设备IP:8080/sqlite/execute \
  -H "Content-Type: application/json" \
  -d '{"sql":"INSERT INTO test (name) VALUES (?)", "params":["test1"]}'

# 查询数据
curl -X POST http://设备IP:8080/sqlite/execute \
  -H "Content-Type: application/json" \
  -d '{"sql":"SELECT * FROM test"}'
```

#### 测试LevelDB接口
```bash
# 设置键值
curl -X POST http://设备IP:8080/kv/set \
  -H "Content-Type: application/json" \
  -d '{"key":"user:1001","value":"张三"}'

# 获取键值
curl http://设备IP:8080/kv/get?key=user:1001
```

### 5. 测试管理端
在浏览器访问: `http://设备IP:8080/admin/`

检查功能:
- [ ] SQLite SQL执行器
- [ ] LevelDB键值管理
- [ ] 状态监控面板

## 预期结果

### API响应示例

**SQLite查询成功:**
```json
{
  "success": true,
  "data": [{"id": 1, "name": "test1"}],
  "message": "Query executed successfully",
  "rows_affected": 0
}
```

**LevelDB获取成功:**
```json
{
  "success": true,
  "key": "user:1001",
  "value": "张三",
  "exists": true
}
```

## 常见问题

### 1. LevelDB依赖找不到
**问题**: Gradle同步时提示找不到`com.github.hf:leveldb:1.19.0@aar`

**解决**: 
- 检查`build.gradle`中是否添加了`jitpack.io`仓库
- 或手动下载aar包放入`libs/`目录

### 2. 端口被占用
**问题**: 启动服务时提示端口8080被占用

**解决**: 修改`MidbHttpServer.kt`中的端口号，或释放8080端口

### 3. LevelDB初始化失败
**问题**: JNI错误，无法加载原生库

**解决**: 
- 确认设备架构是armeabi-v7a或arm64-v8a
- x86模拟器可能不支持

### 4. 管理端页面无法加载
**问题**: 访问`/admin/`显示404

**解决**: 检查`assets/admin/`目录下的文件是否正确放置

## 下一步

1. **功能测试**: 按照上述测试步骤验证所有功能
2. **错误处理**: 添加更完善的异常处理
3. **安全性**: 添加Token验证或IP白名单
4. **优化**: 添加连接池、查询缓存等

## 项目状态

- ✅ 基础架构完成
- ✅ 核心功能实现
- ✅ 管理端前端完成
- ⏳ 待Gradle同步验证
- ⏳ 待真机测试

---

**实施完成时间**: 2026-04-27  
**下一步**: 在Android Studio中打开项目并测试
