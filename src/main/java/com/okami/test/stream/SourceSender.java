package com.okami.test.stream;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;

/**
 * @EnableBinding 该注解用来指定一个或多个定义了@Input或@Output注解的接口，以此实现对消息通道（Channel）的绑定
 */
//@EnableBinding(Sink.class)
@EnableBinding(SourceSender.StreamOutput.class)
public class SourceSender {

    public interface StreamOutput {
        String output = "output";

        @Output(SourceSender.StreamOutput.output)
        MessageChannel output();
    }

//    @StreamListener(StreamOutput.output)
//    public void send(Object payload) {
//        System.out.println("send: " + payload);
//    }
}
