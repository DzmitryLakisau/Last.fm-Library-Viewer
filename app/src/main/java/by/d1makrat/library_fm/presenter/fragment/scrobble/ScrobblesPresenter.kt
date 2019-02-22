package by.d1makrat.library_fm.presenter.fragment.scrobble

import android.net.Uri
import by.d1makrat.library_fm.model.FilterRange
import by.d1makrat.library_fm.model.Scrobble
import by.d1makrat.library_fm.presenter.fragment.ItemsPresenter
import by.d1makrat.library_fm.utils.DateUtils
import by.d1makrat.library_fm.view.fragment.ScrobblesView

abstract class ScrobblesPresenter(var filterRange: FilterRange): ItemsPresenter<Scrobble, ScrobblesView<Scrobble>>() {

    fun onRefresh(){
        view?.hideListHead()

        stopLoading()
        loadFirstPage()
    }

    fun onFilter(){
        view?.showFilterDialog()
    }

    fun onOpenInBrowser() {
        view?.openBrowser(Uri.parse(DateUtils.getUrlFromTimestamps(mUrlForBrowser, filterRange)))
    }

    fun onLoadingSuccessful(scrobbles: List<Scrobble>) {
        view?.removeAllHeadersAndFooters()
        isLoading = false

        val size = scrobbles.size
        when {
            size > 0 -> {
                view?.populateList(scrobbles)
                view?.showListHead(DateUtils.getMessageFromTimestamps(view?.getListItemsCount()!!, filterRange))

                checkIfAllIsLoaded(size)
            }
            view?.getListItemsCount() == 0 -> view?.showEmptyHeader(DateUtils.getMessageFromTimestamps(view?.getListItemsCount() as Int, filterRange))
            else -> checkIfAllIsLoaded(size)
        }

        //hack when recyclerView not showing items after first load when loaded items take place that less than screen size
//        if (mLayoutManager.getChildCount() == mLayoutManager.getItemCount()) {
//            mPage++
//            loadItems()
//        }
    }

    fun onFilterDialogFinished(filterRange: FilterRange) {
        this.filterRange.startOfPeriod = filterRange.startOfPeriod
        this.filterRange.endOfPeriod = filterRange.endOfPeriod

        view?.hideListHead()

        stopLoading()
        loadFirstPage()
    }

    fun onCreatingNewView(){
        view?.hideListHead()

        loadFirstPage()
    }

    fun onShowingFromBackStack(itemCount: Int){
        view?.showListHead(DateUtils.getMessageFromTimestamps(itemCount, filterRange))
    }

    fun onScrobblesOfDayPressed(isRecentScrobblesFragment: Boolean, listItemPressed: Scrobble){
        if (isRecentScrobblesFragment)
            onFilterDialogFinished(DateUtils.getTimeRangesOfDay(listItemPressed.getRawDate()))
        else
            view?.openScrobblesFragment(DateUtils.getTimeRangesOfDay(listItemPressed.getRawDate()))
    }
}
