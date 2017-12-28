package by.d1makrat.library_fm.ui.fragment;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import by.d1makrat.library_fm.AppContext;
import by.d1makrat.library_fm.AsynctaskCallback;
import by.d1makrat.library_fm.HttpsClient;
import by.d1makrat.library_fm.R;
import by.d1makrat.library_fm.model.RankedItem;
import by.d1makrat.library_fm.model.Scrobble;

import static by.d1makrat.library_fm.Constants.*;

public abstract class ScrobblesListFragment extends ListFragment implements AbsListView.OnScrollListener, FilterDialogFragment.DialogListener, AsynctaskCallback {
    //max limit=1000
    protected static String urlForBrowser;
    private String filter_string = null;
    private String list_head_text = null;
    private String empty_list_text = null;
    protected String mFrom = null;
    protected String mTo = null;

    private boolean isLoading = false;
    private boolean isCreated = true;
    protected boolean allIsLoaded = false;
    private boolean wasEmpty = false;

    protected AsyncTask mGetItemsAsynctask;
    protected BaseAdapter mListAdapter;
    private ListView mListView;
    private View list_head;
    protected List<Scrobble> mScrobbles = new ArrayList<>();
    protected List<RankedItem> mRankedItems = new ArrayList<>();
    protected int mPage = 1;
    private View mFooterView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

//        mPage = 1;

        mListAdapter = createAdapter();
    }

//    private BaseAdapter createAdapter(){
//
//        Drawable drawable;
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
//            drawable = getResources().getDrawable(R.drawable.default_albumart);
//        } else
//            drawable = getResources().getDrawable(R.drawable.default_albumart, null);
//
//        return mListAdapter = new ScrobblesListAdapter(getActivity().getLayoutInflater(), drawable, mScrobbles);
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.list_with_head, container, false);

        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.scrobbles));
//        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(null);

        if (!isCreated) {
            if (wasEmpty) {
                //было загружено и пусто
                rootView.findViewById(R.id.empty_list).setVisibility(View.VISIBLE);
                ((TextView) rootView.findViewById(R.id.empty_list_text)).setText(empty_list_text);
            } else {
                //было загружено и данные не пустые
//                rootView.findViewById(R.id.list_head).setVisibility(View.VISIBLE);
                ((TextView) rootView.findViewById(R.id.list_head)).setText(list_head_text);
            }
        } else {
            if (!HttpsClient.isNetworkAvailable()) {
                //создаётся и сеть отсуствует

                //loadItemsFromDatabase
            }
        }

        mListView = rootView.findViewById(android.R.id.list);
        list_head = rootView.findViewById(R.id.list_head);

        mFooterView = getActivity().getLayoutInflater().inflate(R.layout.list_spinner, (ViewGroup) null);
        mListView.addFooterView(mFooterView);
        mListView.setAdapter(mListAdapter);
        mListView.removeFooterView(mFooterView);

//        progressBar = mFooterView.findViewById(R.id.progressbar);

//        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(this);
        registerForContextMenu(mListView);

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (isCreated)
            loadItems();
