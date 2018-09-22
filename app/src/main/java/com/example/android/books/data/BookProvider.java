package com.example.android.books.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.example.android.books.data.BookContract.BookEntry;
import android.util.Log;
import com.example.android.books.data.BookDbHelper;

import java.net.URI;

/**
 * {@link ContentProvider} for Books app.
 */
public class BookProvider extends ContentProvider {
    /** Tag for the log messages */
    public static final String LOG_TAG = BookProvider.class.getSimpleName();

    /** DB helper object */
    private BookDbHelper mDbHelper;

    /** URI matcher code for the content URI for the books table */
    private static final int BOOKS = 100;

    /** URI matcher code for the content URI for a single book in the books table */
    private static final int BOOK_ID = 101;

    /**
     * UriMatcher object to match a content URI to a corresponding code.
     * The input passed into the constructor represents the code to return for the root URI.
     * It's common to use NO_MATCH as the input for this case.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    // Static initializer. This is run the first time anything is called from this class.
    // Refer https://developer.android.com/guide/topics/providers/content-provider-creating#ContentURI
    // Designing content URIs section.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // The content URI of the form "content://com.example.android.books/books" will map to the
        // integer code {@link #BOOKS}. This URI is used to provide access to MULTIPLE rows
        // of the books table.
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS, BOOKS);

        // The content URI of the form "content://com.example.android.books/books/#" will map to the
        // integer code {@link #BOOK_ID}. This URI is used to provide access to ONE single row
        // of the books table.
        //
        // In this case, the "#" wildcard is used where "#" can be substituted for an integer.
        // For example, "content://com.example.android.books/books/3" matches, but
        // "content://com.example.android.books/books" (without a number at the end) doesn't match.
        sUriMatcher.addURI(BookContract.CONTENT_AUTHORITY, BookContract.PATH_BOOKS + "/#", BOOK_ID);
    }

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
        // TODO: Create and initialize a BookDbHelper object to gain access to the books database.
        // Make sure the variable is a global variable, so it can be referenced from other
        // ContentProvider methods.
        Log.v(LOG_TAG, "inside onCreate()");
        mDbHelper = new BookDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection, selection
     * arguments, and sort order.
     */
    @Override
    public Cursor query(Uri uri,
                        String[] projection,
                        String selection,
                        String[] selectionArgs,
                        String sortOrder) {

        Log.v(LOG_TAG, "Query method starts.");
        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        Log.v(LOG_TAG, "matching " + uri + " to " + match);
        switch (match) {
            case BOOKS:
                // For the BOOKS code, query the books table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the books table.
                // TODO: Perform database query on books table
                cursor = database.query(BookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.books/books/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the books table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(BookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the Cursor.
        // so we know what content URI the Cursor was created for.
        // If the data at this URI changes, then we know we need to update the Cursor.
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                return insertBook(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert a book into the database with the given content values. Return the new content URI
     * for that specific row in the database.
     */
    private Uri insertBook(Uri uri, ContentValues values) {
        // Check that the name is not null
        String name = values.getAsString(BookEntry.COLUMN_BOOK_PRODUCT_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Book requires a name");
        }
        // Check that the gender is valid
        Integer gender = values.getAsInteger(BookEntry.COLUMN_BOOK_PRICE);
        /*
        if (gender == null || !BookEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Book requires valid gender");
        }
        */
        // If the weight is provided, check that it's greater than or equal to 0 kg
        Integer quantity = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Book requires valid quantity");
        }
        // No need to check the breed, any value is valid (including null).

        // We already know were in the BOOKS case from the UriMatcher result,
        // so we need to continue walking down the diagram and get a database object,
        // and then do the insertion.
        // Lets start by getting a database object.

        // Insert a new book into the books database table with the given ContentValues
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Once we have a database object, we can call the insert() method on it,
        // passing in the book table name and the ContentValues object.
        // The return value is the ID of the new row that was just created,
        // in the form of a long data type (which can store numbers larger than the int data type).
        // Insert the new book with the given values
        long id = database.insert(BookEntry.TABLE_NAME, null, values);

        // Based on the ID, we can determine if the database operation went smoothly or not.
        // If the ID is equal to -1, then we know the insertion failed. Otherwise,
        // the insertion was successful. Hence, we add this check in the code.
        // If the insertion failed, we log an error message using Log.e() and
        // also return a null URI. That way, if a class tries to insert a book,
        // but receives a null URI, theyll know that something went wrong.
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the data has changed for the book content URI.
        getContext().getContentResolver().notifyChange(uri, null);

        // Once we know the ID of the new row in the table,
        // return the new URI with the ID appended to the end of it.
        // Create specific book URI with ID. i.e. content://com.example.android.books/books/6
        return ContentUris.withAppendedId(uri, id);
    }

    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */
    @Override
    public int update(Uri uri,
                      ContentValues contentValues,
                      String selection,
                      String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                // Example:
                // URI: content://com.example.android.books/books/
                // ContentValues: name is Milo, breed is French bulldog, weight is 20
                // Selection: name=?
                // SelectionArgs: { Toto }
                return updateBook(uri, contentValues, selection, selectionArgs);
            case BOOK_ID:
                // For the BOOK_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                // Example:
                // URI: content://com.example.android.books/books/5
                // ContentValues: name is Milo, breed is French bulldog, weight is 20
                //  ?? What is in selection and selectionArgs ???
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateBook(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update books in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more books).
     * Return the number of rows that were successfully updated.
     */
    private int updateBook(Uri uri,
                           ContentValues values,
                           String selection,
                           String[] selectionArgs) {

        // TODO: Update the selected books in the books database table with the given ContentValues
        // If the {@link BookEntry#COLUMN_BOOK_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(BookEntry.COLUMN_BOOK_PRODUCT_NAME)) {
            String name = values.getAsString(BookEntry.COLUMN_BOOK_PRODUCT_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Book requires a name");
            }
        }

        // if the {@link BookEntry#COLUMN_BOOK_GENDER} key is present,
        // check that the gender value is valid.
        /*
        if (values.containsKey(BookEntry.COLUMN_BOOK_GENDER)) {
            Integer gender = values.getAsInteger(BookEntry.COLUMN_BOOK_GENDER);
            if (gender == null || !BookEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Book requires valid gender");
            }
        }
        */

        // if the {@link BookEntry#COLUMN_BOOK_WEIGHT} key is present,
        // check that the weight value is valid.
        if (values.containsKey(BookEntry.COLUMN_BOOK_QUANTITY)) {
            // Check that the wieght is greater than or equal to 0 kg
            Integer weight = values.getAsInteger(BookEntry.COLUMN_BOOK_QUANTITY);
            if (weight != null && weight < 0)  {
                throw new IllegalArgumentException("Book requires valid weight");
            }
        }
        // No need to check for the breed, nay value is valid (including null)

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // TODO: Return the number of rows that were affected
        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(BookEntry.TABLE_NAME, values, selection, selectionArgs);

        // if 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // return the nubmer rows updated
        return rowsUpdated;
    }


    /**
     * Delete the data at the given selection and selection arguments.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Tracek the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                //Example inputs to delete() method:
                //URI: content://com.example.android.books/books
                //Selection: breed=?
                //SelectionArgs: { Calico }
                // Get writeable database
                // Delete all rows that match the selection and selection args
                // return database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case BOOK_ID:
                // Example inputs to delete() method:
                // URI: content://com.example.android.books/books/5
                // Selection: name=?
                // SelectionArgs: { Milo }
                // Delete a single row given by the ID in the URI
                selection = BookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                // return database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                rowsDeleted = database.delete(BookEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // if 1 or more rows were deleted, then notify all listeners that the data at the
        // given URI has changed
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of rows deleted
        return rowsDeleted;
    }

    /**
     * Returns the MIME type of data for the content URI.
     * UriMatcher BOOKS case ? Return MIME type BookEntry.CONTENT_LIST_TYPE
     * UriMatcher BOOK_ID case ? Return MIME type BookEntry.CONTENT_ITEM_TYPE
     */
    @Override
    public String getType(Uri uri) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case BOOKS:
                return BookEntry.CONTENT_LIST_TYPE;
            case BOOK_ID:
                return BookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}
