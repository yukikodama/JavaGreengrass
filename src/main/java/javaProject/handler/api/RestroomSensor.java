package javaProject.handler.api;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import org.json.JSONObject;

import java.util.Map;

public class RestroomSensor extends PirSensor {
    protected QueryRequest createQueryRequest() {
        return new QueryRequest().withTableName("JavaGreengrassSensorType").withKeyConditionExpression("SensorType = :t").addExpressionAttributeValuesEntry(":t", new AttributeValue().withS("pir"));
    }

    protected JSONObject createJSONObject(final Map<String, AttributeValue> m) {
        return new JSONObject()
                .put("SensorId", m.get("SensorId").getS())
                .put("UpdateAt", Long.valueOf(m.get("UpdateAt").getN()))
                .put("Pir", Integer.valueOf(m.get("Pir").getN()));
    }
}
