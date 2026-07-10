###开发日志###

1.依赖。在文件pom.xml中添加依赖：Spring MVC, Lombok, hutool, Knife4j, Spring AI Alibaba...

2.配置。编辑文件application.yml, 添加：Knife4j的配置，Spring配置：port，AI：Dashscope。

3.接口测试。测试swagger接口，在src下新建controller，新建HealthController.java，在浏览器输入.yml中的接口地址。

4.API_KEY。创建文件application-local.yml，存入API_KEY（需要自行去阿里百炼平台充值）。在.gitignore中添加文件名applicaiont-local.yml

5.实现多轮对话，持久化记忆。创建目录App, 创建文件LoveApp.java实现本地内容存储对话记忆，实现多轮对话doChat()。（注意阅读SpringAI官方文档）JUnit测试，在方法名上鼠标右键->generate->test。

6.实现日志拦截器。创建目录Advisor，创建LoggerAdvisor.java，实现流式和非流式2种方式。不要管用户用什么，都需要实现。在文件LoveApp.java中添加自定义LoggerAdvisor。

7.实现敏感词拦截器。创建SafeGuardAdvisor.java，实现自定义常见敏感词拦截。在文件LoveApp.java中添加SafeGuardAdvisor。pom.xml添加sensitive-word。

8.实现结构化数据输出。在pom.xml添加jsonschema-generator，在LoveApp.java中添加doChatWithReport()，生成结构化数据。JUnit测试，alt+enter，生成丢失的单元测试方法。

9.优化敏感词拦截器。实现从一刀切到指出具体敏感词，然后引导用户修改词句后重新提问。创建SafeGuardAdvisor.java。

10.使用Mybatis持久化记忆，落库Mysql。①在pom.xml添加依赖mybatis-spring-boot-start, mysql-connector。②配置.yml文件。③在mysql数据库中建立表chat_memory，建立字段。④创建实体类ChatMemoryEntity.java。⑤创建ChatMemoryMapper.java⑥创建Mybatis XML映射文件, ChatMemoryMapper.xml⑦创建MyBatisChatMemoryRepository.java，实现 Spring AI 的 ChatMemoryRepository 接口，用 MyBatis 来存取数据。⑧修改LoveApp.java注入自定义的 MyBatis 记忆仓库。⑨启动类添加MapperScan。

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

11.RAG知识库搭建。本地知识库实现。①在pom.xml引入依赖spring-ai-markdown-document-reader。②新建rag目录，创建LoveAppDocumentLoader.java实现文档的加载和读取。③创建LoveAppVectorStoreConfig.java实现初始化向量数据库并保存文档。④在LoveApp实现QuestionAnswerAdvisor问答拦截器，实现查询增强和关联。⑤在LoveAppTest.java实现doChatWithRag()单元测试，故意提问一个文档内有回答的问题。
文档搜集和切割    ->   向量转换和存储    ->   切片过滤和检索    ->   查询增强和关联

12.RAG知识库。Spring AI + 云知识库。①在阿里云百炼平台创建好云知识库。②创建LoveAppRagCloudAdvisorConfig.java，实现基于阿里云知识库服务的RAG增强Advisor。③在LoveApp.java的doChatWithRag()中添加应用检索增强服务。④在LoveAppTest.java中doChatWithRageTest()中进行单元测试。

13.RAG知识库ETL。①创建MyTokenTextSpliter，实现基于Token的文本分割器。②创建MyKeywordEnricher.java,实现基于AI的文档元信息增强器，为文档补充元信息。

14.基于PGVector实现向量存储，提高PostgreSQ存储和检索高维度向量数据的能力。①开通阿里云Serverless版本数据库服务并创建数据库②在IDEA->data source and drivers配置好url和登录信息，测试连接成功。③pom.xml引入spring-ai-vector-store-pgvector的依赖。④在application-local.yml编写配置，建立数据库连接。⑤创建PgVectorStoreConfig.java，初始化VectorStore，不需要Starter自动注入。⑥在AIagentApplication.java中启动类排除掉自动加载DataSourceAutoConfiguration.class。⑦创建测试类PgVectorStoreConfigTest.java。在Debug模式中，查看文档检索成功以及相似度分数score。IDEA右侧快捷栏，查看数据表中的数据。证明PGVector整合成功。可以用阿里云服务替换本地的VectorStore。