package javaProject.handler.greengrass;

import com.amazonaws.greengrass.javasdk.IotDataClient;
import com.amazonaws.services.lambda.runtime.Context;
import org.iot.raspberry.grovepi.GrovePi;
import org.iot.raspberry.grovepi.GroveUtil;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseSensor extends TimerTask {
    private static final String CPUINFO = "/proc/cpuinfo";
    protected IotDataClient iotDataClient = new IotDataClient();
    protected GrovePi grovepi;
    protected String serial;
    protected long createAt;

    public BaseSensor() throws Exception {
        Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(new String(Files.readAllBytes(Paths.get(CPUINFO))));
        serial = m.find() ? m.group(1) : "none";
        createAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
        grovepi = new GrovePi4J();
    }

    protected int getAnalogValue(byte[] value) {
        int[] i = GroveUtil.unsign(value);
        return (i[1] * 256) + i[2];
    }

    protected String getSystemEnv(String name) {
        return System.getenv(name);
    }

    public String handleRequest(Object event, Context context) {
        return "ok";
    }

    protected boolean isWorktime() {
        Calendar calendar = Calendar.getInstance();
        int week_int = calendar.get(Calendar.DAY_OF_WEEK);
        int hour_int = calendar.get(Calendar.HOUR_OF_DAY);
        return (2 <= week_int && week_int <= 7) && (8 <= hour_int && hour_int <= 21);
    }
}
