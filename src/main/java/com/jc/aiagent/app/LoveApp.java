package com.jc.aiagent.app;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.jc.aiagent.advisor.LoggerAdvisor;
import com.jc.aiagent.advisor.SafeGuardAdvisor;
import com.jc.aiagent.rag.LoveAppRagCustomAdvisorFactory;
import com.jc.aiagent.rag.QueryRewriter;
import com.jc.aiagent.repository.MybatisChatMemoryRepository;
import com.jc.aiagent.tools.PDFGenerationTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
/***
 * 恋爱大师App
 */
public class LoveApp {
    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。"+
            "与情感婚恋家庭心理等无关的话题和问题不要回答。";
    @Autowired
    private QueryRewriter queryRewriter;

    /**
     * 初始化客户端
     * @param dashscopeChatModel
     */
    public LoveApp(ChatModel dashscopeChatModel, MybatisChatMemoryRepository mybatisChatMemoryRepository) {
        // 启动时打印模型信息
        if (dashscopeChatModel.getDefaultOptions() instanceof DashScopeChatOptions options) {
            log.info("【模型配置】当前加载的大模型是: {}", options.getModel());
        }
//        //初始化基于内存的对话记忆
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(10)//对话最多10轮
//                .build();
        // 使用 MyBatis 持久化记忆仓库
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mybatisChatMemoryRepository)
                .maxMessages(10)
                .build();
       chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志拦截器 (Order=0)
                        new LoggerAdvisor()
                        // 自定义敏感词拦截器 (Order=2)
//                        new SafeGuardAdvisor()
                ).build();
    }

    /**
     * AI基础对话，已支持多轮对话
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId){
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = Optional.ofNullable(chatResponse)
                .map(ChatResponse::getResult)
                .map(r->r.getOutput())
                .map(r->r.getText())
                .orElse("==============AI返回为空！================\n");
        log.info("\n========== AI 对话记录 ==========\n" +
                "    会话ID: {}\n" +
                "    用户输入: {}\n" +
                "    AI回复: {}\n" +
                "    ================================\n",chatId,message,content);
        return content;
    }

    record LoveReport(String title, List<String> suggestions){

    }

    /**
     * AI 恋爱报告功能（结构化数据输出）
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message, String chatId){
        try {
            LoveReport loveReport = chatClient
                    .prompt()
                    .system(SYSTEM_PROMPT+"每次对话完成后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                    .user(message)
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                    .call()
                    .entity(LoveReport.class);
            log.info("loveReport = {}",loveReport);
            return loveReport;
        } catch (Exception e) {
            // 容错处理：当触发敏感词拦截（返回纯文本）或 AI 返回非标准 JSON 时，捕获异常防止程序崩溃
            log.warn("生成恋爱报告失败，可能是由于敏感词拦截或AI返回格式异常: {}", e.getMessage());
            return new LoveReport(
                    "报告生成失败",
                    List.of("您的输入可能涉及敏感内容被拦截，或AI未能返回正确格式，请修改提问内容后重试。")
            );
        }
    }

    /**
     * 对话接入RAG知识库
     */
    @Resource
    private VectorStore loveAppVectorStore;
    @Resource
    private Advisor loveAppRagCloudAdvisor;
    @Resource
    private QueryRewriter loveAppQueryRewriter;
    public String doChatWithRag(String message, String chatId){
        //应用查询重写
        message = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 应用 RAG 知识库问答
                .advisors(QuestionAnswerAdvisor.builder(loveAppVectorStore).build())
                //应用RAG检索增强服务（阿里云知识库）
//                .advisors(loveAppRagCloudAdvisor)
                //应用CustomAdvisorFactory
                .advisors(LoveAppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(loveAppVectorStore, "已婚"))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("==========AI回答内容==========\n{}", content);
        return content;
    }

    /***
     * AI恋爱报告支持工具调用
     */
    @Resource
    private ToolCallback[] allTools;
    public String doChatWithTools(String message, String chatId){
        ChatResponse response = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("==========AI恋爱报告支持工具调用==========\n{}", content);
        return content;
    }

    /**
     * 使用MCP服务完成对话
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("使用MCP对话的content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }
}
