# 开发日志

1.依赖。在文件pom.xml中添加依赖：Spring MVC, Lombok, hutool, Knife4j, Spring AI Alibaba...

2.配置。编辑文件application.yml, 添加：Knife4j的配置，Spring配置：port，AI：Dashscope。

3.接口测试。测试swagger接口，在src下新建controller，新建HealthController.java，在浏览器输入.yml中的接口地址。

4.API_KEY。创建文件application-local.yml，存入API_KEY（需要自行去阿里百炼平台充值）。在.gitignore中添加文件名applicaiont-local.yml

5.实现多轮对话，持久化记忆。创建目录App, 创建文件LoveApp.java实现本地内容存储对话记忆，实现多轮对话doChat()。（注意阅读SpringAI官方文档）JUnit测试，在方法名上鼠标右键->generate->test。

6.实现日志拦截器。创建目录Advisor，创建LoggerAdvisor.java，实现流式和非流式2种方式。不要管用户用什么，都需要实现。在文件LoveApp.java中添加自定义LoggerAdvisor。

7.实现敏感词拦截器。创建SafeGuardAdvisor.java，实现自定义常见敏感词拦截。在文件LoveApp.java中添加SafeGuardAdvisor。pom.xml添加sensitive-word。

8.实现结构化数据输出。在pom.xml添加jsonschema-generator，在LoveApp.java中添加doChatWithReport()，生成结构化数据。JUnit测试，alt+enter，生成丢失的单元测试方法。

9.优化敏感词拦截器。实现从一刀切到指出具体敏感词，然后引导用户修改词句后重新提问。创建SafeGuardAdvisor.java。

10.使用Mybatis持久化记忆，落库Mysql。

①在pom.xml添加依赖mybatis-spring-boot-start, mysql-connector。

②配置.yml文件。

③在mysql数据库中建立表chat_memory，建立字段。

④创建实体类ChatMemoryEntity.java。

⑤创建ChatMemoryMapper.java。

⑥创建Mybatis XML映射文件, ChatMemoryMapper.xml。

⑦创建MyBatisChatMemoryRepository.java，实现 Spring AI 的 ChatMemoryRepository 接口，用 MyBatis 来存取数据。

⑧在LoveApp.java注入自定义的 MyBatis 记忆仓库。

⑨启动类添加MapperScan。

```
整体架构总结

用户输入 → ChatClient → MessageChatMemoryAdvisor

↓

MessageWindowChatMemory (窗口截断，最多10条)

↓

MyBatisChatMemoryRepository (自定义实现)

↓

ChatMemoryMapper (MyBatis)

↓

MySQL chat_memory 表
```

```
核心流程：

读取记忆：每次对话前，findByConversationId 从 MySQL 查出历史记录
窗口截断：MessageWindowChatMemory 只保留最近 10 条
写回记忆：对话完成后，saveAll 先删旧数据，再把当前窗口内的消息批量写入 MySQL
```

11.RAG知识库搭建，本地知识库实现。

①在pom.xml引入依赖spring-ai-markdown-document-reader。

②新建rag目录，创建LoveAppDocumentLoader.java实现文档的加载和读取。

③创建LoveAppVectorStoreConfig.java实现初始化向量数据库并保存文档。

④在LoveApp实现QuestionAnswerAdvisor问答拦截器，实现查询增强和关联。

⑤在LoveAppTest.java实现doChatWithRag()单元测试，提问一个文档内有回答的问题，查看相似度score。

```
文档搜集和切割    
↓   
向量转换和存储    
↓
切片过滤和检索
↓   
查询增强和关联
```

12.RAG知识库，Spring AI + 云知识库。

①在阿里云百炼平台创建好云知识库。

②创建LoveAppRagCloudAdvisorConfig.java，实现基于阿里云知识库服务的RAG增强Advisor。

③在LoveApp.java的doChatWithRag()中添加应用检索增强服务。

④在LoveAppTest.java中doChatWithRageTest()中进行单元测试。

13.RAG知识库ETL。

①创建MyTokenTextSpliter，实现基于Token的文本分割器。

