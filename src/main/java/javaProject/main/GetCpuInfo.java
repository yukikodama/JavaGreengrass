package javaProject.main;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetCpuInfo {
    public static void main(String... arg) {
        try {
            String cpuinfo = new String(Files.readAllBytes(Paths.get("/proc/cpuinfo")));
            Matcher m = Pattern.compile("Serial\\s+:\\s+(\\w{16})").matcher(cpuinfo);
            System.out.println(m.find() ? m.group(1): "none");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
