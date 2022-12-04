package com.shizq.bika.db;

import android.content.Context;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import java.util.List;

public class SearchRepository {
    private LiveData<List<String>> listLiveData;
    private SearchDao searchDao;


    public LiveData<List<String>> getListLiveData() {
        return listLiveData;
    }

    public SearchRepository(Context context) {
        SearchDatabase wordDatabase = SearchDatabase.Companion.getDatabase(context.getApplicationContext());
        searchDao = wordDatabase.getSearchDao();
        listLiveData = searchDao.getAllSearchLive();
    }

    public void insertSearch(Search... search) {
        new InsertAsyncTask(searchDao).execute(search);
    }

    public void updateSearch(Search... search) {
        new UpdateAsyncTask(searchDao).execute(search);
    }

    public void deleteSearch(Search... search) {
        new DeleteAsyncTask(searchDao).execute(search);
    }

    public void deleteAllSearch() {
        new DeleteAllAsyncTask(searchDao).execute();
    }


    static class InsertAsyncTask extends AsyncTask<Search, Void, Void> {
        private SearchDao searchDao;

        InsertAsyncTask(SearchDao searchDao) {
            this.searchDao = searchDao;
        }

        @Override
        protected Void doInBackground(Search... search) {
            searchDao.insertSearch(search);
            return null;
        }

    }

    static class UpdateAsyncTask extends AsyncTask<Search, Void, Void> {
        private SearchDao searchDao;

        UpdateAsyncTask(SearchDao searchDao) {
            this.searchDao = searchDao;
        }

        @Override
        protected Void doInBackground(Search... search) {
            searchDao.updateSearch(search);
            return null;
        }

    }

    static class DeleteAsyncTask extends AsyncTask<Search, Void, Void> {
        private SearchDao searchDao;

        DeleteAsyncTask(SearchDao searchDao) {
            this.searchDao = searchDao;
        }

        @Override
        protected Void doInBackground(Search... search) {
            searchDao.deleteSearch(search);
            return null;
        }

    }

    static class DeleteAllAsyncTask extends AsyncTask<Void, Void, Void> {
        private SearchDao searchDao;

        DeleteAllAsyncTask(SearchDao searchDao) {
            this.searchDao = searchDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            searchDao.deleteAllSearch();
            return null;
        }

    }
}