②创建MyKeywordEnricher.java,实现基于AI的文档元信息增强器，为文档补充元信息。

14.基于PGVector实现向量存储，提高PostgreSQ存储和检索高维度向量数据的能力。

①开通阿里云Serverless版本数据库服务并创建数据库。

②在IDEA->data source and drivers配置好url和登录信息，测试连接成功。

③pom.xml引入spring-ai-vector-store-pgvector的依赖。

④在application-local.yml编写配置，建立数据库连接。

⑤创建PgVectorStoreConfig.java，初始化VectorStore，不需要Starter自动注入。

⑥在AIagentApplication.java中启动类排除掉自动加载DataSourceAutoConfiguration.class。

⑦创建测试类PgVectorStoreConfigTest.java。在Debug模式中，查看文档检索成功以及相似度分数score。IDEA右侧快捷栏，查看数据表中的数据。证明PGVector整合成功。可以用阿里云服务替换本地的VectorStore。

15.实现查询重写，规范化用户的提示词。

①创建QueryRewriter。

②在LoveApp.java中doChatWithRag()添加查询重写。③测试doChatWithRag()。

16.根据用户查询需求生成对应的Advisor，实现文档的检索过滤。

①创建LoveAppRagCustomAdvisorFactory。填写好过滤表达式，设置好相似度阈值和召回数。

②在LoveApp.java的Advisor应用CustomAdvisorFactory。

③测试doChatWithRag()。

④Debug查看Logger中advisor信息。断点在adviseCall()处。

17.实现上下文查询增强。

①创建LoveAppContextualQueryAugmenterFactory.java,禁止查询内容为空，提供错误信息提示，提供正确信息引导。

②在LoveAppRagCustomAdvisorFactory.java添加应用LoveAppContextualQueryAugmentrFactory。

③单元测试LoveAppTest.doChatWitRag()。

18.实现Agent工具调用，查询天气功能。

A.用户输入城市名，返回天气信息。

B.通过获取用户IP地址，自动返回天气信息。

①创建tools目录。

②在application.yml中启用工具调用日志。

②申请天气查询API接口，并在application-local.yml中填写。

③实现查询天气功能，创建WeatherTool.java，创建WeatherToolTest.java。

19.实现读取文件、保存文件工具。

①创建目录constant，创建文件常量接口FileConstant.java。

②创建文件操作工具类FileOperationTool.java。

③创建测试类FileOperationToolTest.java。

20.实现联网搜索工具。

①创建WebSearchTool.java。

②在application-local.yml配置apikey。

③创建WebSearchToolTest.java

21.实现网页抓取工具。

①在pom.xml引入jsoup依赖。

②创建WebScrapingTool.java。

③创建WebScrapingToolTest.java。

22.实现终端操作工具。

①创建TerminalOperationTool.java。

②创建TerminalOperationToolTest.java。

23.实现资源下载工具。

①创建ResourceDownloadTool.java。

②创建ResourceDownloadToolTest.java。

24.实现PDF生成工具。

①在pom.xml引入itextpdf。

②创建PDFGenerationTool.java。

③创建PDFGenerationToolTest.java。

25.实现集中注册工具，让Agent自主决定使用哪些工具。

①创建ToolRegistration.java，通过ToolCallback保存工具对象。

②在LoveApp.java中创建doChatWithTools()绑定所有已经注册的工具。

③单元测试，创建doChatWithToolsTest()。

26.优化pdf生成工具，实现彩色emoji插入显示。

①下载.ttf字体文件包到Resource/font目录。

②重构PDFGenrationTool.java。

③单元测试，在PDFGenerationToolTest.java编写含有emoji元素的测试用例。

27.实现用户查询工具。

①创建UserQueryTool.java。

②创建UserQueryToolTest.java。

28.接入MCP协议，实现图片搜索服务，本地开发。

A.Server服务端开发：

①申请并记录API。

②在根目录下，新建Module，在新窗口打开Module，在pom.xml引入依赖。

③编辑application.yml配置文件，创建application-stdio.yml，创建application-sse.yml。

④创建tool目录，创建ImageSerachTool.java。

