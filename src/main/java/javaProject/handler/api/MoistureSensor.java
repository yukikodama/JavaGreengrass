package javaProject.handler.api;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.json.JSONObject;

import java.util.Map;

public class MoistureSensor extends PirSensor {

    protected QueryRequest createQueryRequest() {
        return new QueryRequest().withTableName("JavaGreengrassSensorType")
                .withKeyConditionExpression("SensorType = :t ")
                .withFilterExpression("Uses = :u")
                .addExpressionAttributeValuesEntry(":t", new AttributeValue().withS("moisture"))
                .addExpressionAttributeValuesEntry(":u", new AttributeValue().withS("moisture"));
    }

    protected JSONObject createJSONObject(final Map<String, AttributeValue> m) {
        return new JSONObject()
                .put("SensorId", m.get("SensorId").getS())
                .put("CreateAt", Long.valueOf(m.get("CreateAt").getN()))
                .put("UpdateAt", Long.valueOf(m.get("UpdateAt").getN()))
                .put("Moisture", Integer.valueOf(m.get("Moisture").getN()))
                .put("Temperature", Integer.valueOf(m.get("Moisture").getN()))
                .put("Humidity", Integer.valueOf(m.get("Moisture").getN()))
                .put("TTL", Integer.valueOf(m.get("TTL").getN()));
    }

    protected String getSensorTableName() {
        return "JavaGreengrassMoistureSensor";
    }
}
