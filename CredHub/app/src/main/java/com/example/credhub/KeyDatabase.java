package com.example.credhub;

import android.provider.BaseColumns;

public final class KeyDatabase {

    private KeyDatabase() {}

    //Class with the table data for the local database
    public static class KeyEntry implements BaseColumns {
        public static final String TABLE_NAME = "storeKey";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLUMN_NAME_PASSWORD = "password";
    }

    //Initial table creation
    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE if not exists " + KeyDatabase.KeyEntry.TABLE_NAME + " ("+
                    KeyDatabase.KeyEntry.COLUMN_NAME_ID + " TEXT PRIMARY KEY,"+
                    KeyDatabase.KeyEntry.COLUMN_NAME_USERNAME + " TEXT," +
                    KeyDatabase.KeyEntry.COLUMN_NAME_PASSWORD + " TEXT)";

    //Full table deletion
    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + KeyDatabase.KeyEntry.TABLE_NAME;
}
