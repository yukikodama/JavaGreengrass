package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import org.iot.raspberry.grovepi.GroveAnalogIn;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumiditySensor;
import org.iot.raspberry.grovepi.devices.GroveTemperatureAndHumidityValue;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoistureSensor extends TimerTask {
    private static final String TOPIC = "topic/moistureSensor";
    private static final String CPUINFO = "/proc/cpuinfo";

    private IotDataClient iotDataClient = new IotDataClient();
    private AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    private String serial;
    private GrovePi4J grovepi = new GrovePi4J();
    private GroveAnalogIn analogIn2;
    private GroveTemperatureAndHumiditySensor dht2;

    private int count = 0;
    private long createAt;

    static {
        try {
            new Timer().scheduleAtFixedRate(new MoistureSensor(), 0, 10000);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    private MoistureSensor() throws Exception {
        Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(new String(Files.readAllBytes(Paths.get(CPUINFO))));
        serial = m.find() ? m.group(1) : "none";
        createAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
        analogIn2 = grovepi.getAnalogIn(2, 4);
        dht2 = new GroveTemperatureAndHumiditySensor(grovepi, 2, GroveTemperatureAndHumiditySensor.Type.DHT11);
    }

    @Override
    public void run() {
        try {
            long updateAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
            byte[] b = analogIn2.get();
            GroveTemperatureAndHumidityValue v = dht2.get();
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }

    public String handleRequest(Object input, Context context) {
        return "ok";
    }
}
