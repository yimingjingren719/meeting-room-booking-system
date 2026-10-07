# 基于 Web 的会议室预约系统

项目按照阶段报告中的设计方案实现，采用前后端分离架构：

- 前端：Vue 3、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts
- 后端：Java 17+、Spring Boot 3、Spring Security、JWT、MyBatis-Plus、Flyway
- 数据：本地开发默认使用 H2 文件数据库，生产配置支持 MySQL 8.0

## 已实现范围

- 普通用户、审批管理员、系统管理员三角色登录与权限控制
- 会议室条件筛选和按日期查看可用时段
- 预约创建、冲突检测、取消预约、审批通过或驳回
- 已通过预约的二次审批冲突防护和完整不可用时段展示
- 站内通知、未读消息和预约状态追踪
- 已通过预约开始前 30 分钟自动提醒，避免错过会议
- 个人资料维护、密码修改和个人预约权限提示
- 管理员会议室新增、编辑、停用、维护时段和有效预约保护
- 直接为现有用户授予或撤销审批管理员权限，并管理会议室预约权限
- 常用会议室收藏和按人数、设备、时间自动推荐
- 管理员统计看板、人员与权限管理和审计记录
- Flyway 数据库迁移和演示数据自动初始化

## 本地启动

后端使用 H2 文件库，首次启动会自动建表并写入演示数据。

```powershell
cd backend
mvn spring-boot:run
```

如果 Windows 项目路径包含中文，`pom.xml` 会把 Maven 构建产物自动放到
`%USERPROFILE%\.meeting-room-booking-system\backend-target`，应用数据仍保存在
`backend/data`。这是为了兼容 JDK 23 读取中文绝对路径 `@argfile` 的行为，
不需要手动设置环境变量。

前端：

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。Vite 会把 `/api` 请求代理到
`http://localhost:8080`。

演示账号：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 普通用户 | `user` | `user123` |
| 审批管理员 | `approver` | `approver123` |
| 系统管理员 | `admin` | `admin123` |

## MySQL 配置

启动 MySQL 8.0 后设置以下环境变量：

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_URL="jdbc:mysql://localhost:3306/meeting_room?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="meeting"
$env:DB_PASSWORD="change-me"
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
mvn spring-boot:run
```

`backend/src/main/resources/db/migration` 下的脚本同时兼容 H2 的 MySQL
模式和 MySQL 8.0。

## 项目结构

```text
meeting-room-booking-system/
├─ backend/                         Spring Boot 后端
│  ├─ src/main/java/                后端 Java 源码
│  ├─ src/main/resources/           应用配置和 Flyway 脚本
│  ├─ src/test/                     集成测试
│  ├─ Dockerfile                    后端容器镜像
│  └─ pom.xml                       Maven 依赖与构建配置
├─ frontend/                        Vue 3 前端
│  ├─ public/                       图标和本地会议室图片
│  ├─ src/api/                      Axios 接口封装
│  ├─ src/components/               通用组件
│  ├─ src/layouts/                  登录后主布局
│  ├─ src/router/                   路由与权限守卫
│  ├─ src/stores/                   Pinia 登录状态
│  ├─ src/utils/                    格式化工具
│  ├─ src/views/                    业务页面
│  ├─ Dockerfile                    前端容器镜像
│  ├─ nginx.conf                    Nginx 托管和 API 反向代理
│  ├─ vite.config.js                Vite 开发代理与构建配置
│  └─ package.json                  前端依赖与脚本
├─ docker-compose.yml               前端、后端和 MySQL 编排
├─ .env.example                     环境变量示例
└─ README.md                        项目说明
```

## 文件说明

### 根目录文件

| 文件 | 说明 |
| --- | --- |
| `.env.example` | MySQL、数据库账号密码和 JWT 密钥的环境变量示例，可用于创建本地 `.env`。 |
| `.gitignore` | 忽略 IDE、前端依赖、构建产物、H2 数据文件、日志和 npm 缓存。 |
| `README.md` | 项目总览、启动步骤、账号、配置、验证方式及文件职责说明。 |
| `docker-compose.yml` | 编排 MySQL 8.4、Spring Boot 后端和 Nginx 前端，支持一键部署。 |

### 后端构建与配置文件

| 文件 | 说明 |
| --- | --- |
| `backend/pom.xml` | Maven 项目配置，声明 Spring Boot、Security、MyBatis-Plus、JWT、Flyway、MySQL、H2 和测试依赖；Windows 下将构建目录放到纯 ASCII 用户目录，避免中文路径导致 `spring-boot:run` 类路径乱码。 |
| `backend/Dockerfile` | 两阶段构建后端镜像，先使用 Maven 打包，再使用 Java 17 JRE 运行可执行 JAR。 |
| `backend/src/main/resources/application.yml` | 默认本地配置，使用 H2 文件数据库，配置 JWT、MyBatis-Plus、Flyway、H2 控制台和 CORS。 |
| `backend/src/main/resources/application-mysql.yml` | MySQL 专用配置，从环境变量读取数据库地址、用户名和密码。 |
| `backend/src/main/resources/db/migration/V1__init.sql` | 初始化用户、角色、楼栋、会议室、设备、预约、审批、通知和审计日志表及索引。 |
| `backend/src/main/resources/db/migration/V2__local_room_images.sql` | 将演示会议室的图片地址更新为前端本地静态资源。 |
| `backend/src/main/resources/db/migration/V3__admin_and_user_features.sql` | 增加用户预约禁用字段、禁用原因和常用会议室收藏表。 |
| `backend/src/main/resources/db/migration/V4__booking_reminder.sql` | 为预约记录增加开始前提醒发送标记。 |

### 后端启动、通用与配置类

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/MeetingRoomApplication.java` | Spring Boot 启动类，同时开启定时任务调度。 |
| `backend/src/main/java/com/usst/meetingroom/common/ApiResponse.java` | 统一接口返回结构，包含业务码、提示信息和数据体。 |
| `backend/src/main/java/com/usst/meetingroom/common/BusinessException.java` | 业务异常类型，可携带业务状态码和可读错误信息。 |
| `backend/src/main/java/com/usst/meetingroom/common/GlobalExceptionHandler.java` | 全局异常处理，统一处理参数校验、权限和未知异常。 |
| `backend/src/main/java/com/usst/meetingroom/common/StatusText.java` | 将预约状态枚举转换为中文显示文本。 |
| `backend/src/main/java/com/usst/meetingroom/config/SecurityConfig.java` | Spring Security 主配置，关闭 CSRF、启用 CORS、注册 JWT 过滤器并设置无状态会话。 |
| `backend/src/main/java/com/usst/meetingroom/config/DataInitializer.java` | 首次启动时创建三类角色、演示用户、楼栋、会议室、设备、预约、审批和通知数据。 |

