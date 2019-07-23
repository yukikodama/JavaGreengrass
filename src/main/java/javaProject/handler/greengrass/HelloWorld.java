package javaProject.handler.greengrass;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.*;


public class HelloWorld {
    static {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new PublishHelloWorld(), 0, 10000);
    }

    public String handleRequest(Object input, Context context) {
        return "Hello from java";
    }
}

class PublishHelloWorld extends TimerTask {
    private IotDataClient iotDataClient = new IotDataClient();
    private String publishMessage = String.format("Hello world! Sent from Greengrass Core running on platform: %s-%s using Java", System.getProperty("os.name"), System.getProperty("os.version"));
    private PublishRequest publishRequest = new PublishRequest().withTopic("hello/world").withPayload(ByteBuffer.wrap(String.format("{\"message\":\"%s\"}", publishMessage).getBytes()));

    public void run() {
        try {
            iotDataClient.publish(publishRequest);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