⑤创建ImageSearchToolTest.java。

⑥编辑application.java主类，使用@Bean注册工具。

⑦Maven Package打包好项目jar，记录好.jar目录，准备导入到客户端项目中。AIagent\jc-image-search-mcp\target\jc-image-search-mcp-0.0.1-SNAPSHOT.jar

B.Client客户端开发：

①在pom.xml引入依赖。

②创建mcp-servers.json。

③在LoveApp.java，创建doChatWithMcp()。

④创建doChatWithMcpTest（）。

⑤在pom.xml配置好MCP信息，在doChatWithMcp()断点debug。

C.从stdio切换到sse：

①在image-search-server设置active:sse。

②以Debug模式启动JcImageSearchMcpApplication。

③在AIagent项目中，配置application.yml，设置好url。

④run运行doChatWithMcpTest（），在ImageSearchTool.java断点测试。会显示query关键词。

29.本地实现Manus智能体。

①创建目录，agent->model。

②创建AgentState.java。

③创建BaseAgent.java。

④创建终止任务的工具类TerminateTool.java。

⑤创建ReActAgent.java。

⑥创建ToolCallAgent.java。

⑦创建恋爱大师智能体类LoveManus.java。

⑧单元测试，创建LoveManusTest.java。

30.AI应用接口开发。

①修改抽象基础代理类BaseAgent.java，新增方法runStream()。

②新建AI访问控制器，AiController.java，并测试接口。

③新建跨域配置类CorsConfig.java。

31.实现AI会话SSE流式传输与接口。

①在loveApp.java创建方法doChatByStream()。

②在AiController.java创建doChatWithLoveAppSSE()，doChatWithLoveAppServerSseEmitter(),doChatWithLoveAppServerSentEvent()。

③对SSE接口进行测试。

32.使用VibeCoding实现前端开发。①将以下提示词提交给Trae。②启动后端服务器application。③打开终端切换到前端目录，输入npm run dev，访问localhost:5173进行测试。

提示词如下：

```
你是一位专业的前端开发，请帮我根据下列信息来生成对应的前端项目代码。 
 
需求:
 
 1）主页：用于切换不同的应用 
 
 2）页面 1：AI 恋爱大师应用。页面风格为聊天室，上方是聊天记录（用户信息在右边，AI 信息在左边），下方是输入框，进入页面后自动生成一个聊天室 id，用于区分不同的会话。通过 SSE 的方式调用 doChatWithLoveAppSse 接口，实时显示对话内容。 
 
 3）页面 2：AI 超级智能体应用。页面风格同页面 1，但是调用 doChatWithManus 接口，也是实时显示对话内容。 
 
 技术选型:
 
 1. Vue3 项目 
 2. Axios 请求库 
 
 后端接口信息:
 
 接口地址前缀：http://localhost:8123/api 
 
 SpringBoot 后端接口代码:
 
 package com.jc.aiagent.controller; 
 
 import com.jc.aiagent.agent.LoveManus; 
 import jakarta.annotation.Resource; 
 import org.springframework.ai.chat.model.ChatModel; 
 import org.springframework.ai.tool.ToolCallback; 
 import org.springframework.web.bind.annotation.GetMapping; 
 import org.springframework.web.bind.annotation.RequestMapping; 
 import org.springframework.web.bind.annotation.RestController; 
 import org.springframework.web.servlet.mvc.method.annotation.SseEmitter; 
 
 /** 
  * AI 访问控制器 
  */ 
 @RestController 
 @RequestMapping("/ai") 
 public class AiController { 
 
     @Resource 
     private ToolCallback[] allTools; 
 
     @Resource 
     private ChatModel dashscopeChatModel; 
 
     /** 
      * 流式调用 Manus 超级智能体 
      * 
      * @param message 
      * @return 
      */ 
     @GetMapping("/manus/chat") 
     public SseEmitter doChatWithManus(String message) { 
         LoveManus loveManus = new LoveManus(allTools, dashscopeChatModel); 
         return loveManus.runStream(message); 
     } 
 
 } 
 
 必须遵守的开发规则： 
 -用中文回复 
 -改完代码后显示修改内容的文件名和行号 
 -禁止读取以.log结尾的文件 
 -代码开发遵循开闭原则
```

