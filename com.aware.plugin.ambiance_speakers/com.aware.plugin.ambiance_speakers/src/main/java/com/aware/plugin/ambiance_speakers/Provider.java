package com.aware.plugin.ambiance_speakers;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.Aware;
import com.aware.utils.DatabaseHelper;

import java.util.HashMap;

public class Provider extends ContentProvider {

    public static String AUTHORITY = "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers"; //change to package.provider.your_plugin_name

    public static final int DATABASE_VERSION = 4; //increase this if you make changes to the database structure, i.e., rename columns, etc.
    public static final String DATABASE_NAME = "plugin_ambiance_speakers.db"; //the database filename, use plugin_xxx for plugins.


    //For each table, add two indexes: DIR and ITEM. The index needs to always increment. Next one is 3, and so on.
    private static final int AMBIANCE_SPEAKERS = 1;
    private static final int AMBIANCE_SPEAKERS_ID = 2;

    //Put tables names in this array so AWARE knows what you have on the database
    public static final String[] DATABASE_TABLES = {
        "plugin_ambiance_speakers"
    };

    public static final String[] TABLES_FIELDS = {
            AmbianceSpeakers_Data._ID + " integer primary key autoincrement," +
                    AmbianceSpeakers_Data.TIMESTAMP + " real default 0," +
                    AmbianceSpeakers_Data.DEVICE_ID + " text default ''," +
                    AmbianceSpeakers_Data.AMBIANCE_LEVEL + " text default ''," +
                    AmbianceSpeakers_Data.RAW + " blob default null"
    };


    public static final class AmbianceSpeakers_Data implements BaseColumns {
        private AmbianceSpeakers_Data(){};

//        public static final Uri CONTENT_URI = Uri.parse("content://"+"com.aware.phone.provider.ambiance_speakers"+"/plugin_ambiance_speakers");
        public static final Uri CONTENT_URI = Uri.parse("content://"+"com.aware.plugin.ambiance_speakers.provider.ambiance_speakers"+"/plugin_ambiance_speakers");
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.ambiance_speakers";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.ambiance_speakers";

        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String AMBIANCE_LEVEL = "ambiance_level";
        public static final String RAW = "blob_raw";
    }




    //Helper variables for ContentProvider - DO NOT CHANGE
    private UriMatcher sUriMatcher;
    private DatabaseHelper dbHelper;
    private static SQLiteDatabase database;
    private void initialiseDatabase() {
        if (dbHelper == null)
            dbHelper = new DatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS);
        if (database == null)
            database = dbHelper.getWritableDatabase();
    }
    //--

    //For each table, create a hashmap needed for database queries
    private HashMap<String, String> tableOneHash;

    /**
     * Returns the provider authority that is dynamic
     * @return
     */
    public static String getAuthority(Context context) {
//        AUTHORITY = context.getPackageName() + ".provider.ambiance_speakers";
        AUTHORITY = "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers";
        return AUTHORITY;

    }

    @Override
    public boolean onCreate() {
        //This is a hack to allow providers to be reusable in any application/plugin by making the authority dynamic using the package name of the parent app
//        AUTHORITY = getAuthority(getContext());
//        AUTHORITY = getContext().getPackageName();
//        AUTHORITY = getContext().getPackageName() + ".provider.ambiance_speakers";
        AUTHORITY = "com.aware.plugin.ambiance_speakers.provider.ambiance_speakers";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        //For each table, add indexes DIR and ITEM
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], AMBIANCE_SPEAKERS);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0] + "/#", AMBIANCE_SPEAKERS_ID);

        //Create each table hashmap so Android knows how to insert data to the database. Put ALL table fields.
        tableOneHash = new HashMap<>();
        tableOneHash.put(AmbianceSpeakers_Data._ID, AmbianceSpeakers_Data._ID);
        tableOneHash.put(AmbianceSpeakers_Data.TIMESTAMP, AmbianceSpeakers_Data.TIMESTAMP);
        tableOneHash.put(AmbianceSpeakers_Data.DEVICE_ID, AmbianceSpeakers_Data.DEVICE_ID);
        tableOneHash.put(AmbianceSpeakers_Data.AMBIANCE_LEVEL, AmbianceSpeakers_Data.AMBIANCE_LEVEL);
        tableOneHash.put(AmbianceSpeakers_Data.RAW, AmbianceSpeakers_Data.RAW);

        return true;
    }

    @Override
    public synchronized int delete(Uri uri, String selection, String[] selectionArgs) {
        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case, increasing the index accordingly
            case AMBIANCE_SPEAKERS:
                count = database.delete(DATABASE_TABLES[0], selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);
        return count;
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues initialValues) {

        initialiseDatabase();

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();

        database.beginTransaction();

        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case AMBIANCE_SPEAKERS:
                long _id = database.insertWithOnConflict(DATABASE_TABLES[0], AmbianceSpeakers_Data.DEVICE_ID, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (_id > 0) {
                    Uri dataUri = ContentUris.withAppendedId(AmbianceSpeakers_Data.CONTENT_URI, _id);
                    getContext().getContentResolver().notifyChange(dataUri, null, false);
                    database.setTransactionSuccessful();
                    database.endTransaction();
                    return dataUri;
                }
                database.endTransaction();
                throw new SQLException("Failed to insert row into " + uri);
            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        initialiseDatabase();

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {

            //Add all tables' DIR entries, with the right table index
            case AMBIANCE_SPEAKERS:
                qb.setTables(DATABASE_TABLES[0]);
                qb.setProjectionMap(tableOneHash); //the hashmap of the table
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        //Don't change me
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {

            //Add each table indexes DIR and ITEM
            case AMBIANCE_SPEAKERS:
                return AmbianceSpeakers_Data.CONTENT_TYPE;
            case AMBIANCE_SPEAKERS_ID:
                return AmbianceSpeakers_Data.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public synchronized int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        initialiseDatabase();

        database.beginTransaction();

        int count;
        switch (sUriMatcher.match(uri)) {

            //Add each table DIR case
            case AMBIANCE_SPEAKERS:
                count = database.update(DATABASE_TABLES[0], values, selection, selectionArgs);
                break;

            default:
                database.endTransaction();
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        database.setTransactionSuccessful();
        database.endTransaction();

        getContext().getContentResolver().notifyChange(uri, null, false);

        return count;
    }
}
