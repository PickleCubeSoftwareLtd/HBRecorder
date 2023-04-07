package com.hbisoft.hbrecorder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;


class FileObserver extends android.os.FileObserver {

    private List<SingleFileObserver> mObservers;
    private final String mPath;
    private final int mMask;
    private final FileObserverCallback callback;

    FileObserver(String path, FileObserverCallback callback) {
        super(path, ALL_EVENTS);
        mPath = path;
        mMask = ALL_EVENTS;
        this.callback = callback;
    }


    @Override
    public void startWatching() {
        if (mObservers != null) return;

        mObservers = new ArrayList<>();
        Stack<String> stack = new Stack<>();
        stack.push(mPath);

        while (!stack.isEmpty()) {
            String parent = stack.pop();
            mObservers.add(new SingleFileObserver(parent, mMask));
            File path = new File(parent);
            File[] files = path.listFiles();
            if (null == files) continue;

            for (File f : files) {
                if (f.isDirectory() && !f.getName().equals(".") && !f.getName().equals("..")) {
                    stack.push(f.getPath());
                }
            }
        }

        for (SingleFileObserver sfo : mObservers) {
            sfo.startWatching();
        }
    }

    @Override
    public void stopWatching() {
        if (mObservers == null) return;

        for (SingleFileObserver sfo : mObservers) {
            sfo.stopWatching();
        }
        mObservers.clear();
        mObservers = null;
    }

    @Override
    public void onEvent(int event, final String path) {
        if (event == android.os.FileObserver.CLOSE_WRITE) {
            Log.d("HBRecorderFileObserver", "CLOSE_WRITE for file: " + path);
            new Handler(Looper.getMainLooper()).post(() -> callback.onFileComplete(path));
        }
    }

    class SingleFileObserver extends android.os.FileObserver {
        final String mPath;

        SingleFileObserver(String path, int mask) {
            super(path, mask);
            mPath = path;
        }

        @Override
        public void onEvent(int event, String path) {
            String newPath = mPath + "/" + path;
            FileObserver.this.onEvent(event, newPath);
        }
    }
}
