package com.zebra.emdkaar;


import android.content.Context;
import android.util.Log;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EMDKWrapper extends GenericScanningLibrary
{
    //  How to send callbacks to the main application
    private IEMDKWrapperCommunication mainApp;
    private EMDKImplementationWrapper emdk;

    public EMDKWrapper(IEMDKWrapperCommunication mainApplication, Context c)
    {
        this.mainApp = mainApplication;
            emdk = new EMDKImplementationWrapper(c);
    }


    public void startScan(boolean isContinuous) {
        emdk.startScan(isContinuous);
    }

    public void setContinuousMode(boolean b) {
        emdk.setConuousMode(b);
    }

    public void onDestroy() {
        emdk.onDestroy();
    }

    public void onPause() {
        emdk.onPause();
    }

    public void onResume() {
        emdk.onResume();
    }

    public void stopScan() {
        emdk.stopScan();
    }

    public void setDecoders() {
        emdk.setDecodersRequested();
    }

    //  Need to wrap the EMDK logic in a private class as EMDKListener requires a public @Override
    //  whose parameter is only exposed through the Scanner jar
    private class EMDKImplementationWrapper implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener {
        private EMDKManager emdkManager = null;
        private BarcodeManager barcodeManager = null;
        private Scanner scanner = null;
        private List<ScannerInfo> deviceList;
        private int scannerIndex = 0; // Keep the selected scanner
        private boolean bContinuousMode = false;
        private String statusString = "";
        boolean setDecodersRequested = false;
        private static final String LOG_TAG = "EMDK Impl Wrapper";

        private EMDKImplementationWrapper(Context c)
        {
            deviceList = new ArrayList<ScannerInfo>();

            EMDKResults results = EMDKManager.getEMDKManager(c, this);
            if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
                mainApp.setStatus("Status: " + "EMDKManager object request failed!");
            }
        }

        @Override
        public void onOpened(EMDKManager emdkManager) {
            mainApp.setStatus("Status: " + "EMDK open success!");

            this.emdkManager = emdkManager;

            // Acquire the barcode manager resources
            barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

            // Add connection listener
            if (barcodeManager != null) {
                barcodeManager.addConnectionListener(this);
            }

        }

        @Override
        public void onClosed() {
            if (emdkManager != null) {

                // Remove connection listener
                if (barcodeManager != null){
                    barcodeManager.removeConnectionListener(this);
                    barcodeManager = null;
                }

                // Release all the resources
                emdkManager.release();
                emdkManager = null;
            }
            mainApp.setStatus("Status: " + "EMDK closed unexpectedly! Please close and restart the application.");
        }

        @Override
        public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {
            String status;
            String scannerName = "";

            String statusExtScanner = connectionState.toString();
            String scannerNameExtScanner = scannerInfo.getFriendlyName();

            if (deviceList.size() != 0) {
                scannerName = deviceList.get(scannerIndex).getFriendlyName();
            }

            if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {

                switch(connectionState) {
                    case CONNECTED:
                        deInitScanner();
                        setDecodersRequested();
                        initScanner();
                        break;
                    case DISCONNECTED:
                        deInitScanner();
                        mainApp.asyncUpdate(true);
                        break;
                }

                status = scannerNameExtScanner + ":" + statusExtScanner;
            }
            else {
                status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            }
                mainApp.setStatus(status);
        }

        @Override
        public void onData(ScanDataCollection scanDataCollection) {
            if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
                ArrayList<ScanData> scanData = scanDataCollection.getScanData();
                for(ScanData data : scanData) {
                    String dataString =  data.getData();
                    mainApp.setData(dataString);
                }
            }

        }

        @Override
        public void onStatus(StatusData statusData) {
            ScannerStates state = statusData.getState();
            switch(state) {
                case IDLE:
                    statusString = statusData.getFriendlyName()+" is enabled and idle...";
                    mainApp.setStatus(statusString);
                    if (!scanner.isReadPending())
                    {
                        if (setDecodersRequested)
                        {
                            //  Note: Decoders should only be set once the scanner is enabled.
                            setDecoders();
                            setDecodersRequested = false;
                        }
                    }
                    if (bContinuousMode) {
                        try {
                            // An attempt to use the scanner continuously and rapidly (with a delay < 100 ms between scans)
                            // may cause the scanner to pause momentarily before resuming the scanning.
                            // Hence add some delay (>= 100ms) before submitting the next read.
                            //  :/  Based on BarcodeSample1
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            scanner.read();
                        } catch (ScannerException e) {
                            statusString = e.getMessage();
                            mainApp.setStatus(statusString);
                        }
                    }
                    mainApp.asyncUpdate(true);
                    //new AsyncUiControlUpdate().execute(true);
                    break;
                case WAITING:
                    statusString = "Scanner is waiting for trigger press...";
                    mainApp.setStatus(statusString);
                    mainApp.asyncUpdate(false);
                    //AsyncUiControlUpdate().execute(false);
                    break;
                case SCANNING:
                    statusString = "Scanning...";
                    mainApp.setStatus(statusString);
                    mainApp.asyncUpdate(false);
                    //new AsyncUiControlUpdate().execute(false);
                    break;
                case DISABLED:
                    statusString = statusData.getFriendlyName()+" is disabled.";
                    mainApp.setStatus(statusString);
                    mainApp.asyncUpdate(true);
                    //new AsyncUiControlUpdate().execute(true);
                    break;
                case ERROR:
                    statusString = "An error has occurred.";
                    mainApp.setStatus(statusString);
                    mainApp.asyncUpdate(true);
                    break;
                default:
                    break;
            }
        }

        private void setDecodersRequested() {
            if ((scanner != null) && (scanner.isEnabled())) {
                if (scanner.isReadPending()) {
                    try {
                        setDecodersRequested = true;
                        scanner.cancelRead();
                    } catch (ScannerException e) {
                        mainApp.setStatus(e.getMessage());
                    }
                }
                else
                {
                    setDecoders();
                }
            }
            else
            {
                setDecodersRequested = true;
            }
        }

        private void initScanner() {

            if (scanner == null) {

                if ((deviceList != null) && (deviceList.size() != 0)) {
                    scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
                }
                else {
                    mainApp.setStatus("Status: " + "Failed to get the specified scanner device! Please close and restart the application.");
                    return;
                }

                if (scanner != null) {

                    scanner.addDataListener(this);
                    scanner.addStatusListener(this);

                    try {
                        scanner.enable();
                    } catch (ScannerException e) {

                        mainApp.setStatus("Status: " + e.getMessage());
                    }
                }else{
                    mainApp.setStatus("Status: " + "Failed to initialize the scanner device.");
                }
            }
        }

        private void deInitScanner() {

            if (scanner != null) {

                try {
                    scanner.cancelRead();
                    scanner.disable();

                } catch (ScannerException e) {

                    mainApp.setStatus("Status: " + e.getMessage());
                }
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                try{
                    scanner.release();
                } catch (ScannerException e) {

                    mainApp.setStatus("Status: " + e.getMessage());
                }

                scanner = null;
            }
        }

        private void startScan(boolean isContinuous) {

            if(scanner == null) {
                Log.e(LOG_TAG, "Scanner object is null");
            }

            if (scanner != null) {
                try {

                    if(scanner.isEnabled())
                    {
                        scanner.read();

                        if (isContinuous)
                            bContinuousMode = true;
                        else
                            bContinuousMode = false;

                        mainApp.asyncUpdate(false);
                    }
                    else
                    {
                        mainApp.setStatus("Status: Scanner is not enabled");
                    }

                } catch (ScannerException e) {

                    mainApp.setStatus("Status: " + e.getMessage());
                }
            }

        }

        private void setDecoders() {

            if (scanner == null) {
                Log.e(LOG_TAG, "Scanner object is null");
            }

            if ((scanner != null) && (scanner.isEnabled())) {
                try {

                    ScannerConfig config = scanner.getConfig();
                    config.decoderParams.ean8.enabled = true;
                    scanner.setConfig(config);

                } catch (ScannerException e) {

                    mainApp.setStatus("Status: " + e.getMessage());
                }
            }
        }


        private void stopScan() {

            if (scanner != null) {

                try {

                    // Reset continuous flag
                    bContinuousMode = false;

                    // Cancel the pending read.
                    scanner.cancelRead();

                    mainApp.asyncUpdate(true);
                    //new AsyncUiControlUpdate().execute(true);

                } catch (ScannerException e) {

                    mainApp.setStatus("Status: " + e.getMessage());
                }
            }
        }

        public void setConuousMode(boolean b) {
            bContinuousMode = b;
        }

        public void onDestroy() {
        // De-initialize scanner
            deInitScanner();

            // Remove connection listener
            if (barcodeManager != null) {
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
            }

            // Release all the resources
            if (emdkManager != null) {
                emdkManager.release();
                emdkManager = null;

            }

        }

        public void onPause() {
            // De-initialize scanner
            deInitScanner();

            // Remove connection listener
            if (barcodeManager != null) {
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
                deviceList = null;
            }

            // Release the barcode manager resources
            if (emdkManager != null) {
                emdkManager.release(FEATURE_TYPE.BARCODE);
            }
        }

        public void onResume() {
            // Acquire the barcode manager resources
            if (emdkManager != null) {
                barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

                // Add connection listener
                if (barcodeManager != null) {
                    barcodeManager.addConnectionListener(this);
                }

                // Initialize scanner
                setDecodersRequested();
                initScanner();
            }
        }
    }

}



