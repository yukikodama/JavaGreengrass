package javaProject.handler.greengrass;

import com.amazonaws.services.lambda.runtime.Context;
import org.iot.raspberry.grovepi.GroveUtil;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseSensor extends TimerTask {
    private static final String CPUINFO = "/proc/cpuinfo";
    protected String serial;
    protected long createAt;

    public BaseSensor() throws Exception {
        Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(new String(Files.readAllBytes(Paths.get(CPUINFO))));
        serial = m.find() ? m.group(1) : "none";
        createAt = LocalDateTime.now().atZone(ZoneOffset.ofHours(+9)).toInstant().toEpochMilli();
    }

    protected int getAnalogValue(byte[] value) {
        int[] i = GroveUtil.unsign(value);
        return (i[1] * 256) + i[2];
    }

    public String handleRequest(Object input, Context context) {
        return "ok";
    }
}
