package com.shizq.bika.database;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class HistoryRepository {
    private LiveData<List<History>> listLiveData;
    private LiveData<List<History>> listLiveFirstPageData;
    private HistoryDao historyDao;

    public HistoryRepository(Context context) {
        HistoryDatabase historyDatabase = HistoryDatabase.Companion.getDatabase(context.getApplicationContext());
        historyDao = historyDatabase.getHistoryDao();
        listLiveData = historyDao.getAllHistoryLive();
        listLiveFirstPageData = historyDao.getFirstPageHistoryLive();
    }

    public LiveData<List<History>> getListLiveData() {
        return listLiveData;
    }
    public LiveData<List<History>> getFirstPageLiveData() {
        return listLiveFirstPageData;
    }

//    public List<History> getAllHistory(int page) {
//        return historyDao.gatAllHistory(page);
//    }

    public List<History> getAllHistory(int page) throws ExecutionException, InterruptedException {
        return new GatAllHistory(historyDao).execute(String.valueOf(page)).get();
    }

    public List<History> getHistory(String id) throws ExecutionException, InterruptedException {
        return new GatHistory(historyDao).execute(id).get();
    }

    public void insertHistory(History... history) {
        new InsertAsyncTask(historyDao).execute(history);
    }

    public void updateHistory(History... history) {
        new UpdateAsyncTask(historyDao).execute(history);
    }

    public void deleteHistory(History... history) {
        new DeleteAsyncTask(historyDao).execute(history);
    }

    public void deleteAllHistory() {
        new DeleteAllAsyncTask(historyDao).execute();
    }

    static class GatAllHistory extends AsyncTask<String, Void,  List<History>> {
        private HistoryDao historyDao;

        GatAllHistory(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        @Override
        protected List<History> doInBackground(String... string) {
            return historyDao.gatAllHistory(string);
        }

    }

    static class GatHistory extends AsyncTask<String, Void,  List<History>> {
        private HistoryDao historyDao;

        GatHistory(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        @Override
        protected List<History> doInBackground(String... string) {
            return historyDao.gatHistory(string);
        }

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

    static class UpdateAsyncTask extends AsyncTask<History, Void, Void> {
        private HistoryDao historyDao;

        UpdateAsyncTask(HistoryDao historyDao) {
            this.historyDao = historyDao;
        }

        @Override
        protected Void doInBackground(History... history) {
            historyDao.updateHistory(history);
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