关键开发步骤：

①创建ai-agent-web。创建src/api/chat.ts。封装了 doChatWithLoveAppSse 和 doChatWithManus 两个 SSE 接口。

②创建src/components/ChatRoom.vue。实现聊天室主界面。

③创建src/router/index.ts。配置三个路由：/、/love-app、/manus。

④创建src/views/LoveAppView.vue。进入页面时自动生成 chatId。调用 doChatWithLoveAppSse 接口。

⑤创建src/views/ManusAppView.vue。调用 doChatWithManus 接口。

33.使用VibeCoding实现邮箱注册登录功能。提示词如下：

```
你是一位专业的全栈开发工程师，请帮我根据下列信息来生成对应的项目代码。 

需求:实现web系统邮箱注册登录功能。

前端开发：
1)在主页的基础上，新增注册登录界面。必须先登录，然后才能进入主页。如果未登录，那么不能进入主页。
后端开发：
2）使用邮箱注册登录服务。注册包含：输入邮箱地址，输入密码，重输入密码，输入验证码。①前端实现对输入的邮箱地址合法性校验，判断邮箱是否已被注册。②注册密码长度需要大于8位且小于20位，必须包含数字、小写字母、大写字母，否则注册失败。③重输入密码需要与第一次的输入密码相同，否则注册失败。④验证码需要验证功能，验证成功才能成功注册。相关服务需要使用邮箱授权码或者API的话，如果你需要配置文件的详细信息，可以引导我并向我提问。
3）存储用户信息使用Mysql+Mybatis写入到数据库。在Mysql新建user用户表。Mysql数据库地址、用户名和密码信息在application.yml文件中。
4）请你根据功能需要优化现有的数据库。现有数据库信息：
mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| jc_ai_agent        |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.168 sec)

mysql> use jc_ai_agent;
Database changed
mysql> show tables;
+-----------------------+
| Tables_in_jc_ai_agent |
+-----------------------+
| chat_memory           |
+-----------------------+
1 row in set (0.025 sec)

mysql> describe chat_memory;
+-----------+-------------+------+-----+-------------------+-------------------+
| Field     | Type        | Null | Key | Default           | Extra             |
+-----------+-------------+------+-----+-------------------+-------------------+
| id        | bigint      | NO   | PRI | NULL              | auto_increment    |
| content   | text        | NO   |     | NULL              |                   |
| type      | varchar(10) | NO   |     | NULL              |                   |
| timestamp | timestamp   | NO   | MUL | CURRENT_TIMESTAMP | DEFAULT_GENERATED |
| chat_id   | varchar(36) | NO   |     | NULL              |                   |
+-----------+-------------+------+-----+-------------------+-------------------+
5 rows in set (0.023 sec)

5）如果存在其他必定需要的开发功能模块，请你补充。

技术选型:
1.Vue3 项目 
2.Axios 请求库 
3.Mysql
4.Mybatis

 
 必须遵守的开发规则： 
 -用中文回复。
 -改完代码后显示修改内容的文件名和行号。 
 -禁止读取以.log结尾的文件。 
 -代码开发遵循开闭原则，尽量少改动原有的代码。

```

已创建/修改的文件汇总：

```
类型	文件	说明
后端实体	entity/User.java	用户实体
后端DTO	dto/UserRegisterDTO.java	注册请求DTO（含校验注解）
后端DTO	dto/UserLoginDTO.java	登录请求DTO
后端DTO	dto/VerifyCodeDTO.java	验证码请求DTO
后端Mapper	mapper/UserMapper.java	用户数据访问
后端XML	mapper/UserMapper.xml	MyBatis映射
后端Service	service/UserService.java	用户服务接口
后端Service	service/impl/UserServiceImpl.java	注册/登录/验证码业务
后端Controller	controller/UserController.java	用户接口
后端工具	utils/JwtUtil.java	JWT Token生成与验证
后端工具	utils/VerifyCodeCache.java	验证码内存缓存
后端拦截器	interceptor/AuthInterceptor.java	登录认证拦截
后端配置	config/WebMvcConfig.java	拦截器注册
后端通用	common/Result.java	统一响应结果
后端POM	pom.xml	添加邮件依赖
后端YML  application-local.yml 添加邮箱配置
前端页面	views/LoginView.vue	登录页
前端页面	views/RegisterView.vue	注册页
前端页面	views/HomeView.vue	主页（增加用户信息和退出）
前端API	api/auth.ts	认证API + Axios拦截器
前端路由	router/index.ts	路由守卫
```

