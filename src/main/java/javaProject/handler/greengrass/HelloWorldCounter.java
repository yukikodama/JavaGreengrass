package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class HelloWorldCounter extends BaseSensor {

    int counter = 0;

    public HelloWorldCounter() throws Exception {
    }

    @Override
    public String handleRequest(Object event, Context context) {
        try {
            System.out.println("event: " + event);
            System.out.println("context: " + context);
            long updateAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
            String publishMessage = new JSONObject()
                    .put("SensorId", serial)
                    .put("CreateAt", createAt)
                    .put("UpdateAt", updateAt)
                    .put("Counter", counter++)
                    .put("TTL", (updateAt / 1000) + 900).toString();
            iotDataClient.publish(new PublishRequest().withTopic("hello/world/counter").withPayload(ByteBuffer.wrap(publishMessage.getBytes())));
            Thread.sleep(5000);
        } catch (Exception ex) {
            System.err.println(ex);
        }
        return "ok";
    }

    @Override
    public void run() {
    }
}
