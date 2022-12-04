package com.shizq.bika.db;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class HistoryRepository {
    private LiveData<List<History>> listLiveData;
    private HistoryDao historyDao;


    public LiveData<List<History>> getListLiveData() {
        return listLiveData;
    }

    public HistoryRepository(Context context) {
        HistoryDatabase historyDatabase = HistoryDatabase.Companion.getDatabase(context.getApplicationContext());
        historyDao = historyDatabase.getHistoryDao();
        listLiveData = historyDao.getAllHistoryLive();
    }

    public void insertHistory(History... history) {
        new InsertAsyncTask(historyDao).execute(history);
    }

    public void deleteHistory(History... history) {
        new DeleteAsyncTask(historyDao).execute(history);
    }

    public void deleteAllHistory() {
        new DeleteAllAsyncTask(historyDao).execute();
    }


    static class InsertAsyncTask extends AsyncTask<History, Void, Void> {
        private HistoryDao historyDao;

        InsertAsyncTask(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        @Override
        protected Void doInBackground(History... history) {
            historyDao.insertHistory(history);
            return null;
        }

    }

    static class DeleteAsyncTask extends AsyncTask<History, Void, Void> {
        private HistoryDao historyDao;

        DeleteAsyncTask(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        @Override
        protected Void doInBackground(History... history) {
            historyDao.deleteHistory(history);
            return null;
        }

    }

    static class DeleteAllAsyncTask extends AsyncTask<Void, Void, Void> {
        private HistoryDao historyDao;

        DeleteAllAsyncTask(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            historyDao.deleteAllHistory();
            return null;
        }

    }
}
