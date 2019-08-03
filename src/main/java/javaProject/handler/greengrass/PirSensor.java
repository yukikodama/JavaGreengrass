package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.commons.lang3.BooleanUtils;
import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GroveDigitalOut;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveLightSensor;
import org.iot.raspberry.grovepi.devices.GroveSoundSensor;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PirSensor extends TimerTask {
    private static final String TOPIC = "topic/pirsensor";
    private static final String CPUINFO = "/proc/cpuinfo";

    private IotDataClient iotDataClient = new IotDataClient();
    private AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    private GetItemRequest requestGetItemRequest = new GetItemRequest().withTableName("JavaGreengrassRequest").addKeyEntry("RequestType", new AttributeValue().withS("restroom"));
    private PutItemRequest requestPutItemRequest = new PutItemRequest().withTableName("JavaGreengrassRequest").addItemEntry("RequestType", new AttributeValue().withS("restroom")).addItemEntry("Request", new AttributeValue().withN("0"));
    private String serial;
    private GroveLightSensor lightSensor0;
    private GroveSoundSensor soundSensor1;
    private GroveDigitalIn digitalIn7;
    private GroveDigitalOut digitalOut3;
    private GroveDigitalOut digitalOut4;
    private int count = 0;
    private long createAt;

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
        createAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
        GrovePi grovepi = new GrovePi4J();
        lightSensor0 = new GroveLightSensor(grovepi, 0);
        soundSensor1 = new GroveSoundSensor(grovepi, 1);
        digitalIn7 = grovepi.getDigitalIn(7);
        digitalOut3 = grovepi.getDigitalOut(3);
        digitalOut4 = grovepi.getDigitalOut(4);
        amazonDynamoDB.putItem(new PutItemRequest().withTableName("JavaGreengrassSensorType").addItemEntry("SensorType", new AttributeValue().withS("pir")).addItemEntry("SensorId", new AttributeValue().withS(serial)).addItemEntry("Uses", new AttributeValue().withS(getSystemEnv("USES"))));
    }

    @Override
    public void run() {
        try {
            long updateAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
            boolean b = digitalIn7.get();
            digitalOut3.set(b);
            int light = lightSensor0.get().intValue();
            int sound = soundSensor1.get().intValue();
            int request = 0;
            if (b) {
                request = Integer.valueOf(amazonDynamoDB.getItem(requestGetItemRequest).getItem().get("Request").getN());
                digitalOut4.set(BooleanUtils.toBoolean(request));
            } else {
                amazonDynamoDB.putItem(requestPutItemRequest);
                digitalOut4.set(false);
            }
            String publishMessage = new JSONObject()
                    .put("SensorId", serial)
                    .put("CreateAt", createAt)
                    .put("UpdateAt", updateAt)
                    .put("Pir", BooleanUtils.toInteger(b))
                    .put("Light", light)
                    .put("Sound", sound)
                    .put("During", count)
                    .put("TTL", updateAt / 1000)
                    .put("Request", request).toString();
            amazonDynamoDB.putItem(new PutItemRequest()
                    .withTableName("JavaGreengrassPirSensor")
                    .addItemEntry("SensorId", new AttributeValue().withS(serial))
                    .addItemEntry("CreateAt", new AttributeValue().withN(String.valueOf(createAt)))
                    .addItemEntry("UpdateAt", new AttributeValue().withN(String.valueOf(updateAt)))
                    .addItemEntry("Pir", new AttributeValue().withN(String.valueOf(BooleanUtils.toInteger(b))))
                    .addItemEntry("Light", new AttributeValue().withN(String.valueOf(light)))
                    .addItemEntry("Sound", new AttributeValue().withN(String.valueOf(sound)))
                    .addItemEntry("During", new AttributeValue().withN(String.valueOf(count++)))
                    .addItemEntry("TTL", new AttributeValue().withN(String.valueOf(updateAt / 1000)))
                    .addItemEntry("Request", new AttributeValue().withN(String.valueOf(request)))
            );
            iotDataClient.publish(new PublishRequest().withTopic(TOPIC).withPayload(ByteBuffer.wrap(publishMessage.getBytes())));
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public String handleRequest(Object input, Context context) {
        return "ok";
    }

    protected String getSystemEnv(String name) {
        return System.getenv(name);
    }
}
