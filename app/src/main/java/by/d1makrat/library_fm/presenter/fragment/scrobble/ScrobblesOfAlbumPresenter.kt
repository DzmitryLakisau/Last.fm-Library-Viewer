package by.d1makrat.library_fm.presenter.fragment.scrobble

import by.d1makrat.library_fm.AppContext
import by.d1makrat.library_fm.model.FilterRange
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ScrobblesOfAlbumPresenter(val artist: String, val album: String, filterRange: FilterRange): ScrobblesPresenter(filterRange) {

    init {
        mUrlForBrowser = AppContext.getInstance().user.url + "/library/music/" + artist + "/" + album
    }

    override fun performOperation() {
        compositeDisposable.add(
                repository.getScrobblesOfAlbum(artist, album,filterRange.startOfPeriod, filterRange.endOfPeriod)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                {
                                    onLoadingSuccessful(it)
                                },
                                {
                                    onException(it)
                                }
                        )
        )
    }

    override fun checkIfAllIsLoaded(size: Int) {
        allIsLoaded = true
        view?.showAllIsLoaded()
    }
}
