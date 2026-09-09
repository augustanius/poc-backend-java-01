package com.poc.app.frameworks.datasource;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class DataSourceRoutingContext {

    public enum DataSourceType {
        PRIMARY,
        REPLICA
    }

    private DataSourceRoutingContext() {}

    public static DataSourceType determineDataSource() {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return DataSourceType.REPLICA;
        }
        return DataSourceType.PRIMARY;
    }
}
