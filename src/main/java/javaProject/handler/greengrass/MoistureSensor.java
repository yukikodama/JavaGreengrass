package javaProject.handler.greengrass;


import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import software.amazon.awssdk.regions.Region;

import org.iot.raspberry.grovepi.GroveAnalogIn;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumidityValue;
import org.json.JSONObject;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Timer;

public class MoistureSensor extends BaseSensor {
    private static final String TOPIC = "topic/moisturesensor";
    private DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    private GroveAnalogIn analogIn2;
    private GroveTemperatureAndHumiditySensor dht2;

    static {
        try {
            new Timer().scheduleAtFixedRate(new MoistureSensor(), 0, 10000);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private MoistureSensor() throws Exception {
        analogIn2 = grovepi.getAnalogIn(2, 4);
        dht2 = new GroveTemperatureAndHumiditySensor(grovepi, 2, GroveTemperatureAndHumiditySensor.Type.DHT11);

        HashMap<String,AttributeValue> itemValues = new HashMap<>();
        itemValues.put("SensorType", AttributeValue.builder().s("moisture").build());
        itemValues.put("SensorId", AttributeValue.builder().s(serial).build());
        itemValues.put("Uses", AttributeValue.builder().s("moisture").build());
        dynamoDbClient.putItem(PutItemRequest.builder().tableName("JavaGreengrassSensorType").item(itemValues).build());
    }

    @Override
    public void run() {
        try {
            long updateAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
            GroveTemperatureAndHumidityValue dht = dht2.get();
            int moisture = this.getAnalogValue(analogIn2.get());
            int temperature = (int) dht.getTemperature();
            int humidity = (int) dht.getHumidity();
            String publishMessage = new JSONObject()
                    .put("SensorId", serial)
                    .put("CreateAt", createAt)
                    .put("UpdateAt", updateAt)
                    .put("Moisture", moisture)
                    .put("Temperature", temperature)
                    .put("Humidity", humidity)
                    .put("TTL", (updateAt / 1000) + 900)
                    .toString();
            HashMap<String,AttributeValue> itemValues = new HashMap<>();
            itemValues.put("SensorId", AttributeValue.builder().s(serial).build());
            itemValues.put("CreateAt", AttributeValue.builder().n(String.valueOf(createAt)).build());
            itemValues.put("UpdateAt", AttributeValue.builder().n(String.valueOf(updateAt)).build());
            itemValues.put("Moisture", AttributeValue.builder().n(String.valueOf(moisture)).build());
            itemValues.put("Temperature", AttributeValue.builder().n(String.valueOf(temperature)).build());
            itemValues.put("Humidity", AttributeValue.builder().n(String.valueOf(temperature)).build());
            itemValues.put("TTL", AttributeValue.builder().n(String.valueOf((updateAt / 1000) + 900)).build());
            dynamoDbClient.putItem(PutItemRequest.builder().tableName("JavaGreengrassMoistureSensor").item(itemValues).build());
            iotDataClient.publish(new PublishRequest().withTopic(TOPIC).withPayload(ByteBuffer.wrap(publishMessage.getBytes())));
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
