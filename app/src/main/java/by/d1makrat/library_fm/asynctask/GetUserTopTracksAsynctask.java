package by.d1makrat.library_fm.asynctask;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import by.d1makrat.library_fm.APIException;
import by.d1makrat.library_fm.AppContext;
import by.d1makrat.library_fm.AsynctaskCallback;
import by.d1makrat.library_fm.HttpsClient;
import by.d1makrat.library_fm.UrlConstructor;
import by.d1makrat.library_fm.database.DatabaseWorker;
import by.d1makrat.library_fm.json.JsonParser;
import by.d1makrat.library_fm.model.RankedItem;

public class GetUserTopTracksAsynctask extends GetRankedItemsAsynctask {

    private Exception mException;
    private AsynctaskCallback asynctaskCallback;

    public GetUserTopTracksAsynctask(AsynctaskCallback pAsynctaskCallback) {
        asynctaskCallback = pAsynctaskCallback;
    }

    @Override
    protected List<RankedItem> doInBackground(String... params) {
        List<RankedItem> topTracks = new ArrayList<>();
        DatabaseWorker databaseWorker = new DatabaseWorker(AppContext.getInstance().getApplicationContext());

        try {
            databaseWorker.openDatabase();

            if (HttpsClient.isNetworkAvailable()) {
                UrlConstructor urlConstructor = new UrlConstructor();
                URL apiRequestUrl = urlConstructor.constructGetUserTopTracksApiRequestUrl(params[0], params[1]);

                HttpsClient httpsClient = new HttpsClient();
                String response = httpsClient.request(apiRequestUrl, "GET");

                JsonParser jsonParser = new JsonParser();

                String errorOrNot = jsonParser.checkForApiErrors(response);
                if (!errorOrNot.equals("No error"))
                    mException = new APIException(errorOrNot);
                else
                    topTracks = jsonParser.parseUserTopTracks(response);

                if (params[1].equals("1"))
                    databaseWorker.deleteTopTracks(params[0]);

                databaseWorker.bulkInsertTopTracks(topTracks, params[0]);
            }
            else {
                topTracks = databaseWorker.getTopTracks(params[0], params[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mException = e;
        } finally {
            databaseWorker.closeDatabase();
        }

        return topTracks;
    }

    @Override
    protected void onPostExecute(List<RankedItem> result) {
        if (mException != null)
            asynctaskCallback.onException(mException);
        else {
            asynctaskCallback.onLoadingRankedItemsSuccessful(result);
        }
    }
}