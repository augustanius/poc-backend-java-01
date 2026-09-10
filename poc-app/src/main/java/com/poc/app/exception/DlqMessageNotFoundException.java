package com.poc.app.exception;

import com.poc.common.exception.NotFoundException;

import java.util.Map;

public class DlqMessageNotFoundException extends NotFoundException {

    public DlqMessageNotFoundException(String id) {
        super("DLQ message not found: " + id, Map.of("dlqMessageId", id));
    }
}
