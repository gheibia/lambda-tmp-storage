package com.slalom;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TempStorageCalculator
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DIR_PATH = "/tmp";
    private static final String FILE_NAME_ENV_VAR = "S3_FILE_NAME";
    private static final String BUCKET_NAME_ENV_VAR = "S3_BUCKET_NAME";
    private final String FILE_NAME;
    private final String BUCKET_NAME;

    public TempStorageCalculator() {
        String fileName;
        String bucketName;
        try {
            fileName = System.getenv(FILE_NAME_ENV_VAR);
            bucketName = System.getenv(BUCKET_NAME_ENV_VAR);
        } catch (Exception ex) {
            fileName = "";
            bucketName = "";
        }
        FILE_NAME = fileName;
        BUCKET_NAME = bucketName;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        if (FILE_NAME.isBlank() || BUCKET_NAME.isBlank()) {
            return logErrorAndRespond(context, "Incorrect setup", null);
        }
        try {
            getFileFromS3();
            final var size = FileUtils.sizeOfDirectory(new File(DIR_PATH));
            return logAndRespond(context, size);
        } catch (SdkClientException | AwsServiceException ex) {
            return logErrorAndRespond(context, "Downloading file from S3 failed", ex);
        } catch (IOException | SecurityException ex) {
            return logErrorAndRespond(context, "Storing file failed", ex);
        } catch(Exception ex) {
            return logErrorAndRespond(context, "Unhandled exception", ex);
        }
    }

    private void getFileFromS3() throws IOException {
        final var s3 = S3Client.builder().build();
        final var getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(FILE_NAME)
                .build();

        final var response = s3.getObject(getObjectRequest);
        final var fileName = new StringBuilder(DIR_PATH).append("/").append(Instant.now().getEpochSecond()).toString();

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            fos.write(response.readAllBytes());
        } catch (IOException ex) {
            throw ex;
        }
    }

    private APIGatewayProxyResponseEvent logAndRespond(Context context, long size) {
        LambdaLogger logger = context.getLogger();
        logger.log(String.format("Directory size: %d", size));

        var response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        response.setStatusCode(200);
        response.setBody(GSON.toJson(new ValidResponse(size)));
        return response;
    }

    private APIGatewayProxyResponseEvent logErrorAndRespond(Context context, String msg, Exception ex) {
        LambdaLogger logger = context.getLogger();
        logger.log(msg);
        if (ex != null)
            logger.log(String.format("Error: %s, %s", ex.getMessage(), Arrays.toString(ex.getStackTrace())));

        var response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setHeaders(headers);
        response.setStatusCode(400);
        response.setBody(GSON.toJson(new ErrorResponse(msg)));
        return response;
    }
}