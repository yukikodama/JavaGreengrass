package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.greengrass.javasdk.model.PublishRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import org.iot.raspberry.grovepi.GroveAnalogIn;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumidityValue;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;
import org.json.JSONObject;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Timer;

public class MoistureSensor extends BaseSensor {
    private static final String TOPIC = "topic/moisturesensor";


    private AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    private GrovePi grovepi;
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
        grovepi = new GrovePi4J();
        analogIn2 = grovepi.getAnalogIn(2, 4);
        dht2 = new GroveTemperatureAndHumiditySensor(grovepi, 2, GroveTemperatureAndHumiditySensor.Type.DHT11);
        amazonDynamoDB.putItem(new PutItemRequest().withTableName("JavaGreengrassSensorType").addItemEntry("SensorType", new AttributeValue().withS("moisture")).addItemEntry("SensorId", new AttributeValue().withS(serial)).addItemEntry("Uses", new AttributeValue().withS("moisture")));
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
            amazonDynamoDB.putItem(new PutItemRequest()
                    .withTableName("JavaGreengrassMoistureSensor")
                    .addItemEntry("SensorId", new AttributeValue().withS(serial))
                    .addItemEntry("CreateAt", new AttributeValue().withN(String.valueOf(createAt)))
                    .addItemEntry("UpdateAt", new AttributeValue().withN(String.valueOf(updateAt)))
                    .addItemEntry("Moisture", new AttributeValue().withN(String.valueOf(moisture)))
                    .addItemEntry("Temperature", new AttributeValue().withN(String.valueOf(temperature)))
                    .addItemEntry("Humidity", new AttributeValue().withN(String.valueOf(humidity)))
                    .addItemEntry("TTL", new AttributeValue().withN(String.valueOf((updateAt / 1000) + 900)))
            );
            iotDataClient.publish(new PublishRequest().withTopic(TOPIC).withPayload(ByteBuffer.wrap(publishMessage.getBytes())));
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
