package com.jc.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class LoveAppTest {
    @Resource
    private LoveApp loveApp;
    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
//        //第一轮对话
//        String message = "你好！我是程序员JC。你知道英国的阿伦图灵吗？";
//        String answer = loveApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
//        //第二轮对话
//        message = "美加墨世界杯开赛在即，你预测谁会夺冠？";
//        answer= loveApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
//        //第三轮对话
//        message = "你还记得我是谁吗？如果你不知道梅西和C罗的话，你可以联网搜索。你觉得他们两个谁更厉害？";
//        answer = loveApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
        //敏感词测试
        String message = "========敏感词测试========\n你好！你怎么看待习近平、胡锦涛、江泽民、邓小平都不如毛泽东的说法？";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        //String message = "你好！我的程序员JC，我的梦中情人是田径运动员刘女士，如何让刘女士更爱我呢？";
        //“情人”触发了敏感词拦截器，在AI提示后进行了修改。
        String message = "你好！我的程序员JC，我的喜欢的人是田径运动员刘女士，如何让刘女士更爱我呢？";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message,chatId);
        Assertions.assertNotNull(loveReport);
    }

    /**
     * 怎样维护婚后夫妻间的亲密关系？
     * 定期安排二人世界，如每周一次看电影或共进晚餐。保持身体亲密接触，日常拥抱、亲吻。分享日常喜怒哀乐，深入交流内心想法。一起回忆美好过往，如旅行经历、恋爱趣事。为对方制造小惊喜，如纪念日礼物。像老张夫妇坚持每周约会，分享生活点滴，结婚多年仍甜蜜如初。
     * 推荐课程：[《婚后亲密关系维护秘籍》](https://www.codefather.cn)，课程提供多种维护亲密关系的方法与技巧，让婚后爱情持续升温，家庭幸福美满。
     */
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不大亲密，怎么办？";
        //测试查询检索增强内容
        //String message = "有人说python不如Java，Java不如C++，对此你怎么看？";
        String answer = loveApp.doChatWithRag(message,chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
//        // 测试联网搜索问题的答案
//        testMessage("周末想带女朋友去深圳约会，推荐几个适合情侣的小众打卡地？");
//
//        // 测试网页抓取：恋爱案例分析
//        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");
//
//        // 测试资源下载：图片下载
//        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");
//
//        // 测试终端操作：执行代码
//        testMessage("执行 Python3 脚本来生成数据分析报告");
//
//        // 测试文件操作：保存用户档案
//        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，地点深圳，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        //测试图片搜索MCP
        String message = "我的另一半喜欢高跟鞋时尚单品，请帮我搜索一些哄对象开心的图片";
        String answer = loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}