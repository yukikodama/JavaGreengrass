package javaProject.handler.api;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;

import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PirSensor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static LambdaLogger logger = null;

    private AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
    private ScanRequest sensorScanRequest = new ScanRequest().withTableName("JavaGreengrassSensor");
    protected Map<String, String> headers = new HashMap<>();

    public PirSensor() {
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
    }

    protected void postExecute() {
        amazonDynamoDB.putItem(new PutItemRequest().withTableName("JavaGreengrassRequest").addItemEntry("RequestType", new AttributeValue().withS("restroom")).addItemEntry("Request", new AttributeValue().withN("1")));
    }

    int getLimit(final APIGatewayProxyRequestEvent event) {
        try {
            int limit = Integer.valueOf(event.getQueryStringParameters().get("limit"));
            return (limit <= 0 || 60 < limit) ? 1 : limit;
        } catch (Exception e) {
            return 1;
        }
    }

    protected JSONObject createJSONObject(final Map<String, AttributeValue> m) {
        return new JSONObject()
                .put("SensorId", m.get("SensorId").getS())
                .put("CreateAt", Long.valueOf(m.get("CreateAt").getN()))
                .put("UpdateAt", Long.valueOf(m.get("UpdateAt").getN()))
                .put("Pir", Integer.valueOf(m.get("Pir").getN()))
                .put("Light", Integer.valueOf(m.get("Light").getN()))
                .put("Sound", Integer.valueOf(m.get("Sound").getN()))
                .put("During", Integer.valueOf(m.get("During").getN()))
                .put("TTL", Integer.valueOf(m.get("TTL").getN()))
                .put("Request", Integer.valueOf(m.get("Request").getN()));
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        logger = context.getLogger();
        logger.log("event: " + event);
        logger.log("context: " + context);

        if ("POST".equals(event.getHttpMethod().toUpperCase())) {
            postExecute();
        }

        ScanResult scanResult = amazonDynamoDB.scan(sensorScanRequest);
        List<JSONObject> pirSensor = scanResult.getItems().stream().map(item -> item.get("SensorId").getS()).map(sensorId -> {
            QueryResult queryResult = amazonDynamoDB.query(new QueryRequest().withTableName("JavaGreengrassPirSensor").withKeyConditionExpression("SensorId = :s").addExpressionAttributeValuesEntry(":s", new AttributeValue().withS(sensorId)).withLimit(this.getLimit(event)).withScanIndexForward(false));
            return queryResult.getItems().stream().map(m -> this.createJSONObject(m));
        }).flatMap(m -> m).peek(System.out::println).distinct().collect(Collectors.toList());

        return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(new JSONArray().put(pirSensor).toString());
    }
}