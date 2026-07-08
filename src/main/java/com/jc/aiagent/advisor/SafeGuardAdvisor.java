package com.jc.aiagent.advisor;



import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SafeGuardAdvisor implements CallAdvisor, StreamAdvisor {
        public static final String DEFAULT_FAILURE_RESPONSE = "用户输入涉及敏感内容，我无法回答，聊点其他的试试？";
        // order值越小优先级越高，设置为2保证在LoggerAdvisor(0)之后执行
        private int order = 2;

        public SafeGuardAdvisor() {}
        @Override
        public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain){
                String userMessage = chatClientRequest.prompt().getUserMessage().getText();
                // 1. 获取所有敏感词并进行去重处理
                List<String> sensitiveWords = SensitiveWordHelper.findAll(userMessage)
                        .stream()
                        .distinct()
                        .collect(Collectors.toList());
                // 2. 如果包含敏感词，构建引导提示并拦截
                if (!sensitiveWords.isEmpty()) {
                        String failureMessage = buildFailureMessage(sensitiveWords);
                        return createFailureResponse(chatClientRequest, failureMessage);
                }
                return callAdvisorChain.nextCall(chatClientRequest);
        }

        /**
         * 构建包含具体敏感词的引导提示，实现从“一刀切”到“引导提问”
         * @param sensitiveWords
         * @return
         */
        private String buildFailureMessage(List<String> sensitiveWords) {
                String wordsStr = String.join("、", sensitiveWords);
                return String.format("抱歉，您的输入中包含了系统限制的敏感词汇：【%s】。为了能够正常为您解答，请您修改或替换这些词汇后重新提问。", wordsStr);
        }

        private ChatClientResponse createFailureResponse(ChatClientRequest chatClientRequest, String failureMessage) {
                return ChatClientResponse.builder()
                        .chatResponse(ChatResponse.builder()
                                .generations(List.of(new Generation(new AssistantMessage(failureMessage))))
                                .build())
                        .context(Map.copyOf(chatClientRequest.context()))
                        .build();
        }
        @Override
        public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain  streamAdvisorChain){
                String userMessage = chatClientRequest.prompt().getUserMessage().getText();
                List<String> sensitiveWords = SensitiveWordHelper.findAll(userMessage)
                        .stream()
                        .distinct()
                        .collect(Collectors.toList());

                if (!sensitiveWords.isEmpty()) {
                        String failureMessage = buildFailureMessage(sensitiveWords);
                        return createFailureResponseFlux(chatClientRequest, failureMessage);
                }
                return streamAdvisorChain.nextStream(chatClientRequest);
        }

        private Flux<ChatClientResponse> createFailureResponseFlux(ChatClientRequest chatClientRequest, String failureMessage) {
                ChatClientResponse response = ChatClientResponse.builder()
                        .chatResponse(ChatResponse.builder()
                                .generations(List.of(new Generation(new AssistantMessage(failureMessage))))
                                .build())
                        .context(Map.copyOf(chatClientRequest.context()))
                        .build();
                return Flux.just(response);
        }

        @Override
        public int getOrder() {
                return this.order;
        }
        @Override
        public String getName(){
                return this.getClass().getSimpleName();
        }

}
