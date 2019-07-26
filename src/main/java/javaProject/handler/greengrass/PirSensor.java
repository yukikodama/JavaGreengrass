package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.amazonaws.regions.Regions;
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
    private GroveDigitalOut digitalOut4;
    private int count = 0;

    static {
        try {
            new Timer().scheduleAtFixedRate(new PirSensor(), 0, 10000);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private PirSensor() throws Exception {
        Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(new String(Files.readAllBytes(Paths.get(CPUINFO))));
        serial = m.find() ? m.group(1) : "none";
        digitalIn2 = new GrovePi4J().getDigitalIn(2);
        digitalOut3 = new GrovePi4J().getDigitalOut(3);
        digitalOut4 = new GrovePi4J().getDigitalOut(4);
    }

    public Item getItem() {
        return table.getItem(new GetItemSpec().withPrimaryKey("RequestType", "restroom"));
    }

    public void putItem() {
        table.putItem(new Item().withPrimaryKey("RequestType", "restroom").withInt("Request", 0));
    }

    @Override
    public void run() {
        try {
            boolean b = digitalIn2.get();
            digitalOut3.set(b);
            int request = 0;
            if (b) {
                request = ((java.math.BigDecimal) getItem().get("Request")).intValue();
                digitalOut4.set(BooleanUtils.toBoolean(request));
            } else {
                putItem();
                digitalOut4.set(false);
            }
            String publishMessage = new JSONObject().put("SensorId", serial).put("Pir", BooleanUtils.toInteger(b)).put("During", 10 * count++).put("request", request).toString();
            iotDataClient.publish(new PublishRequest().withTopic(TOPIC).withPayload(ByteBuffer.wrap(publishMessage.getBytes())));
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public String handleRequest(Object input, Context context) {
        return "ok";
    }
}