### 后端领域模型

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/domain/User.java` | `sys_user` 用户实体，包含账号、密码、姓名、联系方式和状态。 |
| `backend/src/main/java/com/usst/meetingroom/domain/Role.java` | `sys_role` 角色实体，对应普通用户、审批管理员和系统管理员。 |
| `backend/src/main/java/com/usst/meetingroom/domain/UserRole.java` | `sys_user_role` 用户与角色多对多关联实体。 |
| `backend/src/main/java/com/usst/meetingroom/domain/UserFavoriteRoom.java` | `user_favorite_room` 用户常用会议室收藏实体。 |
| `backend/src/main/java/com/usst/meetingroom/domain/Building.java` | `building` 楼栋实体，保存校区、地址和启用状态。 |
| `backend/src/main/java/com/usst/meetingroom/domain/Room.java` | `room` 会议室实体，保存容量、位置、开放时间、状态和图片。 |
| `backend/src/main/java/com/usst/meetingroom/domain/Equipment.java` | `equipment` 设备实体，用于会议室设备筛选和展示。 |
| `backend/src/main/java/com/usst/meetingroom/domain/RoomEquipment.java` | `room_equipment` 会议室与设备关联实体，并保存数量。 |
| `backend/src/main/java/com/usst/meetingroom/domain/RoomUnavailable.java` | `room_unavailable` 维护或临时封闭时段实体。 |
| `backend/src/main/java/com/usst/meetingroom/domain/Booking.java` | `booking` 预约主实体，保存申请人、会议室、预约状态和提醒发送标记。 |
| `backend/src/main/java/com/usst/meetingroom/domain/BookingApproval.java` | `booking_approval` 审批记录实体，保存审批人、结果和意见。 |
| `backend/src/main/java/com/usst/meetingroom/domain/Notification.java` | `notification` 站内通知实体，保存接收人、标题、内容和已读状态。 |
| `backend/src/main/java/com/usst/meetingroom/domain/AuditLog.java` | `audit_log` 审计日志实体，记录关键操作、对象、结果和详情。 |
| `backend/src/main/java/com/usst/meetingroom/domain/BookingStatus.java` | 预约状态枚举：待审批、已通过、已驳回、已取消、已完成和已过期。 |
| `backend/src/main/java/com/usst/meetingroom/domain/RoomStatus.java` | 会议室状态枚举：可预约、维护中和已停用。 |

### 后端 DTO

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/dto/LoginRequest.java` | 登录请求参数，校验用户名和密码非空。 |
| `backend/src/main/java/com/usst/meetingroom/dto/RegisterRequest.java` | 注册请求参数，校验用户名格式、密码长度、姓名和邮箱。 |
| `backend/src/main/java/com/usst/meetingroom/dto/AuthResponse.java` | 登录或注册成功后返回 JWT 和用户资料。 |
| `backend/src/main/java/com/usst/meetingroom/dto/UserProfile.java` | 对外用户资料，包括账号、姓名、联系方式和角色列表。 |
| `backend/src/main/java/com/usst/meetingroom/dto/RoomResponse.java` | 会议室详情响应，包含楼栋、容量、设备、状态和可用时段。 |
| `backend/src/main/java/com/usst/meetingroom/dto/AvailabilitySlot.java` | 单个可用时段响应，包含起止时间、状态码和中文状态。 |
| `backend/src/main/java/com/usst/meetingroom/dto/BookingCreateRequest.java` | 新建预约参数，校验会议室、主题、人数、开始时间和结束时间。 |
| `backend/src/main/java/com/usst/meetingroom/dto/BookingView.java` | 预约列表和详情响应，合并申请人、会议室、审批意见和状态文本。 |
| `backend/src/main/java/com/usst/meetingroom/dto/ApprovalRequest.java` | 审批请求参数，包含通过或驳回结果及审批意见。 |
| `backend/src/main/java/com/usst/meetingroom/dto/CancelBookingRequest.java` | 取消预约请求，包含可选的取消原因。 |
| `backend/src/main/java/com/usst/meetingroom/dto/NotificationView.java` | 通知响应，包含关联预约、标题、内容、已读状态和时间。 |
| `backend/src/main/java/com/usst/meetingroom/dto/StatsResponse.java` | 统计看板响应，包含指标、会议室排行、状态分布和七日趋势。 |
| `backend/src/main/java/com/usst/meetingroom/dto/RoomStatusRequest.java` | 修改会议室状态的请求参数。 |
| `backend/src/main/java/com/usst/meetingroom/dto/UnavailableRequest.java` | 创建维护时段的请求参数，包含会议室、起止时间和原因。 |
| `backend/src/main/java/com/usst/meetingroom/dto/AuditLogView.java` | 审计日志响应，包含操作人显示名和操作详情。 |
| `backend/src/main/java/com/usst/meetingroom/dto/UnavailableView.java` | 维护时段响应，包含会议室名称、起止时间、原因和创建时间。 |
| `backend/src/main/java/com/usst/meetingroom/dto/UpdateProfileRequest.java` | 用户修改姓名、邮箱和手机号的请求参数。 |
| `backend/src/main/java/com/usst/meetingroom/dto/ChangePasswordRequest.java` | 用户修改密码请求，校验当前密码和新密码。 |
| `backend/src/main/java/com/usst/meetingroom/dto/AdminUserView.java` | 管理员人员列表响应，包含角色、账号状态和预约权限。 |
| `backend/src/main/java/com/usst/meetingroom/dto/BookingPermissionRequest.java` | 设置用户是否允许预约及禁止原因。 |
| `backend/src/main/java/com/usst/meetingroom/dto/RoomUpsertRequest.java` | 新增或编辑会议室时的楼栋、容量、时间、状态和设备参数。 |

