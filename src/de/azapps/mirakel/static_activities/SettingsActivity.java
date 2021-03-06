/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 * 
 * Copyright (c) 2013 Anatolij Zelenin, Georg Semmler.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package de.azapps.mirakel.static_activities;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Toast;
import de.azapps.mirakel.helper.ExportImport;
import de.azapps.mirakel.helper.Helpers;
import de.azapps.mirakel.helper.PreferencesHelper;
import de.azapps.mirakelandroid.R;

public class SettingsActivity extends PreferenceActivity {

	public static final int FILE_ASTRID = 0, FILE_IMPORT_DB = 1;
	@SuppressWarnings("unused")
	private static final String TAG = "SettingsActivity";

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("DarkTheme", false))
			setTheme(R.style.AppBaseThemeDARK);
		super.onCreate(savedInstanceState);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			// Display the fragment as the main content.
			((FrameLayout) findViewById(android.R.id.content)).removeAllViews();
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, new SettingsFragment())
					.commit();
			getActionBar().setDisplayHomeAsUpEnabled(true);
		} else {
			addPreferencesFromResource(R.xml.preferences);
			new PreferencesHelper(this).setFunctionsApp();
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		final Context that = this;

		switch (requestCode) {
		case FILE_ASTRID:
			if (resultCode != RESULT_OK)
				return;
			final String path_astrid = Helpers.getPathFromUri(data.getData(),
					this);

			// Do the import in a background-task
			new AsyncTask<String, Void, Boolean>() {
				ProgressDialog dialog;

				@Override
				protected Boolean doInBackground(String... params) {
					return ExportImport.importAstrid(that, path_astrid);
				}

				@Override
				protected void onPostExecute(Boolean success) {
					dialog.dismiss();
					if (!success) {
						Toast.makeText(that, R.string.astrid_unsuccess,
								Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(that, R.string.astrid_success,
								Toast.LENGTH_SHORT).show();
						android.os.Process.killProcess(android.os.Process
								.myPid()); // ugly
											// but
											// simple
					}
				}

				@Override
				protected void onPreExecute() {
					dialog = ProgressDialog.show(that,
							that.getString(R.string.astrid_importing),
							that.getString(R.string.astrid_wait), true);
				}
			}.execute("");

			break;
		case FILE_IMPORT_DB:
			if (resultCode != RESULT_OK)
				return;
			final String path_db = Helpers.getPathFromUri(data.getData(), this);
			// Check if this is an database file
			if (!path_db.endsWith(".db")) {
				Toast.makeText(that, R.string.import_wrong_type,
						Toast.LENGTH_LONG).show();
				return;
			}
			new AlertDialog.Builder(this)
					.setTitle(R.string.import_sure)
					.setMessage(
							this.getString(R.string.import_sure_summary,
									path_db))
					.setNegativeButton(android.R.string.cancel,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {

								}
							})
					.setPositiveButton(android.R.string.yes,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									ExportImport.importDB(that, new File(
											path_db));
								}
							}).create().show();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
