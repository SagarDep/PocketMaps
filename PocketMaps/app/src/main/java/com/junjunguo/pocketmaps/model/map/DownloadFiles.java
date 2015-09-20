package com.junjunguo.pocketmaps.model.map;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.junjunguo.pocketmaps.model.dataType.MyMap;
import com.junjunguo.pocketmaps.model.listeners.MapDownloadListener;
import com.junjunguo.pocketmaps.model.util.Constant;
import com.junjunguo.pocketmaps.model.util.MyApp;
import com.junjunguo.pocketmaps.model.util.Variable;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This file is part of Pockets Maps
 * <p/>
 * Created by GuoJunjun <junjunguo.com> on June 14, 2015.
 */
public class DownloadFiles {
    private List<MapDownloadListener> mapDownloadListeners;
    private static DownloadFiles downloadFiles;
    private boolean asytaskFinished;
    private AsyncTask asyncTask;
    private MapDownloader mapDownloader;

    private DownloadFiles() {
        this.mapDownloadListeners = new ArrayList<>();
        asytaskFinished = true;
    }

    public static DownloadFiles getDownloader() {
        if (downloadFiles == null) {
            downloadFiles = new DownloadFiles();
        }
        return downloadFiles;
    }

    /**
     * download and unzip map files and save it in  mapsFolder/currentArea-gh/
     *
     * @param mapsFolder maps folder for maps
     * @param mapName    area (country) to download
     * @param urlStr     download link
     */
    public void startDownload(final File mapsFolder, final String mapName, final String urlStr) {
        mapDownloader = new MapDownloader();
        final long startTime = System.currentTimeMillis();
        asytaskFinished = false;
        asyncTask = new AsyncTask<URL, Integer, MapDownloader>() {
            protected MapDownloader doInBackground(URL... params) {
                //                try {
                if (!mapsFolder.exists()) { mapsFolder.mkdirs();}
                mapDownloader.downloadFile(urlStr,
                        (new File(mapsFolder.getAbsolutePath(), urlStr.substring(urlStr.lastIndexOf("/") + 1)))
                                .getAbsolutePath(), mapName, new MapDownloadListener() {
                            public void downloadStart() {
                            }

                            public void downloadFinished() {
                                broadcastFinished(mapName);
                            }

                            public void progressUpdate(Integer value) {
                                publishProgress(value);
                            }
                        });
                //                } catch (IOException e) {
                //                    Variable.getVariable().setDownloadStatus(Constant.PAUSE);
                //                    asyncTask.cancel(true);
                //                    e.printStackTrace();
                //                    MyApp.tracker().send(new HitBuilders.ExceptionBuilder()
                //                            .setDescription(this.getClass().getSimpleName() + e.getMessage())
                // .setFatal(false).build());
                //                }
                return mapDownloader;
            }

            protected void onCancelled() {
                super.onCancelled();
                asytaskFinished = true;
            }

            protected void onPreExecute() {
                super.onPreExecute();
                asytaskFinished = true;
                broadcastStart();
                Variable.getVariable().setDownloadStatus(Constant.DOWNLOADING);
            }

            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                broadcastOnUpdate(values[0]);
            }

            protected void onPostExecute(MapDownloader mapDownloader) {
                super.onPostExecute(mapDownloader);
                long endTime = System.currentTimeMillis();
                MyApp.tracker().send(new HitBuilders.TimingBuilder().setCategory("DownloadMap")
                        .setValue((endTime - startTime) / 1000)
                        .setVariable("s," + Variable.getVariable().getMapFinishedPercentage() + "%").setLabel(mapName)
                        .build());
            }
        }.execute();
    }

    /**
     * add to broadcast list
     *
     * @param listener
     */
    public void addListener(MapDownloadListener listener) {
        //        log("add listener before- "+mapDownloadListeners.toString());
        if (!mapDownloadListeners.contains(listener)) this.mapDownloadListeners.add(listener);
        //        log("add listener before- "+mapDownloadListeners.toString());
    }

    /**
     * remove listener from broadcast list
     *
     * @param listener
     */
    public void removeListener(MapDownloadListener listener) {
        //        log("remove listener before- "+mapDownloadListeners.toString());
        this.mapDownloadListeners.remove(listener);
        //        log("remove listener after-"+mapDownloadListeners.toString());
    }

    /**
     * broadcast download finished
     *
     * @param mapName
     */
    private void broadcastFinished(String mapName) {
        Variable.getVariable().setDownloadStatus(Constant.COMPLETE);
        for (MapDownloadListener listener : mapDownloadListeners) {
            //            log("download file finished - " + listener.getClass().getSimpleName());
            listener.downloadFinished();
        }
        Variable.getVariable().addRecentDownloadedMap(new MyMap(mapName));
        // load map to local select list when finish downloading ?
        broadcastFinished(mapName);
    }

    /**
     * broadcast download start
     */
    private void broadcastStart() {
        for (MapDownloadListener listener : mapDownloadListeners) {
            listener.downloadStart();
        }
    }

    /**
     * broadcast download start
     *
     * @param value
     */
    private void broadcastOnUpdate(Integer value) {
        for (MapDownloadListener listener : mapDownloadListeners) {
            listener.progressUpdate(value);
        }
    }

    public boolean isAsytaskFinished() {
        return asytaskFinished;
    }

    public void cancelAsyncTask() { asyncTask.cancel(true);}

    /**
     * send message to logcat
     *
     * @param str
     */
    private void log(String str) {
        Log.i(this.getClass().getSimpleName(), str);
    }

}