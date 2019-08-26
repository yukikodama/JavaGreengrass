package javaProject.handler.greengrass;

import org.apache.commons.lang3.BooleanUtils;
import org.iot.raspberry.grovepi.GroveDigitalIn;
import org.iot.raspberry.grovepi.GroveDigitalOut;
import org.iot.raspberry.grovepi.devices.GroveLightSensor;
import org.iot.raspberry.grovepi.devices.GroveSoundSensor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Timer;

public class PirSensor extends BaseSensor {
    private static final String TOPIC = "topic/pirsensor";
    private DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    private HashMap<String, AttributeValue> keyValues = new HashMap<>();
    private HashMap<String, AttributeValue> putIValues = new HashMap<>();
    private GetItemRequest getItemRequest;
    private PutItemRequest putItemRequest;
    private GroveLightSensor lightSensor0;
    private GroveSoundSensor soundSensor1;
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
        keyValues.put("RequestType", AttributeValue.builder().s("restroom").build());
        getItemRequest = GetItemRequest.builder().tableName("JavaGreengrassRequest").key(keyValues).build();
        putIValues.put("RequestType", AttributeValue.builder().s("restroom").build());
        putIValues.put("Request", AttributeValue.builder().n("0").build());
        putItemRequest = PutItemRequest.builder().tableName("JavaGreengrassRequest").item(putIValues).build();
        lightSensor0 = new GroveLightSensor(grovepi, 0);
        soundSensor1 = new GroveSoundSensor(grovepi, 1);
        digitalIn2 = grovepi.getDigitalIn(2);
        digitalOut3 = grovepi.getDigitalOut(3);
        digitalOut4 = grovepi.getDigitalOut(4);

        HashMap<String, AttributeValue> itemValues = new HashMap<>();
        itemValues.put("SensorType", AttributeValue.builder().s("pir").build());
        itemValues.put("SensorId", AttributeValue.builder().s(serial).build());
        itemValues.put("Uses", AttributeValue.builder().s(getSystemEnv("USES")).build()); //restroom or pir
        dynamoDbClient.putItem(PutItemRequest.builder().tableName("JavaGreengrassSensorType").item(itemValues).build());
    }

    @Override
    public void run() {
        try {
            long updateAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
            boolean b = digitalIn2.get();
            digitalOut3.set(b);
            int light = lightSensor0.get().intValue();
            int sound = soundSensor1.get().intValue();
            int request = 0;
            if (b) {
                request = Integer.valueOf(dynamoDbClient.getItem(getItemRequest).item().get("Request").n());
                digitalOut4.set(BooleanUtils.toBoolean(request));
            } else {
                dynamoDbClient.putItem(putItemRequest);
                digitalOut4.set(false);
            }
            HashMap<String, AttributeValue> itemValues = new HashMap<>();
            itemValues.put("SensorId", AttributeValue.builder().s(serial).build());
            itemValues.put("CreateAt", AttributeValue.builder().n(String.valueOf(createAt)).build());
            itemValues.put("UpdateAt", AttributeValue.builder().n(String.valueOf(updateAt)).build());
            itemValues.put("Pir", AttributeValue.builder().n(String.valueOf(BooleanUtils.toInteger(b))).build());
            itemValues.put("Light", AttributeValue.builder().n(String.valueOf(light)).build());
            itemValues.put("Sound", AttributeValue.builder().n(String.valueOf(sound)).build());
            itemValues.put("TTL", AttributeValue.builder().n(String.valueOf((updateAt / 1000) + 900)).build());
            itemValues.put("Request", AttributeValue.builder().n(String.valueOf(request)).build());
            dynamoDbClient.putItem(PutItemRequest.builder().tableName("JavaGreengrassPirSensor").item(itemValues).build());
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
