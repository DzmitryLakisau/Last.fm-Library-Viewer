package by.d1makrat.library_fm.asynctask;

import android.os.AsyncTask;

import java.net.URL;

import by.d1makrat.library_fm.APIException;
import by.d1makrat.library_fm.https.HttpsClient;
import by.d1makrat.library_fm.https.RequestMethod;
import by.d1makrat.library_fm.json.JsonParser;
import by.d1makrat.library_fm.json.UserParser;
import by.d1makrat.library_fm.model.User;
import by.d1makrat.library_fm.utils.UrlConstructor;

import static by.d1makrat.library_fm.Constants.API_NO_ERROR;

public class GetUserInfoAsyncTask extends AsyncTask<Void, Void, User> {

    private GetUserInfoCallback mAsynctaskCallback;
    private Exception mException = null;

    public GetUserInfoAsyncTask(GetUserInfoCallback pGetUserInfoCallback) {
        mAsynctaskCallback = pGetUserInfoCallback;
    }

    @Override
    protected User doInBackground(Void... params) {

        User user = null;

        try {
            UrlConstructor urlConstructor = new UrlConstructor();
            URL apiRequestUrl = urlConstructor.constructGetUserInfoApiRequestUrl(null);

            HttpsClient httpsClient = new HttpsClient();
            String response = httpsClient.request(apiRequestUrl, RequestMethod.GET);

            JsonParser jsonParser = new JsonParser();

            String errorOrNot = jsonParser.checkForApiErrors(response);
            if (!errorOrNot.equals(API_NO_ERROR)) {
                mException = new APIException(errorOrNot);
            }
            else {
                UserParser userParser = new UserParser(response);
                user = userParser.parse();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mException = e;
        }

        return user;
    }

    @Override
    protected void onPostExecute(User result) {
        if (mException == null) {
            mAsynctaskCallback.onUserInfoReceived(result);
        }
        else
            mAsynctaskCallback.onException(mException);
    }
}