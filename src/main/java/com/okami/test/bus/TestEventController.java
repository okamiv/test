package com.okami.test.bus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestEventController {

    @Autowired
    private EventPublishService eventPublishService;

    @RequestMapping("/hello")
    public String hello() {
        eventPublishService.publish();
        return "hello";
    }


}