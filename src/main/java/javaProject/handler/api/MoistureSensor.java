package javaProject.handler.api;

import org.json.JSONObject;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.HashMap;
import java.util.Map;

public class MoistureSensor extends PirSensor {

    protected QueryRequest createQueryRequest() {
        HashMap<String, AttributeValue> values = new HashMap<>();
        values.put(":t", AttributeValue.builder().s("moisture").build());
        values.put(":u", AttributeValue.builder().s("moisture").build());
        return QueryRequest.builder().tableName("JavaGreengrassSensorType")
                .keyConditionExpression("SensorType = :t ")
                .filterExpression("Uses = :u")
                .expressionAttributeValues(values).build();
    }

    protected JSONObject createJSONObject(final Map<String, AttributeValue> m) {
        return new JSONObject()
                .put("SensorId", m.get("SensorId").s())
                .put("CreateAt", Long.valueOf(m.get("CreateAt").n()))
                .put("UpdateAt", Long.valueOf(m.get("UpdateAt").n()))
                .put("Moisture", Integer.valueOf(m.get("Moisture").n()))
                .put("Temperature", Integer.valueOf(m.get("Moisture").n()))
                .put("Humidity", Integer.valueOf(m.get("Moisture").n()))
                .put("TTL", Integer.valueOf(m.get("TTL").n()));
    }

    protected String getSensorTableName() {
        return "JavaGreengrassMoistureSensor";
    }
}