//            loadItemsFromWeb(mPage, null, null);
    }

    @Override
    public void onScrollStateChanged(AbsListView l, int ScrollState) {
    }

    @Override
    public void onScroll(AbsListView l, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (!allIsLoaded && !isLoading){
            if ((firstVisibleItem + visibleItemCount) == totalItemCount && (totalItemCount > 0)) {
                mPage++;
                loadItems();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_scrobbles, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                    if (!isLoading) {
                        allIsLoaded = false;
                        getView().findViewById(R.id.list_head).setVisibility(View.GONE);
                        KillTaskIfRunning(mGetItemsAsynctask);
                        mScrobbles.clear();
                        mRankedItems.clear();
                        mListAdapter.notifyDataSetChanged();
                        mPage = 1;
                        loadItems();
                    }
                return true;
            case R.id.action_filter:
                if (!isLoading) {
                    FilterDialogFragment dialogFragment = new FilterDialogFragment();
                    Bundle args = new Bundle();
                    args.putString(FILTER_DIALOG_FROM_BUNDLE_KEY, mFrom);
                    args.putString(FILTER_DIALOG_TO_BUNDLE_KEY, mTo);
                    dialogFragment.setArguments(args);
                    dialogFragment.setTargetFragment(this, 0);
                    dialogFragment.show(getFragmentManager(), "DialogFragment");
                }
                return true;
            case R.id.open_in_browser:
                Intent intent;

                if (mFrom != null && mTo != null) {
                    Date date_from = new Date(Long.valueOf(mFrom) * 1000);
                    Date date_to = new Date(Long.valueOf(mTo) * 1000);

                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForBrowser.concat("?from=" + new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date_from) + "&to=" + new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(date_to))));
                }
                else
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlForBrowser));

                startActivity(intent);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(contextMenu, v, menuInfo);
        getActivity().getMenuInflater().inflate(R.menu.context_menu, contextMenu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem pMenuItem) {

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        View listItemPressed = ((AdapterContextMenuInfo) pMenuItem.getMenuInfo()).targetView;
        Fragment fragment;

        switch (pMenuItem.getItemId()) {
            case R.id.scrobbles_of_artist:
                fragment = createFragment(SCROBBLES_OF_ARTIST_TAG, listItemPressed);
                fragmentTransaction.replace(R.id.content_main, fragment, SCROBBLES_OF_ARTIST_TAG);
                break;
            case R.id.scrobbles_of_track:
                fragment = createFragment(SCROBBLES_OF_TRACK_TAG, listItemPressed);
                fragmentTransaction.replace(R.id.content_main, fragment, SCROBBLES_OF_TRACK_TAG);
                break;
            case R.id.scrobbles_of_album:
                fragment = createFragment(SCROBBLES_OF_ALBUM_TAG, listItemPressed);
                fragmentTransaction.replace(R.id.content_main, fragment, SCROBBLES_OF_ALBUM_TAG);
                break;
            default:
                return super.onContextItemSelected(pMenuItem);
        }
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return true;
    }

    private Fragment createFragment(String pTypeOfFragment, View pListItemPressed){

        Fragment fragment = new Fragment();
        Bundle bundle = new Bundle();

        String artist = ((TextView) pListItemPressed.findViewById(R.id.artist)).getText().toString();

        switch (pTypeOfFragment) {
            case SCROBBLES_OF_ARTIST_TAG:
                fragment = new ScrobblesOfArtistFragment();
                break;
            case SCROBBLES_OF_TRACK_TAG:
                String track = ((TextView) pListItemPressed.findViewById(R.id.track)).getText().toString();
                fragment = new ScrobblesOfTrackFragment();
                bundle.putString("track", track);
                break;
            case SCROBBLES_OF_ALBUM_TAG:
                String album = ((TextView) pListItemPressed.findViewById(R.id.album)).getText().toString();
                fragment = new ScrobblesOfAlbumFragment();
                bundle.putString("album", album);
                break;
        }
        bundle.putString("artist", artist);
        fragment.setArguments(bundle);
        return fragment;
    }


    private void KillTaskIfRunning(AsyncTask task) {
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            mPage--;
            task.cancel(true);
        }
    }

    public void loadItems() {
//
//            if (!allIsLoaded) {
//            mPage++;
            isLoading = true;
            mListView.addFooterView(mFooterView);

        loadItemsFromWeb();

    }

    protected abstract BaseAdapter createAdapter();

    protected void loadItemsFromWeb(){}

    @Override
    public void onFinishEditDialog(String pFrom, String pTo) {
        getView().findViewById(R.id.list_head).setVisibility(View.GONE);
        allIsLoaded = false;
        mFrom = pFrom;
        mTo = pTo;
        mScrobbles.clear();
        mListAdapter.notifyDataSetChanged();
        mPage = 1;
        filter_string = null;

        KillTaskIfRunning(mGetItemsAsynctask);
        loadItems();
//        loadItemsFromWeb(mPage, pFrom, pTo);
    }

    @Override
    public void onPause() {
        super.onPause();
        isCreated = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        isCreated = false;//TODO isCreated должно быть true и false только В onPostExecute
        KillTaskIfRunning(mGetItemsAsynctask);
    }

    @Override
    public void onLoadingScrobblesSuccessful(List<Scrobble> scrobbles) {
        mListView.removeFooterView(mFooterView);
        isLoading = false;
        wasEmpty = false;

//        if (scrobbles.size() < AppContext.getInstance().getLimit())

        int size = scrobbles.size();
        if (size > 0) {

            onAllIsLoaded(size);

            list_head.setVisibility(View.VISIBLE);
            // result.get(i));
            mScrobbles.addAll(scrobbles);
            mListAdapter.notifyDataSetChanged();
            list_head_text = "Scrobbles: " + mListView.getCount() + ((filter_string == null) ? "" : " within " + filter_string);
            ((TextView) list_head.findViewById(R.id.list_head)).setText(list_head_text);
        }
    }

    protected void onAllIsLoaded(int size){
        if (size < AppContext.getInstance().getLimit()){
            allIsLoaded = true;
            Toast.makeText(getContext(), getResources().getText(R.string.all_scrobbles_are_loaded), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onException(Exception pException) {
        mListView.removeFooterView(mFooterView);
        isLoading = false;
        Toast.makeText(getContext(), pException.getMessage(), Toast.LENGTH_LONG).show();//TODO get context activity or fragment here?
    }

    @Override
    public void onLoadingRankedItemsSuccessful(List<RankedItem> rankedItems) {
        mListView.removeFooterView(mFooterView);
        isLoading = false;
        wasEmpty = false;

        if (rankedItems.size() > 0) {
//            list_head.setVisibility(View.VISIBLE);
            // result.get(i));
            mRankedItems.addAll(rankedItems);
            mListAdapter.notifyDataSetChanged();
//            list_head_text = "Scrobbles: " + mListView.getCount() + ((filter_string == null) ? "" : " within " + filter_string);
//            ((TextView) list_head.findViewById(R.id.list_head)).setText(list_head_text);
        }
    }
}
