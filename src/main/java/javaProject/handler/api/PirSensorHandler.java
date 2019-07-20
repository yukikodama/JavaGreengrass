package javaProject.handler.api;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for requests to Lambda function.
 */
public class PirSensorHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    static LambdaLogger logger = null;
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        logger = context.getLogger();
        logger.log("event: " + event);
        logger.log("context: " + context);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(new JSONObject().put("Output", "Hello World!").toString());
    }
}
