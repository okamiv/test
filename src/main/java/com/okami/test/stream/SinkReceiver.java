package com.okami.test.stream;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.SubscribableChannel;

/**
 * @EnableBinding 该注解用来指定一个或多个定义了@Input或@Output注解的接口，以此实现对消息通道（Channel）的绑定
 */
//@EnableBinding(Sink.class)
@EnableBinding(SinkReceiver.StreamInput.class)
public class SinkReceiver {

    @StreamListener(StreamInput.input)
    public void receive(Object payload) {
        System.out.println("receive: " + payload);
    }


    public interface StreamInput {

        String input = "input";

        @Input(StreamInput.input)
        SubscribableChannel input();
    }
}
