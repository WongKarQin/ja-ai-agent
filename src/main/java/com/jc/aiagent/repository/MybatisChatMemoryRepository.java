package com.jc.aiagent.repository;

import com.jc.aiagent.entity.ChatMemoryEntity;
import com.jc.aiagent.mapper.ChatMemoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于Mybatis + MySQL的ChatMemoryRepository实现
 */
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class MybatisChatMemoryRepository implements ChatMemoryRepository {
    private final ChatMemoryMapper chatMemoryMapper;
    /**
     * 根据会话ID查询历史消息
     */
    @Override
    public List<Message> findByConversationId(String chatId){
        List<ChatMemoryEntity> entities = chatMemoryMapper.selectByChatId(chatId);
        List<Message> messages = new ArrayList<>();
        for(ChatMemoryEntity entity : entities){
            Message message = convetToMessage(entity);
            if(message!=null){
                messages.add(message);
            }
        }
        return messages;
    }
    /**
     * 保存消息到数据库
     * Spring AI 的 MessageWindowChatMemory 会在每次对话后调用此方法
     * 传入的是当前窗口内的所有消息（已截断后的）
     */
    @Override
    public void saveAll(String chatId, List<Message> messages){
        //先删除旧数据
        chatMemoryMapper.deleteByChatId(chatId);
        if(messages==null||messages.isEmpty()){
            return;
        }
        //转化为实体并批量插入
        List<ChatMemoryEntity> entities = new ArrayList<>();
        for(Message message : messages){
            ChatMemoryEntity entity = ChatMemoryEntity.builder()
                    .chatId(chatId)
                    .content(message.getText())
                    .type(message.getMessageType().name())
                    .timeStamp(LocalDateTime.now())
                    .build();
            entities.add(entity);
        }
        chatMemoryMapper.batchInsert(entities);
    }
    /**
     * 获取所有会话ID
     */
    @Override
    public List<String> findConversationIds(){
        // 这里简单返回空列表，如果需要可以自行在 Mapper 中实现 selectAllChatIds()
        return Collections.emptyList();
    }
    @Override
    public void deleteByConversationId(String chatId){

    }
    /**
     * 将数据库中实体转换为Spring AI的Message对象
     * @param entity
     * @return
     */
    private Message convetToMessage(ChatMemoryEntity entity) {
        String content = entity.getContent();
        MessageType type;
        try {
            type = MessageType.valueOf(entity.getType());
        }catch (IllegalArgumentException e){
            return null;
        }
        return switch(type){
            case USER->new UserMessage(content);
            case ASSISTANT->new AssistantMessage(content);
            case SYSTEM->new SystemMessage(content);
            case TOOL->{
                ToolResponseMessage.ToolResponse response = new ToolResponseMessage.ToolResponse("id", "name", content);
                // 使用 Builder 模式创建 ToolResponseMessage
                yield ToolResponseMessage.builder().responses(List.of(response)).build();
            }
        };
    }
}
