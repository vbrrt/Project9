package com.example.android.books;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.example.android.books.data.BookContract.BookEntry;
import com.example.android.books.data.BookDbHelper;

/**
 * Displays list of books that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity  implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifier for the pat data loader */
    private static final int BOOK_LOADER = 0;

    // Adapter for the ListView
    BookCursorAdapter mCursorAdapter;

    /** Database helper that will provide us access to the database */
    private BookDbHelper mDbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                // Where startActivity is defined
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the book data
        ListView bookListView = (ListView) findViewById(R.id.list);

        // ?? How to find out the database is empty ???
        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        bookListView.setEmptyView(emptyView);

        // Setup an Adapter to create a list item for each row of book data in the Cursor
        // There is no book data yet (until the loader finishes) so pass in null for the Cursor
        mCursorAdapter = new BookCursorAdapter(this, null);
        bookListView.setAdapter(mCursorAdapter);

        // Setup item click listener
        bookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Create new intent to go to {@link EditorActivity}
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific book that was clicked on,
                // by appending the "id" (passed as input to this methdo) onto the
                // {@link BookEntry#CONTENT_URI}.
                // For example, the URI would be "content://com.example.android.books/books/2"
                // For example, the URI would be "content://com.example.android.inventory/books/2"
                // if the book with ID 2 was clicked on.
                Uri currentBookUri = ContentUris.withAppendedId(BookEntry.CONTENT_URI, id);

                // Set the UR on the data fiedl of the intent
                intent.setData(currentBookUri);

                // lauch the {@link EditorActivity} to display the data for the current book.
                startActivity(intent);
            }
        });

        // Kick off the loader method
        getLoaderManager().initLoader(BOOK_LOADER, null, this);
    }


    /**
     * Helper method to insert hardcoded book data into the database. For debugging purposes only.
     */
    private void insertBook() {
        // Gets the database in write mode
        // SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Create a ContentValues object where column names are the keys,
        // and Toto's book attributes are the values.
        ContentValues values = new ContentValues();
        values.put(BookEntry.COLUMN_BOOK_PRODUCT_NAME, "Algorithm");
        values.put(BookEntry.COLUMN_BOOK_PRICE, "10");
        values.put(BookEntry.COLUMN_BOOK_QUANTITY, "5");
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_NAME, "MIT");
        values.put(BookEntry.COLUMN_BOOK_SUPPLIER_PHONE_NUMBER, "408-498-8675");

        // Insert a new row for Toto in the database, returning the ID of that new row.
        // The first argument for db.insert() is the books table name.
        // The second argument provides the name of a column in which the framework
        // can insert NULL in the event that the ContentValues is empty (if
        // this is set to "null", then the framework will not insert a row when
        // there are no values).
        // The third argument is the ContentValues object containing the info for Toto.
        /** long newRowId = db.insert(BookEntry.TABLE_NAME, null, values);
         Log.v("CatalogActivity: ", "Insert new row id " + newRowId);
         */

        // Insert a new row for Toto into the provider using the ContentResolver.
        // Use the {@link BookEntry#CONTENT_URI} to indicate that we want to insert
        // into the books database table.
        // Receive the new content URI that will allow us to access Toto's data in the future.
        Uri newUri = getContentResolver().insert(BookEntry.CONTENT_URI, values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {

            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertBook();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllBooks();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Helper method to delete all books in the database.
     */
    private void deleteAllBooks() {
        int rowsDeleted = getContentResolver().delete(BookEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from book database");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Define a projection that specifies the columns from the table we care about.
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_PRODUCT_NAME,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_BOOK_QUANTITY
        };

        // This loader will execute the ContentProvider's query method
        // on a background thread.
        return new CursorLoader(this,           // Parent activity context
                BookEntry.CONTENT_URI,                  // Provider content URI to query
                projection,                             // Columns to incldue in the resulting Cursor
                null,                          // No selection clause
                null,                       // No selection arguments
                null);                         // Default sort order
    }

    @Override
    public  void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update {@link BookCursorAdapter} with this new cursor
        // containing updated book data
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted.
        mCursorAdapter.swapCursor(null);
    }
}