### 后端 Mapper

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/mapper/UserMapper.java` | 用户表基础增删改查，继承 MyBatis-Plus `BaseMapper`。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/RoleMapper.java` | 查询用户角色代码、审批管理员 ID，并访问角色表。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/UserRoleMapper.java` | 用户角色关联表写入和查询。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/UserFavoriteRoomMapper.java` | 用户常用会议室收藏关系的数据访问。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/BuildingMapper.java` | 楼栋表基础数据访问。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/RoomMapper.java` | 会议室数据访问，并提供预约事务使用的行锁查询。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/EquipmentMapper.java` | 设备表访问，并查询指定会议室绑定的设备。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/RoomEquipmentMapper.java` | 会议室设备关联表的写入和筛选查询。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/RoomUnavailableMapper.java` | 维护时段访问，并提供时间重叠数量查询。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/BookingMapper.java` | 预约数据访问，提供并发冲突检查和预约行锁查询。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/BookingApprovalMapper.java` | 审批结果表基础数据访问。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/NotificationMapper.java` | 站内通知表基础数据访问。 |
| `backend/src/main/java/com/usst/meetingroom/mapper/AuditLogMapper.java` | 审计日志表基础数据访问。 |

### 后端安全组件

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/security/JwtService.java` | 生成、解析和校验 JWT，并写入用户 ID 与角色信息。 |
| `backend/src/main/java/com/usst/meetingroom/security/JwtAuthenticationFilter.java` | 从 `Authorization` 请求头读取 JWT，并建立 Spring Security 认证上下文。 |
| `backend/src/main/java/com/usst/meetingroom/security/UserPrincipal.java` | Spring Security 用户主体，将数据库用户和角色转换为权限集合。 |
| `backend/src/main/java/com/usst/meetingroom/security/RestAuthenticationEntryPoint.java` | 未登录或令牌失效时返回统一 JSON 401 响应。 |
| `backend/src/main/java/com/usst/meetingroom/security/RestAccessDeniedHandler.java` | 已登录但权限不足时返回统一 JSON 403 响应。 |
| `backend/src/main/java/com/usst/meetingroom/security/SecurityErrorWriter.java` | 将安全认证和授权异常写入标准接口响应。 |

### 后端业务服务

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/service/UserService.java` | 用户注册、资料维护、密码修改、会议室预约权限、人员列表以及审批管理员权限授予、撤销和账号停用管理。 |
| `backend/src/main/java/com/usst/meetingroom/service/RoomService.java` | 会议室筛选、设备过滤、可用时段、收藏和智能推荐。 |
| `backend/src/main/java/com/usst/meetingroom/service/BookingService.java` | 预约创建、冲突检测、审批、取消、通知、审计和状态转换核心服务。 |
| `backend/src/main/java/com/usst/meetingroom/service/AuditService.java` | 统一写入登录、预约和资源维护等关键操作的审计日志。 |
| `backend/src/main/java/com/usst/meetingroom/service/NotificationService.java` | 通知列表、未读数量、单条已读和全部已读处理。 |
| `backend/src/main/java/com/usst/meetingroom/service/StatsService.java` | 汇总预约总量、待审批数量、通过率、会议室排行和趋势数据。 |
| `backend/src/main/java/com/usst/meetingroom/service/AdminService.java` | 会议室新增、编辑、停用、维护时段和审计查询，并阻止维护或停用覆盖有效预约。 |
| `backend/src/main/java/com/usst/meetingroom/service/ScheduledTasks.java` | 定时更新过期预约状态，并在会议开始前发送提醒。 |

