package barqsoft.footballscores;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;

import barqsoft.footballscores.service.ScoresWidgetService;
import barqsoft.footballscores.service.myFetchService;

/**
 * Implementation of App Widget functionality.
 */
public class FootballScoresWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Intent intent = new Intent(context, ScoresWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.football_scores_widget);

        views.setRemoteAdapter(R.id.widget_list_view, intent);
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view);

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent pi = PendingIntent.getActivity(context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_list_view, pi);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);



        /**
         *  trigger database update through the myFetchService
         *  this will subsequently trigger a call to notifyAppWidgetViewDataChanged
         */
        Intent queryIntent = new Intent(context, myFetchService.class);
        queryIntent.setAction(myFetchService.ACTION_UPDATE_MATCHES);
        context.startService(queryIntent);
    }
}

