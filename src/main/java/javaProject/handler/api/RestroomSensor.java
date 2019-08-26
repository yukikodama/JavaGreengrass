package javaProject.handler.api;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.json.JSONObject;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.HashMap;
import java.util.Map;

public class RestroomSensor extends PirSensor {

    protected int getLimit(final APIGatewayProxyRequestEvent event) {
        return 1;
    }

    protected QueryRequest createQueryRequest() {
        HashMap<String, AttributeValue> values = new HashMap<>();
        values.put(":t", AttributeValue.builder().s("pir").build());
        values.put(":u", AttributeValue.builder().s("restroom").build());
        return QueryRequest.builder().tableName("JavaGreengrassSensorType")
                .keyConditionExpression("SensorType = :t ")
                .filterExpression("Uses = :u")
                .expressionAttributeValues(values).build();
    }

    protected JSONObject createJSONObject(final Map<String, AttributeValue> m) {
        return new JSONObject()
                .put("SensorId", m.get("SensorId").s())
                .put("UpdateAt", Long.valueOf(m.get("UpdateAt").n()))
                .put("Pir", Integer.valueOf(m.get("Pir").n()));
    }
}
