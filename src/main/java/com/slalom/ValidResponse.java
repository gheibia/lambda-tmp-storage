package com.slalom;

public class ValidResponse {
    private final long directorySize;

    public ValidResponse(long size) {
        directorySize = size;
    }

    private long getDirectorySize() {
        return directorySize;
    }
}