##### 数据库关联：

chat_memory 已添加 user_id 实现用户关联。采用 ThreadLocal 方案，改动最小：

```
文件	修改内容
context/UserContext.java (新增)	ThreadLocal 存储当前用户ID
interceptor/AuthInterceptor.java	请求开始时 setUserId，结束后 clear
entity/ChatMemoryEntity.java	新增 userId 字段
mapper/ChatMemoryMapper.java	查询/删除增加 userId 参数
mapper/ChatMemoryMapper.xml	SQL 增加 user_id 条件
repository/MybatisChatMemoryRepository.java	自动从 ThreadLocal 获取 userId
```

##### 拦截器优化：

浏览器 EventSource API 不支持自定义请求头，无法发送 Authorization: Bearer xxx，导致 SSE 请求被拦截器返回 401，前端无任何提示。

修改的 3 个文件：

```
文件	行号	修改内容
interceptor/AuthInterceptor.java	25-27	Token 获取增加 request.getParameter("token") 兜底逻辑
api/chat.ts	1, 27-28	从 import api 改为 import { getToken }，URL 增加 &token= 参数
api/chat.ts	73-74	Manus 接口同样增加 &token= 参数
```

解决 **ThreadLocal userId 在异步线程中丢失**的问题。

数据流：

```
请求线程:  AuthInterceptor设置userId → Controller捕获userId → ChatUserContextHolder.bind(chatId, userId)
↓
reactor线程: Flux处理 → MemoryRepository.saveAll() → UserContext.getUserId()=null 
→ 兜底: ChatUserContextHolder.getUserId(chatId) → ✓ 正确的userId
↓
完成/异常:  ChatUserContextHolder.unbind(chatId) → 清理映射
```

修改内容 修改了 4 个文件（新增 1 个）

```
通过 chatId → userId 映射 + 异步线程 ThreadLocal 恢复，解决跨线程 userId 丢失问题：

1.新增 ChatUserContextHolder.java 路径：src/main/java/com/jc/aiagent/context/ChatUserContextHolder.java 使用 ConcurrentHashMap<String, Long> 维护 chatId → userId 映射，线程安全 

2.MybatisChatMemoryRepository.java（第 3, 31-34, 51-54, 86-89 行） findByConversationId()、saveAll()、deleteByConversationId() 三个方法均增加兜底逻辑：当 UserContext.getUserId() 为 null 时，从 ChatUserContextHolder.getUserId(chatId) 获取 

3.AiController.java（第 5-6, 87-90, 100-106 行） doChatWithLoveAppServerSseEmitter() 在请求线程上捕获 userId 并绑定到 chatId Flux 的 onError 和 onComplete 回调中解绑映射，防止内存泄漏 

4.BaseAgent.java（第 4, 163-164, 168-171, 223-226 行） runStream() 在请求线程上捕获 userId，在 CompletableFuture.runAsync() 的异步线程开头恢复 UserContext 外层 finally 块中清除 UserContext，防止内存泄漏
```



##### 优化AI超级智能体的回答内容和思考长度：

问题1：用户问“深圳今日天气如何？” Agent直接开始思考...回答出无效内容。

解决方法：

```
将WeatherTool 注册到 Agent 的工具列表中。
修改 1：ToolRegistration.java（第 5 行新增 import，第 21-22 行注入，第 34 行注册）
注入 WeatherTool Spring Bean 将其添加到 ToolCallbacks.from() 

修改 2：LoveManus.java（第 20-21 行新增系统提示）
在 system prompt 中增加了明确的指令：天气相关查询 必须直接使用 getWeather 或 getLocalWeather，禁止使用 web search/scraping
```
问题2：Agent的显示出思考过程不够精简，用户阅读体验差。

