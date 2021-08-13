package com.okami.test.bus;

import org.springframework.cloud.bus.event.RemoteApplicationEvent;

/**
 * 消息事件
 */

public class BusiApplicationEvent extends RemoteApplicationEvent {

    /**
     * 用户名
     */
    private String eventInfo;

    public BusiApplicationEvent() { // 序列化
    }

    public BusiApplicationEvent(Object source, String originService, String destinationService, String eventInfo) {
        super(source, originService);
        this.eventInfo = eventInfo;
    }

    public String getEventInfo() {
        return eventInfo;
    }
}
