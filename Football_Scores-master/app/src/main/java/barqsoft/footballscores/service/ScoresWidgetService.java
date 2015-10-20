package barqsoft.footballscores.service;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.TypedValue;
import android.widget.CursorAdapter;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.FootballScoresWidget;
import barqsoft.footballscores.MainActivity;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilies;

/**
 * Created by andrei on 15.10.2015.
 */
public class ScoresWidgetService extends RemoteViewsService {
    private static String LOG_TAG="ScoresWidgetService";
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        Log.d(LOG_TAG, "onGetViewFactory");
        return new WidgetViewsFactory(
                this.getApplicationContext(), intent
        );
    }

    public class WidgetViewsFactory implements RemoteViewsFactory {

        private Context mContext;
        private int mAppWidgetId;

        private Cursor mCursor;




        public WidgetViewsFactory(Context context, Intent intent) {
            mContext = context;
            mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }


        @Override
        public void onCreate() {
        }

        private Cursor queryDatabase() {
            Cursor c;
            Date now = new Date(System.currentTimeMillis());
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String[] selectionArgs = new String[1];
            selectionArgs[0]  = format.format(now);

            /**
             * To explain clearCallingIdentity, I can only say
             * https://groups.google.com/forum/#!topic/android-developers/yj4xEkZDWhQ
             */
            long token = Binder.clearCallingIdentity();

            c = getContentResolver().query(
                    DatabaseContract.scores_table.buildScoreWithDateGt(),
                    null,
                    null,
                    selectionArgs,
                    DatabaseContract.scores_table.DATE_COL + " ASC"
            );
            Binder.restoreCallingIdentity(token);
            return c;

        }
        @Override
        public void onDataSetChanged() {
            if (mCursor != null)
                mCursor.close();
            mCursor = queryDatabase();
            if (mCursor.getCount() != 0)
                mCursor.moveToFirst();
        }

        @Override
        public void onDestroy() {
           if (mCursor != null)
                mCursor.close();
        }

        @Override
        public int getCount() {
            if (mCursor != null)
                return mCursor.getCount();
            return 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            RemoteViews views;
            final int homeNameIdx = mCursor.getColumnIndex(DatabaseContract.scores_table.HOME_COL);
            final int awayNameIdx = mCursor.getColumnIndex(DatabaseContract.scores_table.AWAY_COL);
            final int dateIdx     = mCursor.getColumnIndex(DatabaseContract.scores_table.DATE_COL);
            final int timeIdx     = mCursor.getColumnIndex(DatabaseContract.scores_table.TIME_COL);
            final int homeScoreIdx= mCursor.getColumnIndex(DatabaseContract.scores_table.HOME_GOALS_COL);
            final int awayScoreIdx= mCursor.getColumnIndex(DatabaseContract.scores_table.AWAY_GOALS_COL);
            final int idIdx       = mCursor.getColumnIndex(DatabaseContract.scores_table.MATCH_ID);

            views = new RemoteViews(mContext.getPackageName(), R.layout.scores_list_item);
            mCursor.moveToPosition(position);
            views.setTextViewText(R.id.home_name, mCursor.getString(homeNameIdx));
            views.setTextColor(R.id.home_name, Color.BLACK);
            //views.setTextViewTextSize(R.id.home_name, TypedValue.COMPLEX_UNIT_SP, 15);
            views.setTextViewText(R.id.away_name, mCursor.getString(awayNameIdx));
            views.setTextColor(R.id.away_name, Color.BLACK);
            //views.setTextViewTextSize(R.id.away_name, TypedValue.COMPLEX_UNIT_SP, 15);

            views.setTextViewText(R.id.score_textview,
                    Utilies.getScores(Integer.valueOf(mCursor.getString(homeScoreIdx)),
                            Integer.valueOf(mCursor.getString(awayScoreIdx))));
            views.setTextColor(R.id.score_textview, Color.BLACK);

            Date today = new Date(System.currentTimeMillis());
            Date tomorrow = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            Date after = new Date(System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000);

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String matchDate = mCursor.getString(dateIdx);
            String timeDescription;

            if (matchDate.equals(format.format(today))) {
                views.setTextViewText(R.id.data_textview, mCursor.getString(timeIdx));
                timeDescription = " at "+ mCursor.getString(timeIdx);
            } else {

                views.setTextViewText(R.id.data_textview, matchDate);
                timeDescription = " on " + matchDate + " at " +mCursor.getString(timeIdx);
            }

            views.setTextColor(R.id.data_textview, Color.BLACK);

            Intent fillInIntent = new Intent();

            fillInIntent.putExtra(MainActivity.EXTRA_MATCH_ID, mCursor.getInt(idIdx));

            if (format.format(tomorrow).equals(matchDate)) {
                fillInIntent.putExtra(MainActivity.EXTRA_PAGER_ID, 1);
            } else if (format.format(after).equals(matchDate)) {
                fillInIntent.putExtra(MainActivity.EXTRA_PAGER_ID, 2);
            } else
                fillInIntent.putExtra(MainActivity.EXTRA_PAGER_ID, 0);


            views.setOnClickFillInIntent(R.id.list_item_id, fillInIntent);


            String contentDescription = mCursor.getString(homeNameIdx) +  " versus "
                    + mCursor.getString(awayNameIdx) + timeDescription;

            int home_score = mCursor.getInt(homeScoreIdx);
            int away_score = mCursor.getInt(awayScoreIdx);
            if (home_score >= 0 && away_score >= 0)
                contentDescription = contentDescription + ". Score "
                        + home_score + " to "
                        + away_score;

            views.setContentDescription(R.id.list_item_id, contentDescription);
            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }
    }
}
