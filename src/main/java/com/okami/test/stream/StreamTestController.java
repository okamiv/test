package com.okami.test.stream;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StreamTestController {

    @Autowired
    private SourceSender.StreamOutput streamOutput;

    @GetMapping("/stream")
    public String test(@RequestParam("info") String info) {
        String message = Strings.isNullOrEmpty(info) ? "测试消息" : info;
        MessageBuilder<byte[]> messageBuilder = MessageBuilder.withPayload(message.getBytes());
        // 发送消息到通道
        streamOutput.output().send(messageBuilder.build());
        return message;
//        streamInput.input()
    }
}
