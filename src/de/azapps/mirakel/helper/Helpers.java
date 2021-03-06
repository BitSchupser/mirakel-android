package de.azapps.mirakel.helper;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import org.joda.time.LocalDate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.azapps.mirakel.Mirakel;
import de.azapps.mirakel.Mirakel.NoSuchListException;
import de.azapps.mirakel.main_activity.MainActivity;
import de.azapps.mirakel.model.list.ListMirakel;
import de.azapps.mirakel.model.task.Task;
import de.azapps.mirakelandroid.R;

public class Helpers {
	private static String TAG = "Helpers";
	private static final short TASK = 0;
	private static final short LIST = 1;
	public static String UNDO = "OLD";

	/**
	 * Wrapper-Class
	 * 
	 * @author az
	 * 
	 */
	public interface ExecInterface {
		public void exec();
	}

	public static Task getTaskFromIntent(Intent intent) {
		Task task = null;
		long taskId = intent.getLongExtra(MainActivity.EXTRA_ID, 0);
		if (taskId == 0) {
			// ugly fix for show Task from Widget
			taskId = (long) intent.getIntExtra(MainActivity.EXTRA_ID, 0);
		}
		if (taskId != 0) {
			task = Task.get(taskId);
		}
		return task;
	}

	/**
	 * Share a Task as text with other apps
	 * 
	 * @param ctx
	 * @param t
	 */
	public static void share(Context ctx, Task t) {
		String subject = getTaskName(ctx, t);
		share(ctx, subject, t.getContent());
	}

	/**
	 * Share a list of Tasks from a List with other apps
	 * 
	 * @param ctx
	 * @param l
	 */
	public static void share(Context ctx, ListMirakel l) {
		String subject = ctx.getString(R.string.share_list_title, l.getName(),
				l.countTasks());
		String body = "";
		for (Task t : l.tasks()) {
			if (t.isDone()) {
				// body += "* ";
				continue;
			} else {
				body += "* ";
			}
			body += getTaskName(ctx, t) + "\n";
		}
		share(ctx, subject, body);
	}

	/**
	 * Helper for the share-functions
	 * 
	 * @param ctx
	 * @param t
	 * @return
	 */
	private static String getTaskName(Context ctx, Task t) {
		String subject;
		if (t.getDue() == null)
			subject = ctx.getString(R.string.share_task_title, t.getName());
		else
			subject = ctx.getString(R.string.share_task_title_with_date,
					t.getName(),
					formatDate(t.getDue(), ctx.getString(R.string.dateFormat)));
		return subject;
	}

	/**
	 * Share something
	 * 
	 * @param context
	 * @param subject
	 * @param shareBody
	 */
	private static void share(Context context, String subject, String shareBody) {
		shareBody += "\n\n" + context.getString(R.string.share_footer);
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);

