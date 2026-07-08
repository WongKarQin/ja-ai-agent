package com.jc.aiagent.mapper;

import com.jc.aiagent.entity.ChatMemoryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMemoryMapper {
    /**
     * 查询指定会话消息，按时间顺序
     * @return
     */
    List<ChatMemoryEntity> selectByChatId(@Param("chatId") String chatId);

    /**
     * 批量插入消息
     */
    void batchInsert(@Param("list") List<ChatMemoryEntity> list);
    /**
     * 删除指定会话的所有消息
     */
    void deleteByChatId(@Param("chatId") String chatId);
}
