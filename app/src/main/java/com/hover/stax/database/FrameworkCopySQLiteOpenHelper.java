package com.hover.stax.database;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import java.io.File;

class FrameworkCopySQLiteOpenHelper implements SupportSQLiteOpenHelper {

	private final Context mContext;
	private final String mName;
	private final SupportSQLiteOpenHelper.Callback mCallback;
	private final boolean mUseNoBackupDirectory;
	private final Object mLock;

	// Delegate is created lazily
	private OpenHelper mDelegate;
	private boolean mWriteAheadLoggingEnabled;

	FrameworkCopySQLiteOpenHelper(
		Context context,
		String name,
		Callback callback) {
		this(context, name, callback, false);
	}

	FrameworkCopySQLiteOpenHelper(
		Context context,
		String name,
		Callback callback,
		boolean useNoBackupDirectory) {
		mContext = context;
		mName = name;
		mCallback = callback;
		mUseNoBackupDirectory = useNoBackupDirectory;
		mLock = new Object();
	}

	private OpenHelper getDelegate() {
		// getDelegate() is lazy because we don't want to File I/O until the call to
		// getReadableDatabase() or getWritableDatabase(). This is better because the call to
		// a getReadableDatabase() or a getWritableDatabase() happens on a background thread unless
		// queries are allowed on the main thread.

		// We defer computing the path the database from the constructor to getDelegate()
		// because context.getNoBackupFilesDir() does File I/O :(
		synchronized (mLock) {
			if (mDelegate == null) {
				final FrameworkCopySQLiteDatabase[] dbRef = new FrameworkCopySQLiteDatabase[1];
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
					    && mName != null
					    && mUseNoBackupDirectory) {
					File file = new File(mContext.getNoBackupFilesDir(), mName);
					mDelegate = new OpenHelper(mContext, file.getAbsolutePath(), dbRef, mCallback);
				} else {
					mDelegate = new OpenHelper(mContext, mName, dbRef, mCallback);
				}
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					mDelegate.setWriteAheadLoggingEnabled(mWriteAheadLoggingEnabled);
				}
			}
			return mDelegate;
		}
	}

	@Override
	public String getDatabaseName() {
		return mName;
	}

	@Override
	@androidx.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	public void setWriteAheadLoggingEnabled(boolean enabled) {
		synchronized (mLock) {
			if (mDelegate != null) {
				mDelegate.setWriteAheadLoggingEnabled(enabled);
			}
			mWriteAheadLoggingEnabled = enabled;
		}
	}

	@Override
	public SupportSQLiteDatabase getWritableDatabase() {
		return getDelegate().getWritableSupportDatabase();
	}

	@Override
	public SupportSQLiteDatabase getReadableDatabase() {
		return getDelegate().getReadableSupportDatabase();
	}

	@Override
	public void close() {
		getDelegate().close();
	}

	static class OpenHelper extends SQLiteOpenHelper {
		/**
		 * This is used as an Object reference so that we can access the wrapped database inside
		 * the constructor. SQLiteOpenHelper requires the error handler to be passed in the
		 * constructor.
		 */
		final FrameworkCopySQLiteDatabase[] mDbRef;
		final Callback mCallback;
		// see b/78359448
		private boolean mMigrated;

		OpenHelper(Context context, String name, final FrameworkCopySQLiteDatabase[] dbRef,
		           final Callback callback) {
			super(context, name, null, callback.version,
				new DatabaseErrorHandler() {
					@Override
					public void onCorruption(SQLiteDatabase dbObj) {
						callback.onCorruption(getWrappedDb(dbRef, dbObj));
					}
				});
			mCallback = callback;
			mDbRef = dbRef;
		}

		synchronized SupportSQLiteDatabase getWritableSupportDatabase() {
			mMigrated = false;
			SQLiteDatabase db = super.getWritableDatabase();
			if (mMigrated) {
				// there might be a connection w/ stale structure, we should re-open.
				close();
				return getWritableSupportDatabase();
			}
			return getWrappedDb(db);
		}

		synchronized SupportSQLiteDatabase getReadableSupportDatabase() {
			mMigrated = false;
			SQLiteDatabase db = super.getReadableDatabase();
			if (mMigrated) {
				// there might be a connection w/ stale structure, we should re-open.
				close();
				return getReadableSupportDatabase();
			}
			return getWrappedDb(db);
		}

		FrameworkCopySQLiteDatabase getWrappedDb(SQLiteDatabase sqLiteDatabase) {
			return getWrappedDb(mDbRef, sqLiteDatabase);
		}

		@Override
		public void onCreate(SQLiteDatabase sqLiteDatabase) {
			mCallback.onCreate(getWrappedDb(sqLiteDatabase));
		}

		@Override
		public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
			mMigrated = true;
			mCallback.onUpgrade(getWrappedDb(sqLiteDatabase), oldVersion, newVersion);
		}

		@Override
		public void onConfigure(SQLiteDatabase db) {
			mCallback.onConfigure(getWrappedDb(db));
		}

		@Override
		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			mMigrated = true;
			mCallback.onDowngrade(getWrappedDb(db), oldVersion, newVersion);
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			if (!mMigrated) {
				// if we've migrated, we'll re-open the db so we should not call the callback.
				mCallback.onOpen(getWrappedDb(db));
			}
		}

		@Override
		public synchronized void close() {
			super.close();
			mDbRef[0] = null;
		}

		static FrameworkCopySQLiteDatabase getWrappedDb(FrameworkCopySQLiteDatabase[] refHolder,
		                                            SQLiteDatabase sqLiteDatabase) {
			FrameworkCopySQLiteDatabase dbRef = refHolder[0];
			if (dbRef == null || !dbRef.isDelegate(sqLiteDatabase)) {
				refHolder[0] = new FrameworkCopySQLiteDatabase(sqLiteDatabase);
			}
			return refHolder[0];
		}
	}
}
