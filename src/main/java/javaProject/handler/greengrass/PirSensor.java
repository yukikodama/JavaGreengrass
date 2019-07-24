package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.commons.lang3.BooleanUtils;
import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GroveDigitalOut;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PirSensor extends TimerTask {
    private static final String TOPIC = "topic/pirsensor";
    private static final String CPUINFO = "/proc/cpuinfo";

    private IotDataClient iotDataClient = new IotDataClient();
    private Table table = new DynamoDB(AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build()).getTable("Request");
    private String serial;

    private GroveDigitalIn digitalIn2;
    private GroveDigitalOut digitalOut3;
    private int count = 0;

    static {
        Timer timer = new Timer();
        try {
            timer.scheduleAtFixedRate(new PirSensor(), 0, 10000);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private PirSensor() throws Exception {
        Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(new String(Files.readAllBytes(Paths.get(CPUINFO))));
        serial = m.find() ? m.group(1) : "none";
        digitalIn2 = new GrovePi4J().getDigitalIn(2);
        digitalOut3 = new GrovePi4J().getDigitalOut(3);
    }

    public Item getItem() {
        GetItemSpec getItemSpec = new GetItemSpec().withPrimaryKey("RequestType", "restroom");
        return table.getItem(getItemSpec);
    }

    @Override
    public void run() {
        try {
            boolean b = digitalIn2.get();
            String request = getItem().get("Request").toString();
            digitalOut3.set(BooleanUtils.toBoolean(request));
            String publishMessage = new JSONObject().put("SensorId", serial).put("Pir", BooleanUtils.toInteger(b)).put("Count", count++).toString();
            iotDataClient.publish(new PublishRequest().withTopic(TOPIC).withPayload(ByteBuffer.wrap(publishMessage.getBytes())));
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public String handleRequest(Object input, Context context) {
        return "ok";
    }
}