		Intent ci = Intent.createChooser(sharingIntent, context.getResources()
				.getString(R.string.share_using));
		ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(ci);
	}

	/**
	 * Format a Date for showing it in the app
	 * 
	 * @param date
	 *            Date
	 * @param format
	 *            Format–String (like dd.MM.YY)
	 * @return The formatted Date as String
	 */
	public static String formatDate(Calendar date, String format) {
		if (date == null)
			return "";
		else {
			return new SimpleDateFormat(format, Locale.getDefault())
					.format(date.getTime());
		}
	}

	/**
	 * makes Placeholders for the SQL IN ()-Syntax
	 * 
	 * @param len
	 * @return
	 */
	public static String makePlaceholders(int len) {
		if (len < 1) {
			// It will lead to an invalid query anyway ..
			throw new RuntimeException("No placeholders");
		} else {
			StringBuilder sb = new StringBuilder(len * 2 - 1);
			sb.append("?");
			for (int i = 1; i < len; i++) {
				sb.append(",?");
			}
			return sb.toString();
		}
	}

	/**
	 * Returns the ID of the Color–Resource for a Due–Date
	 * 
	 * @param origDue
	 *            The Due–Date
	 * @param isDone
	 *            Is the Task done?
	 * @return ID of the Color–Resource
	 */
	public static int getTaskDueColor(Calendar origDue, boolean isDone) {
		if (origDue == null)
			return R.color.Grey;
		LocalDate today = new LocalDate();
		LocalDate nextWeek = new LocalDate().plusDays(7);
		LocalDate due = new LocalDate(origDue);
		int cmpr = today.compareTo(due);
		int color;
		if (isDone) {
			color = R.color.Grey;
		} else if (cmpr > 0) {
			color = R.color.Red;
		} else if (cmpr == 0) {
			color = R.color.Orange;
		} else if (nextWeek.compareTo(due) >= 0) {
			color = R.color.Yellow;
		} else {
			color = R.color.Green;
		}
		return color;
	}

	public static void contact(Context context) {

		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL,
				new String[] { context.getString(R.string.contact_email) });
		i.putExtra(Intent.EXTRA_SUBJECT,
				context.getString(R.string.contact_subject));
		String mirakelVersion = "unknown";
		try {
			mirakelVersion = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "could not get version name from manifest!");
			e.printStackTrace();
		}
		i.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.contact_text,
				mirakelVersion, android.os.Build.VERSION.SDK_INT,
				android.os.Build.DEVICE));
		try {
			Intent ci = Intent.createChooser(i,
					context.getString(R.string.contact_chooser));
			ci.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(ci);
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(context,
					context.getString(R.string.contact_no_client),
					Toast.LENGTH_SHORT).show();
		}
	}

	public static void showFileChooser(int code, String title, Activity activity) {

		Intent fileDialogIntent = new Intent(Intent.ACTION_GET_CONTENT);
		fileDialogIntent.setType("*/*");
		fileDialogIntent.addCategory(Intent.CATEGORY_OPENABLE);

		try {
			activity.startActivityForResult(
					Intent.createChooser(fileDialogIntent, title), code);
		} catch (android.content.ActivityNotFoundException ex) {
			// Potentially direct the user to the Market with a
			// Dialog
			Toast.makeText(activity, R.string.no_filemanager,
					Toast.LENGTH_SHORT).show();
		}
	}

	static public String getPathFromUri(Uri uri, Context ctx) {
		try {
			return FileUtils.getPath(ctx, uri);
		} catch (URISyntaxException e) {
			Toast.makeText(ctx, "Something terrible happened…",
					Toast.LENGTH_LONG).show();
			return "";
		}
	}

	public static void updateLog(ListMirakel listMirakel, Context ctx) {
		updateLog(LIST, listMirakel.toJson(), ctx);

	}

	private static void updateLog(short type, String json, Context ctx) {
		if (ctx == null) {
			Log.e(TAG, "context is null");
			return;
		}
		Log.d(TAG, json);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = settings.edit();
		for (int i = settings.getInt("UndoNumber", 10); i > 0; i--) {
			String old = settings.getString(UNDO + (i - 1), "");
			editor.putString(UNDO + i, old);
		}
		editor.putString(UNDO + 0, type + json);
		editor.commit();
	}

	public static void updateLog(Task task, Context ctx) {
		updateLog(TASK, task.toJson(), ctx);

	}

	public static void undoLast(Context ctx) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String last = settings.getString(UNDO + 0, "");
		if (last != null && !last.equals("")) {
			short type = Short.parseShort(last.charAt(0) + "");
			if (last.charAt(1) != '{') {
				try {
					Long id = Long.parseLong(last.substring(1));
					switch (type) {
					case TASK:
						Task.get(id).delete(true);
						break;
					case LIST:
						ListMirakel.getList(id.intValue()).destroy(true);
						break;
					default:
						Log.wtf(TAG, "unkown Type");
						break;
					}
				} catch (Exception e) {
					Log.e(TAG, "cannot parse String");
				}

			} else {
				JsonObject json = new JsonParser().parse(last.substring(1))
						.getAsJsonObject();
				switch (type) {
				case TASK:
					try {
						Task t = Task.parse_json(json);
						if (Task.get(t.getId()) != null)
							t.save(false);
						else {
							try {
								Mirakel.getWritableDatabase().insert(
										Task.TABLE, null, t.getContentValues());
							} catch (Exception e) {
								Log.e(TAG, "cannot restore Task");
							}
						}
					} catch (NoSuchListException e) {
						Log.e(TAG, "List not found");
					}
					break;
				case LIST:
					ListMirakel l = ListMirakel.parseJson(json);
					if (ListMirakel.getList(l.getId()) != null)
						l.save(false);
					else {
						try {
							Mirakel.getWritableDatabase().insert(
									ListMirakel.TABLE, null,
									l.getContentValues());
						} catch (Exception e) {
							Log.e(TAG, "cannot restore List");
						}
					}
					break;
				default:
					Log.wtf(TAG, "unkown Type");
					break;
				}
			}
		}
		SharedPreferences.Editor editor = settings.edit();
		for (int i = 0; i < settings.getInt("UndoNumber", 10); i++) {
			String old = settings.getString(UNDO + (i + 1), "");
			editor.putString(UNDO + i, old);
		}
		editor.putString(UNDO + 10, "");
		editor.commit();
	}

	public static void logCreate(Task newTask, Context ctx) {
		updateLog(TASK, newTask.getId() + "", ctx);
	}

	public static void logCreate(ListMirakel newList, Context ctx) {
		updateLog(LIST, newList.getId() + "", ctx);
	}

}
