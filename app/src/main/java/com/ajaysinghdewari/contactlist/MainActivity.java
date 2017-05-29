package com.ajaysinghdewari.contactlist;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import static android.Manifest.permission.READ_CONTACTS;

public class MainActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>{

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private ContactsAdapter mAdapter; // The main query adapter
    private ImageLoader mImageLoader; // Handles loading the contact image in a background thread

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAdapter = new ContactsAdapter(this);

        populateContacts();
    }

    private void populateContacts() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateContacts();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        // If this is the loader for finding contacts in the Contacts Provider
        // (the only one supported)
        if (id == ContactsQuery.QUERY_ID) {
            Uri contentUri;

            // There are two types of searches, one which displays all contacts and
            // one which filters contacts by a search query. If mSearchTerm is set
            // then a search query has been entered and the latter should be used.

            if (mSearchTerm == null) {
                // Since there's no search string, use the content URI that searches the entire
                // Contacts table
                contentUri = ContactsQuery.CONTENT_URI;
            } else {
                // Since there's a search string, use the special content Uri that searches the
                // Contacts table. The URI consists of a base Uri and the search string.
                contentUri =
                        Uri.withAppendedPath(ContactsQuery.FILTER_URI, Uri.encode(mSearchTerm));
            }

            // Returns a new CursorLoader for querying the Contacts table. No arguments are used
            // for the selection clause. The search string is either encoded onto the content URI,
            // or no contacts search string is used. The other search criteria are constants. See
            // the ContactsQuery interface.
            return new CursorLoader(getActivity(),
                    contentUri,
                    ContactsQuery.PROJECTION,
                    ContactsQuery.SELECTION,
                    null,
                    ContactsQuery.SORT_ORDER);
        }

        Log.e(TAG, "onCreateLoader - incorrect ID provided (" + id + ")");
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // This swaps the new cursor into the adapter.
        if (loader.getId() == ContactsQuery.QUERY_ID) {
            mAdapter.swapCursor(data);

           /* // If this is a two-pane layout and there is a search query then
            // there is some additional work to do around default selected
            // search item.
            if (mIsTwoPaneLayout && !TextUtils.isEmpty(mSearchTerm) && mSearchQueryChanged) {
                // Selects the first item in results, unless this fragment has
                // been restored from a saved state (like orientation change)
                // in which case it selects the previously selected search item.
                if (data != null && data.moveToPosition(mPreviouslySelectedSearchItem)) {
                    // Creates the content Uri for the previously selected contact by appending the
                    // contact's ID to the Contacts table content Uri
                    final Uri uri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_URI, String.valueOf(data.getLong(ContactsQuery.ID)));
                    mOnContactSelectedListener.onContactSelected(uri);
                    getListView().setItemChecked(mPreviouslySelectedSearchItem, true);
                } else {
                    // No results, clear selection.
                    onSelectionCleared();
                }
                // Only restore from saved state one time. Next time fall back
                // to selecting first item. If the fragment state is saved again
                // then the currently selected item will once again be saved.
                mPreviouslySelectedSearchItem = 0;
                mSearchQueryChanged = false;
            }*/
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() == ContactsQuery.QUERY_ID) {
            // When the loader is being reset, clear the cursor from the adapter. This allows the
            // cursor resources to be freed.
            mAdapter.swapCursor(null);
        }
    }

    public interface ContactsQuery {

        // An identifier for the loader
        final static int QUERY_ID = 1;

        // A content URI for the Contacts table
        final static Uri CONTENT_URI = ContactsContract.Contacts.CONTENT_URI;

        // The search/filter query Uri
        final static Uri FILTER_URI = ContactsContract.Contacts.CONTENT_FILTER_URI;

        // The selection clause for the CursorLoader query. The search criteria defined here
        // restrict results to contacts that have a display name and are linked to visible groups.
        // Notice that the search on the string provided by the user is implemented by appending
        // the search string to CONTENT_FILTER_URI.
        @SuppressLint("InlinedApi")
        final static String SELECTION =
                (Utils.hasHoneycomb() ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME) +
                        "<>''" + " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        // The desired sort order for the returned Cursor. In Android 3.0 and later, the primary
        // sort key allows for localization. In earlier versions. use the display name as the sort
        // key.
        @SuppressLint("InlinedApi")
        final static String SORT_ORDER =
                Utils.hasHoneycomb() ? ContactsContract.Contacts.SORT_KEY_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;

        // The projection for the CursorLoader query. This is a list of columns that the Contacts
        // Provider should return in the Cursor.
        @SuppressLint("InlinedApi")
        final static String[] PROJECTION = {

                // The contact's row id
                ContactsContract.Contacts._ID,

                // A pointer to the contact that is guaranteed to be more permanent than _ID. Given
                // a contact's current _ID value and LOOKUP_KEY, the Contacts Provider can generate
                // a "permanent" contact URI.
                ContactsContract.Contacts.LOOKUP_KEY,

                // In platform version 3.0 and later, the Contacts table contains
                // DISPLAY_NAME_PRIMARY, which either contains the contact's displayable name or
                // some other useful identifier such as an email address. This column isn't
                // available in earlier versions of Android, so you must use Contacts.DISPLAY_NAME
                // instead.
                Utils.hasHoneycomb() ? ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME,

                // In Android 3.0 and later, the thumbnail image is pointed to by
                // PHOTO_THUMBNAIL_URI. In earlier versions, there is no direct pointer; instead,
                // you generate the pointer from the contact's ID value and constants defined in
                // android.provider.ContactsContract.Contacts.
                Utils.hasHoneycomb() ? ContactsContract.Contacts.PHOTO_THUMBNAIL_URI : ContactsContract.Contacts._ID,

                // The sort order column for the returned Cursor, used by the AlphabetIndexer
                SORT_ORDER,
        };

        // The query column numbers which map to each value in the projection
        final static int ID = 0;
        final static int LOOKUP_KEY = 1;
        final static int DISPLAY_NAME = 2;
        final static int PHOTO_THUMBNAIL_DATA = 3;
        final static int SORT_KEY = 4;
    }
}
