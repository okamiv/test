package com.okami.test.bus;

        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.cloud.bus.ServiceMatcher;
        import org.springframework.context.ApplicationEventPublisher;
        import org.springframework.stereotype.Service;

@Service
public class EventPublishService {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ServiceMatcher busServiceMatcher;


    public void publish() {
        System.out.println("发布事件。。。");
        BusiApplicationEvent busiApplicationEvent = new BusiApplicationEvent(this, busServiceMatcher.getServiceId(), "com.okami.test.bus.EventListerner", "test info");
        applicationEventPublisher.publishEvent(busiApplicationEvent);
    }

}