### 后端控制器

| 文件 | 说明 |
| --- | --- |
| `backend/src/main/java/com/usst/meetingroom/controller/AuthController.java` | 提供登录、注册和当前用户资料接口。 |
| `backend/src/main/java/com/usst/meetingroom/controller/RoomController.java` | 提供会议室列表、详情、可用时段、楼栋和设备接口。 |
| `backend/src/main/java/com/usst/meetingroom/controller/BookingController.java` | 提供创建预约、我的预约和取消预约接口。 |
| `backend/src/main/java/com/usst/meetingroom/controller/NotificationController.java` | 提供通知列表、未读数量和已读操作接口。 |
| `backend/src/main/java/com/usst/meetingroom/controller/AdminController.java` | 提供审批列表、审批操作、统计、资源维护和审计日志接口。 |
| `backend/src/main/java/com/usst/meetingroom/controller/HealthController.java` | 提供无需登录的系统健康检查接口。 |

### 后端测试

| 文件 | 说明 |
| --- | --- |
| `backend/src/test/resources/application-test.yml` | 测试环境配置，使用独立 H2 内存数据库，避免污染本地演示数据。 |
| `backend/src/test/java/com/usst/meetingroom/BookingFlowIntegrationTest.java` | 集成测试，覆盖重叠预约拒绝、预约提交、审批、取消、通知、维护冲突和会议室状态保护。 |

### 前端构建、部署与静态资源

