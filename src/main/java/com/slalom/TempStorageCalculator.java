package com.slalom;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TempStorageCalculator
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DIR_PATH = "/tmp";
    private static final int DEFAULT_FILE_SIZE_MB = 100;
    private int FILE_SIZE_MB;

    public TempStorageCalculator() {
        try {
            FILE_SIZE_MB = Integer.valueOf(System.getenv("FILE_SIZE_TO_CREATE_MB"));
        } catch (Exception ex) {
            FILE_SIZE_MB = DEFAULT_FILE_SIZE_MB;
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        try {
            final var fileName = new StringBuilder(DIR_PATH).append("/").append(Instant.now().getEpochSecond())
                    .toString();
            createFile(fileName, FILE_SIZE_MB * 1000 * 1000);
            final var size = FileUtils.sizeOfDirectory(new File(DIR_PATH));
            return logAndRespond(context, size);
        } catch (NullPointerException ex) {
            return logErrorAndRespond(context, "/tmp not found", ex);
        } catch (IOException | SecurityException ex) {
            return logErrorAndRespond(context, "Creating temp file failed", ex);
        }
    }

    private void createFile(final String filename, final long sizeInBytes) throws IOException {
        File file = new File(filename);
        file.createNewFile();

        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(sizeInBytes);
        raf.close();
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