package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.amazonaws.services.lambda.runtime.Context;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    private final String publishMessage;
    private final PublishRequest publishRequest;

    public PublishHelloWorld() {
        String serial = null;
        try {
            Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(new String(Files.readAllBytes(Paths.get("/proc/cpuinfo"))));
            serial = m.find() ? m.group(1) : "none";
        } catch (Exception e) {
            System.err.println(e);
        }
        publishMessage = String.format("Hello world! Sent from Greengrass Core running on platform: %s-%s, Serial: %s", System.getProperty("os.name"), System.getProperty("os.version"), serial);
        publishRequest = new PublishRequest().withTopic("hello/world").withPayload(ByteBuffer.wrap(String.format("{\"message\":\"%s\"}", publishMessage).getBytes()));
    }

    public void run() {
        try {
            iotDataClient.publish(publishRequest);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