| 文件 | 说明 |
| --- | --- |
| `frontend/package.json` | 声明 Vue、Element Plus、Pinia、Vue Router、Axios、ECharts 依赖及开发、构建和 ESLint 脚本。 |
| `frontend/package-lock.json` | 锁定前端依赖的精确版本，保证不同环境安装结果一致。 |
| `frontend/eslint.config.js` | ESLint Flat Config，启用 JavaScript 和 Vue 基础规则。 |
| `frontend/vite.config.js` | 配置 Vue 插件、开发端口、`/api` 后端代理和构建参数。 |
| `frontend/index.html` | 前端 HTML 入口，配置中文语言、移动端视口、页面描述和 favicon。 |
| `frontend/Dockerfile` | 两阶段构建前端镜像，使用 Node 构建后由 Nginx 托管静态文件。 |
| `frontend/nginx.conf` | 配置 SPA 路由回退，并将 `/api` 请求反向代理到后端容器。 |
| `frontend/public/favicon.svg` | 浏览器标签页图标。 |
| `frontend/public/rooms/room-301.jpg` | “301 学术研讨室”本地图片。 |
| `frontend/public/rooms/room-508.jpg` | “508 项目会议室”本地图片，也用于登录页背景。 |
| `frontend/public/rooms/room-hall.jpg` | “A 区多功能厅”本地图片。 |
| `frontend/public/rooms/room-collab.jpg` | “B 区协作室”本地图片。 |

### 前端入口、接口与状态

| 文件 | 说明 |
| --- | --- |
| `frontend/src/main.js` | 创建 Vue 应用，注册 Pinia、Vue Router、Element Plus 中文语言包和全局样式。 |
| `frontend/src/App.vue` | 根组件，仅承载路由出口。 |
| `frontend/src/styles.css` | 全局设计变量、通用布局、卡片、状态色、响应式规则和 Element Plus 微调。 |
| `frontend/src/api/http.js` | Axios 实例，统一添加 JWT、处理业务错误和 401 跳转登录。 |
| `frontend/src/api/index.js` | 按认证、会议室、预约、通知和后台管理分组的接口方法。 |
| `frontend/src/stores/auth.js` | Pinia 登录状态，持久化 Token 和用户资料，并提供角色判断。 |
| `frontend/src/router/index.js` | 路由表、登录守卫和基于角色的页面访问控制。 |
| `frontend/src/utils/format.js` | 日期、时间、日期时间和表单值格式化工具。 |

### 前端布局与通用组件

| 文件 | 说明 |
| --- | --- |
| `frontend/src/layouts/AppLayout.vue` | 登录后主布局，包含侧边导航、顶部栏、通知角标和用户菜单。 |
| `frontend/src/components/AuthShell.vue` | 登录注册共用外壳，包含品牌区、会议空间背景和表单插槽。 |
| `frontend/src/components/StatusTag.vue` | 统一渲染预约、会议室和可用时段状态标签。 |
| `frontend/src/components/ChartPanel.vue` | ECharts 通用容器，按需注册图表组件，并负责初始化、尺寸监听和响应式重绘。 |

### 前端业务页面

| 文件 | 说明 |
| --- | --- |
| `frontend/src/views/LoginView.vue` | 登录页，提供演示账号快速填充和登录后重定向。 |
| `frontend/src/views/RegisterView.vue` | 注册页，校验用户名、密码一致性、邮箱和手机号。 |
| `frontend/src/views/DashboardView.vue` | 工作台，展示个人预约指标、近期预约、通知和管理员趋势图。 |
| `frontend/src/views/RoomsView.vue` | 会议室查询页，支持日期、楼栋、容量和设备筛选，并完成预约提交。 |
| `frontend/src/views/BookingsView.vue` | 我的预约页，按状态筛选、查看审批意见并取消预约。 |
| `frontend/src/views/ApprovalsView.vue` | 审批中心，展示待处理申请并通过或驳回预约。 |
| `frontend/src/views/AnalyticsView.vue` | 数据统计页，使用 ECharts 展示会议室使用排行、状态分布和预约趋势。 |
| `frontend/src/views/ResourcesView.vue` | 资源维护页，管理会议室状态、维护时段和设备目录。 |
| `frontend/src/views/AuditView.vue` | 审计日志页，搜索并展示关键操作、操作人、对象和结果。 |
| `frontend/src/views/NotificationsView.vue` | 通知页，支持全部/未读/已读筛选、单条已读和全部已读。 |
| `frontend/src/views/ProfileView.vue` | 个人资料页，支持修改联系方式、修改密码和查看预约权限。 |
| `frontend/src/views/PersonnelView.vue` | 人员权限页，为现有用户授予或撤销审批管理员权限，并管理预约权限。 |

## 验证

```powershell
cd backend
mvn test

cd ..\frontend
npm run lint
npm run build
```

集成测试覆盖预约冲突、审批、取消、开始前提醒、个人资料与密码、预约权限、审批权限授予与撤销、会议室增删改、收藏和智能推荐。
会议室图片保存在 `frontend/public/rooms`，不依赖外部图片服务。

