package javaProject.handler.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PirSensor implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static LambdaLogger logger = null;

    private DynamoDbClient dynamoDbClient = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    protected Map<String, String> headers = new HashMap<>();

    public PirSensor() {
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
    }

    protected void postExecute() {
        HashMap<String, AttributeValue> putIValues = new HashMap<>();
        putIValues.put("RequestType", AttributeValue.builder().s("restroom").build());
        putIValues.put("Request", AttributeValue.builder().n("1").build());
        dynamoDbClient.putItem(PutItemRequest.builder().tableName("JavaGreengrassRequest").item(putIValues).build());
    }

    protected int getLimit(final APIGatewayProxyRequestEvent event) {
        try {
            int limit = Integer.valueOf(event.getQueryStringParameters().get("limit"));
            return (limit <= 0 || 60 < limit) ? 1 : limit;
        } catch (Exception e) {
            return 1;
        }
    }

    protected JSONObject createJSONObject(final Map<String, AttributeValue> m) {
        return new JSONObject()
                .put("SensorId", m.get("SensorId").s())
                .put("CreateAt", Long.valueOf(m.get("CreateAt").n()))
                .put("UpdateAt", Long.valueOf(m.get("UpdateAt").n()))
                .put("Pir", Integer.valueOf(m.get("Pir").n()))
                .put("Light", Integer.valueOf(m.get("Light").n()))
                .put("Sound", Integer.valueOf(m.get("Sound").n()))
                .put("During", Integer.valueOf(m.get("During").n()))
                .put("TTL", Integer.valueOf(m.get("TTL").n()))
                .put("Request", Integer.valueOf(m.get("Request").n()));
    }

    protected QueryRequest createQueryRequest() {
        HashMap<String, AttributeValue> values = new HashMap<>();
        values.put(":t", AttributeValue.builder().s("pir").build());
        return QueryRequest.builder().tableName("JavaGreengrassSensorType").keyConditionExpression("SensorType = :t").expressionAttributeValues(values).build();
    }

    protected String getSensorTableName() {
        return "JavaGreengrassPirSensor";
    }

    boolean isWorktime() {
        Calendar calendar = Calendar.getInstance();
        int week_int = calendar.get(Calendar.DAY_OF_WEEK);
        int hour_int = calendar.get(Calendar.HOUR_OF_DAY);
        logger.log("DAY_OF_WEEK: " + week_int);
        logger.log("HOUR_OF_DAY: " + hour_int);
        return (2 <= week_int && week_int <= 7) && (0 <= hour_int && hour_int <= 12);
    }

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        logger = context.getLogger();
        logger.log("event: " + event);
        logger.log("context: " + context);

        if (!isWorktime()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(new JSONObject().put("results", java.util.Collections.emptyList()).toString());
        }

        if ("POST".equals(event.getHttpMethod().toUpperCase())) {
            postExecute();
        }
        List<JSONObject> pirSensor = dynamoDbClient.query(createQueryRequest()).items().stream().map(item -> item.get("SensorId").s()).map(sensorId -> {
            HashMap<String, AttributeValue> values = new HashMap<>();
            values.put(":s", AttributeValue.builder().s(sensorId).build());
            QueryRequest queryRequest = QueryRequest.builder().tableName(getSensorTableName()).keyConditionExpression("SensorId = :s").expressionAttributeValues(values).limit(this.getLimit(event)).scanIndexForward(false).build();
            QueryResponse queryResult = dynamoDbClient.query(queryRequest);
            return queryResult.items().stream().map(m -> this.createJSONObject(m));
        }).flatMap(m -> m).peek(System.out::println).distinct().collect(Collectors.toList());
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(new JSONObject().put("results", pirSensor).toString());
    }
}