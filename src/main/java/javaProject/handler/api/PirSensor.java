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
import java.util.Map;
import java.util.stream.Stream;

public class PirSensor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static LambdaLogger logger = null;

    private AmazonDynamoDB amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        logger = context.getLogger();
        logger.log("event: " + event);
        logger.log("context: " + context);

        ScanResult scanResult = amazonDynamoDB.scan(new ScanRequest().withTableName("Sensor"));
        logger.log(scanResult.toString());
        Stream<String> sensorIds = scanResult.getItems().stream().map(item -> item.get("SensorId").getS());
        sensorIds.forEach(sensorId -> {
            Map<String, AttributeValue> values = new HashMap<>();
            values.put(":s", new AttributeValue().withS(sensorId));
            QueryRequest queryRequest = new QueryRequest().withTableName("PirSensor").withKeyConditionExpression("SensorId = :s").withExpressionAttributeValues(values).withLimit(10).withScanIndexForward(false);
            QueryResult queryResult = amazonDynamoDB.query(queryRequest);
            logger.log("queryResult: " + queryResult.getItems());
        });

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(new JSONObject().put("Output", "Hello Pir Sensor!").toString());
    }
}