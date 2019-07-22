package javaProject.main;

import org.iot.raspberry.grovepi.GroveDigitalOut;
import org.iot.raspberry.grovepi.pi4j.GrovePi4J;

public class GrovePiBlinkingLed {
    public static final void main(String... args) {
        try {
            GroveDigitalOut led = new GrovePi4J().getDigitalOut(4);
            led.set(true);
            Thread.sleep(5000);
            led.set(false);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}