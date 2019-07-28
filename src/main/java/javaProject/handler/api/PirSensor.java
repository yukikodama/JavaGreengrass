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
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PirSensor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static LambdaLogger logger = null;

    private AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

    int getLimit(APIGatewayProxyRequestEvent event) {
        int limit = 1;
        try {
            limit = Integer.valueOf(event.getQueryStringParameters().get("limit"));
            if (limit <= 0 || 60 < limit ) {
                limit = 1;
            }
        } catch (Exception e) {
            logger.log(e.getMessage());
        }
        return limit;
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        logger = context.getLogger();
        logger.log("event: " + event);
        logger.log("context: " + context);

        int limit = this.getLimit(event);
        ScanResult scanResult = amazonDynamoDB.scan(new ScanRequest().withTableName("Sensor"));
        logger.log(scanResult.toString());
        Stream<String> sensorIds = scanResult.getItems().stream().map(item -> item.get("SensorId").getS());
        List<JSONObject> pirSensor = sensorIds.map(sensorId -> {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":s", new AttributeValue().withS(sensorId));
            QueryRequest queryRequest = new QueryRequest().withTableName("PirSensor").withKeyConditionExpression("SensorId = :s").withExpressionAttributeValues(values).withLimit(limit).withScanIndexForward(false);
            QueryResult queryResult = amazonDynamoDB.query(queryRequest);

            Stream<JSONObject> jsonObjectStream = queryResult.getItems().stream().map(m -> {
                JSONObject j = new JSONObject().put("SensorId", m.get("SensorId").getS()).put("CreateAt", Long.valueOf(m.get("CreateAt").getN()));

                return j;
            });
            return jsonObjectStream;
            //return queryResult.getItems();
        }).flatMap(m -> m).distinct().collect(Collectors.toList());
        logger.log("pirSensor: " + pirSensor);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(new JSONObject().put("Output", "Hello Pir Sensor!").toString());
    }
}