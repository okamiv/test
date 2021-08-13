package com.okami.test.bus;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class EventListerner implements ApplicationListener<BusiApplicationEvent> {
//
//    @Autowired
//    private ServiceMatcher serviceMatcher;
//
//    @EventListener(classes = BusiApplicationEvent.class)
//    public void listern(BusiApplicationEvent event) {
//        System.out.println("event: " + event);
//        if (serviceMatcher.isForSelf(event)) {
//
//        }
//    }


    public void onApplicationEvent(BusiApplicationEvent event) {
        System.out.println("消费事件： " + event);
    }
}
