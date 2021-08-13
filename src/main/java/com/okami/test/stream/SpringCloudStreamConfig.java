package com.okami.test.stream;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;

@EnableBinding({SinkReceiver.StreamInput.class, SourceSender.StreamOutput.class})
//@EnableBinding({StreamInput.class})
//@EnableBinding({StreamOutput.class})
@Configuration
public class SpringCloudStreamConfig {
}
