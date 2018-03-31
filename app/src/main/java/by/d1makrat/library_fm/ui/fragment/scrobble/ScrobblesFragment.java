package by.d1makrat.library_fm.ui.fragment.scrobble;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import by.d1makrat.library_fm.AppContext;
import by.d1makrat.library_fm.R;
import by.d1makrat.library_fm.adapter.list.ScrobblesAdapter;
import by.d1makrat.library_fm.asynctask.GetItemsCallback;
import by.d1makrat.library_fm.model.Scrobble;
import by.d1makrat.library_fm.ui.CenteredToast;
import by.d1makrat.library_fm.ui.fragment.ItemsFragment;
import by.d1makrat.library_fm.ui.fragment.dialog.FilterDialogFragment;
import by.d1makrat.library_fm.utils.DateUtils;

import static by.d1makrat.library_fm.Constants.DATE_LONG_DEFAUT_VALUE;
import static by.d1makrat.library_fm.Constants.FILTER_DIALOG_FROM_BUNDLE_KEY;
import static by.d1makrat.library_fm.Constants.FILTER_DIALOG_TO_BUNDLE_KEY;
import static by.d1makrat.library_fm.Constants.RECENT_SCROBBLES_FRAGMENT_TAG;

public abstract class ScrobblesFragment extends ItemsFragment<Scrobble> implements FilterDialogFragment.FilterDialogListener, GetItemsCallback<Scrobble> {

    private static final String FILTER_DIALOG_KEY = "FilterDialogFragment";

    private TextView listHeadTextView;

    protected Long mFrom = DATE_LONG_DEFAUT_VALUE;
    protected Long mTo = DATE_LONG_DEFAUT_VALUE;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.list_with_head, container, false);

        setUpRecyclerView(rootView);
        setUpActionBar((AppCompatActivity) getActivity());

        listHeadTextView = rootView.findViewById(R.id.list_head);

        if (mListAdapter.isEmpty()) {
            setUpListHead(mListAdapter.getItemCount(), mFrom, mTo, View.INVISIBLE);
        } else {
            setUpListHead(mListAdapter.getItemCount(), mFrom, mTo, View.VISIBLE);
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                    if (!isLoading) {
                        allIsLoaded = false;

                        killTaskIfRunning(mGetItemsAsynctask);

                        mListAdapter.removeAll();
                        setUpListHead(mListAdapter.getItemCount(), mFrom, mTo, View.INVISIBLE);

                        mPage = 1;
                        loadItems();
                    }
                return true;
            case R.id.action_filter:
                if (!isLoading) {
                    if (getFragmentManager() != null) {
                        FilterDialogFragment dialogFragment = new FilterDialogFragment();
                        Bundle args = new Bundle();
                        args.putLong(FILTER_DIALOG_FROM_BUNDLE_KEY, mFrom);
                        args.putLong(FILTER_DIALOG_TO_BUNDLE_KEY, mTo);
                        dialogFragment.setArguments(args);
                        dialogFragment.setTargetFragment(this, 0);
                        dialogFragment.show(getFragmentManager(), FILTER_DIALOG_KEY);
                    }
                }
                return true;
            case R.id.open_in_browser:
                Intent intent;

                if (mFrom != null && mTo != null) {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(DateUtils.getUrlFromTimestamps(mUrlForBrowser, mFrom, mTo)));
                }
                else {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mUrlForBrowser));
                }

                startActivity(intent);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (getActivity() != null) {
            getActivity().getMenuInflater().inflate(R.menu.menu_context, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem pMenuItem) {

        Scrobble listItemPressed = mListAdapter.getSelectedItem();

        switch (pMenuItem.getItemId()) {
            case R.id.scrobbles_of_artist:
                replaceFragment(listItemPressed.getArtist(), null, null);
                return true;
            case R.id.scrobbles_of_track:
                replaceFragment(listItemPressed.getArtist(), listItemPressed.getTrackTitle(), null);
                return true;
            case R.id.scrobbles_of_album:
                replaceFragment(listItemPressed.getArtist(), null, listItemPressed.getAlbum());
                return true;
            case R.id.scrobbles_of_day:
                if (this instanceof RecentScrobblesFragment) {
                    onFinishFilterDialog(DateUtils.getStartTimestampOfDay(listItemPressed.getRawDate()), DateUtils.getEndTimestampOfDay(listItemPressed.getRawDate()));
                }
                else {
                    Bundle bundle = new Bundle();
                    Fragment fragment = new RecentScrobblesFragment();

                    bundle.putLong(FILTER_DIALOG_FROM_BUNDLE_KEY, DateUtils.getStartTimestampOfDay(listItemPressed.getRawDate()));
                    bundle.putLong(FILTER_DIALOG_TO_BUNDLE_KEY, DateUtils.getEndTimestampOfDay(listItemPressed.getRawDate()));

                    fragment.setArguments(bundle);

                    FragmentActivity activity = getActivity();
                    if (activity != null) {
                        activity.getSupportFragmentManager().beginTransaction()
                                .setCustomAnimations(R.anim.appear_from_right, R.anim.disappear_to_left,
                                        R.anim.appear_from_left, R.anim.disappear_to_right)
                                .replace(R.id.content_main, fragment, RECENT_SCROBBLES_FRAGMENT_TAG)
                                .addToBackStack(null)
                                .commit();
                    }
                }
                return true;
            default:
                return super.onContextItemSelected(pMenuItem);
        }
    }

    @Override
    public void onFinishFilterDialog(Long pFrom, Long pTo) {

        allIsLoaded = false;
        mFrom = pFrom;
        mTo = pTo;

        mListAdapter.removeAll();
        setUpListHead(mListAdapter.getItemCount(), mFrom, mTo, View.INVISIBLE);

        mPage = 1;

        killTaskIfRunning(mGetItemsAsynctask);
        loadItems();
    }

    private void setUpListHead(int pItemCount, Long pFrom, Long pTo, int pVisibility) {
        listHeadTextView.setVisibility(pVisibility);
        if (pVisibility == View.VISIBLE){
            listHeadTextView.setText(DateUtils.getMessageFromTimestamps(pItemCount, pFrom, pTo));
        }
    }

    @Override
    protected ScrobblesAdapter createAdapter(LayoutInflater pLayoutInflater){
        return new ScrobblesAdapter(pLayoutInflater, ContextCompat.getDrawable(AppContext.getInstance(), R.drawable.img_vinyl));//TODO resourcecompat
    }

    protected abstract void performOperation();

    @Override
    public void onStop() {
        super.onStop();

        killTaskIfRunning(mGetItemsAsynctask);
    }

    @Override
    public void onLoadingSuccessful(List<Scrobble> items) {
        isLoading = false;

        mListAdapter.removeAllHeadersAndFooters();

        int size = items.size();
        if (size > 0) {
            mListAdapter.addAll(items);
            setUpListHead(mListAdapter.getItemCount(), mFrom, mTo, View.VISIBLE);

            checkIfAllIsLoaded(size);
        }
        else if (mListAdapter.isEmpty()){
            mListAdapter.addEmptyHeader(DateUtils.getMessageFromTimestamps(mListAdapter.getItemCount(), mFrom, mTo));
        }
        else {
            checkIfAllIsLoaded(size);
        }
    }

    protected void checkIfAllIsLoaded(int size){
        if (size < AppContext.getInstance().getLimit()){
            allIsLoaded = true;
            CenteredToast.show(getContext(), R.string.all_scrobbles_are_loaded, Toast.LENGTH_SHORT);
        }
    }

}