解决方法--精简显示Agent的思考过程：

```
改动概览
后端（3 个文件）

1. ToolCallAgent.java

新增 finalAnswer 字段保存最终回答文本
新增 currentThinkingText 字段保存精简的思考状态描述
think() 方法：无工具调用时保存最终回答，有工具调用时生成精简描述（如"正在调用 天气查询"）
act() 方法：返回精简的执行结果（不再返回原始工具数据）
新增 beautifyToolName() 将工具方法名转为友好中文名

2. ReActAgent.java

重写 runStream() 方法，使用 SSE 命名事件区分消息类型：
thinking 事件：精简的思考状态（如"正在调用 天气查询"）
answer 事件：最终回答（逐字打字机效果流式输出）
complete 事件：完成通知
error 事件：错误信息
新增抽象方法 getCurrentThinkingText() 和 getFinalAnswer()

前端（1 个文件）
3. ChatRoom.vue

监听 thinking / answer / complete / error 四种命名事件
思考状态以轻量标签形式展示（⚡ + 文字，带脉冲动画）
最终回答正常气泡展示，保留打字机效果
兼容旧版 message 默认事件（恋爱大师仍走此通道）

效果对比
之前	现在
显示完整的 "Step 1: 工具 searchWeb 完成了它的任务！结果: {5条搜索结果JSON}	⚡ 正在调用 网页搜索
显示 "Step 2: 工具 scrapeWebPage 完成了它的任务！结果: ...（整页HTML）"	⚡ 正在调用 网页抓取
没有清晰的最终回答	最终回答以气泡形式逐字输出
```

执行流程：

```
Step 1: think() → LLM 生成 "深圳当前天气：晴，气温32°C..." + 调用 doTerminate
                 → finalAnswer = "深圳当前天气：晴，气温32°C..."  ✓
Step 2: act()  → 检测到 doTerminate → state=FINISHED → 循环退出
         ↓ 进入 !answerSent 分支
finalAnswer = "深圳当前天气：晴，气温32°C..." → 前端收到实际内容  ✓
```

对话测试图片：

A.恋爱大师

![1784893051021](C:\Users\jade\AppData\Roaming\Typora\typora-user-images\1784893051021.png)

B.超级智能体

![1784893235339](C:\Users\jade\AppData\Roaming\Typora\typora-user-images\1784893235339.png)

34.使用阿里云Serverless快速部署AI服务。

①创建application-prod.yml，在原yml基础上注释掉了mcp配置。

②在根目录下创建Dockerfile。

③在阿里云serverless托管平台创建环境、新建服务、上传代码、发布部署。

④创建nginx.conf。实现静态资源访问和反向配置。

⑤在前端目录下创建Dockerfile和.dockerignore。

⑥将前端代码推送到阿里云CodeUp。

⑦创建阿里云云效 DevOps 控制台，创建流水线实现自动构建镜像 + 推送到 ACR + 部署到 FC。

⑧Docker HUB访问超时，使用阿里云镜像加速器。

⑨在阿里云函数计算控制台，进入后端服务并创建前端函数。创建 Custom Container 函数。配置 HTTP 触发器。等待创建完成并获取访问地址。验证部署。

⑩测试函数执行成功。访问公网地址自动下载，无法渲染，还需要域名配置。转投腾讯云serverless配置，不需要域名。 

```
用户浏览器
    ↓
阿里云 FC（前端 nginx 容器，端口 9000）
    ├── 静态文件 → Vue3 前端页面
    └── /api/ 反向代理 → 后端 Serverless 地址
  
浏览器 → 前端Serverless(nginx:80)
  /api/user/login
    → proxy_pass https://lovemaster-web-...fcapp.run/api/user/login
    → 后端 Spring Boot (context-path: /api) → /api/user/login
```

35.使用腾讯云Serverless快速部署AI服务。

