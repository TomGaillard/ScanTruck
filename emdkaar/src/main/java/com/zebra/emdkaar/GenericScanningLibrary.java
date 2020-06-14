package com.zebra.emdkaar;

public abstract class GenericScanningLibrary {

    abstract public void startScan(boolean isContinuous);
    abstract public void setContinuousMode(boolean b);
    abstract public void onPause();
    abstract public void onDestroy();
    abstract public void onResume();
    abstract public void stopScan();
    abstract public void setDecoders();
}
