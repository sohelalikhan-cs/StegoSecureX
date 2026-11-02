package com.securestegox;

public class HistoryModel {

    private String operation;
    private String fileName;
    private String dateTime;
    private String status;

    public HistoryModel(String operation,
                        String fileName,
                        String dateTime,
                        String status) {

        this.operation = operation;
        this.fileName = fileName;
        this.dateTime = dateTime;
        this.status = status;
    }

    public String getOperation() {
        return operation;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getStatus() {
        return status;
    }
}