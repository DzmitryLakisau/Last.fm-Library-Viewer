package by.d1makrat.library_fm.ui.fragment.scrobble

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.*
import android.widget.TextView
import android.widget.Toast
import by.d1makrat.library_fm.AppContext
import by.d1makrat.library_fm.Constants
import by.d1makrat.library_fm.R
import by.d1makrat.library_fm.R.id.*
import by.d1makrat.library_fm.adapter.list.ItemsAdapter
import by.d1makrat.library_fm.adapter.list.ScrobblesAdapter
import by.d1makrat.library_fm.model.Scrobble
import by.d1makrat.library_fm.presenter.fragment.scrobble.ScrobblesPresenter
import by.d1makrat.library_fm.view.fragment.ScrobblesView
import by.d1makrat.library_fm.ui.CenteredToast
import by.d1makrat.library_fm.ui.activity.MainActivity
import by.d1makrat.library_fm.ui.fragment.ItemsFragment
import by.d1makrat.library_fm.ui.fragment.dialog.FilterDialogFragment

abstract class ScrobblesFragment: ItemsFragment<Scrobble, ScrobblesView<Scrobble>, ScrobblesPresenter>(), ScrobblesView<Scrobble>, FilterDialogFragment.FilterDialogListener {

    private val FILTER_DIALOG_KEY = "FilterDialogFragment"

    override fun showAllIsLoaded() {
        CenteredToast.show(context, R.string.all_scrobbles_are_loaded, Toast.LENGTH_SHORT)
    }

    private var listHeadTextView: TextView? = null

    protected var mFrom: Long? = Constants.DATE_LONG_DEFAUT_VALUE
    protected var mTo: Long? = Constants.DATE_LONG_DEFAUT_VALUE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.list_with_head, container, false)

        setUpRecyclerView(rootView)

        listHeadTextView = rootView.findViewById(R.id.list_head)

        if (mListAdapter!!.isEmpty) {
            hideListHead()
        } else {
            presenter?.showListHead(mListAdapter?.itemCount as Int)
        }

        presenter?.attachView(this)

        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_options, menu)
    }

     override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return when (item.itemId) {
             R.id.action_refresh -> {
                 presenter?.onRefresh()
                 true
             }
             R.id.action_filter -> {
                 presenter?.onFilter()
                 true
             }
             R.id.open_in_browser -> {
                 presenter?.onOpenInBrowser()
                 true
             }
             else -> super.onOptionsItemSelected(item)
         }
    }

    override fun showFilterDialog() {
        if (fragmentManager != null) {
            val dialogFragment = FilterDialogFragment()
            val args = Bundle()
            args.putLong(Constants.FILTER_DIALOG_FROM_BUNDLE_KEY, mFrom!!)
            args.putLong(Constants.FILTER_DIALOG_TO_BUNDLE_KEY, mTo!!)
            dialogFragment.arguments = args
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(fragmentManager, FILTER_DIALOG_KEY)
        }
    }

    override fun createAdapter(layoutInflater: LayoutInflater): ItemsAdapter<Scrobble> {
        return ScrobblesAdapter(layoutInflater, ContextCompat.getDrawable(AppContext.getInstance(), R.drawable.img_vinyl))//TODO resourcecompat
    }

    override fun hideListHead() {
        listHeadTextView?.visibility = View.INVISIBLE
    }

    override fun showListHead(message: String) {
        listHeadTextView?.text = message
        listHeadTextView?.visibility = View.VISIBLE
    }

    override fun showEmptyHeader(message: String) {
        mListAdapter?.addEmptyHeader(message)
    }

    override fun getListItemsCount(): Int {
        return mListAdapter?.itemCount ?: 0
    }

    override fun onFinishFilterDialog(pFrom: Long?, pTo: Long?) {
        presenter?.onFinishFilterDialog(pFrom, pTo)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        activity?.menuInflater?.inflate(R.menu.menu_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        val listItemPressed: Scrobble = mListAdapter?.selectedItem!!

        return when (item.itemId) {
            scrobbles_of_artist -> {
                (activity as MainActivity).showScrobblesOfArtistFragment(listItemPressed.Artist)
                true
            }
            scrobbles_of_track -> {
                (activity as MainActivity).showScrobblesOfTrackFragment(listItemPressed.Artist, listItemPressed.TrackTitle)
                true
            }
            scrobbles_of_album -> {
                (activity as MainActivity).showScrobblesOfAlbumFragment(listItemPressed.Artist, listItemPressed.Album!!)
                true
            }
            scrobbles_of_day -> {
                presenter?.onScrobblesOfDayPressed(this is RecentScrobblesFragment, listItemPressed)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    override fun showScrobblesOfDay(startOfDay: Long, endOfDay: Long) {
        (activity as MainActivity).showScrobblesOfDay(startOfDay, endOfDay)
    }
}
