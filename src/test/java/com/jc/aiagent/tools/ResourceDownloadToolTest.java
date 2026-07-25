package com.jc.aiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourceDownloadToolTest {

    @Test
    public void downloadResource() {
        String result = new ResourceDownloadTool().downloadResource(
                "https://nmp.about.nike.com/about/prod/cf68f541-fc92-4373-91cb-086ae0fe2f88/002-nike-logos-swoosh-white.jpg?m=eyJlZGl0cyI6eyJqcGVnIjp7InF1YWxpdHkiOjEwMH0sIndlYnAiOnsicXVhbGl0eSI6MTAwfSwiZXh0cmFjdCI6eyJsZWZ0IjowLCJ0b3AiOjAsIndpZHRoIjo1MDAwLCJoZWlnaHQiOjI4MTN9LCJyZXNpemUiOnsid2lkdGgiOjY0MH19fQ%3D%3D&s=6b5ddbd663407475bf3c4dff6ec41e5205fed9df0dca6490e7318620b4e4f57e", "nikelogo.png"
        );
        System.out.println("result = " + result);
        Assertions.assertNotNull(result);
    }

}
