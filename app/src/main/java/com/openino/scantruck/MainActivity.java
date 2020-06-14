package com.openino.scantruck;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.zebra.emdkaar.EMDKWrapper;
import com.zebra.emdkaar.GenericScanningLibrary;
import com.zebra.emdkaar.IEMDKWrapperCommunication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class MainActivity extends Activity implements OnCheckedChangeListener, IEMDKWrapperCommunication {

    private TextView textViewData = null;
    private TextView textViewStatus = null;
    private CheckBox checkBoxContinuous = null;
    private Button buttonEnvoie = null;
    private EditText editTextNom = null;
    private EditText editTextQuantite = null;
    private String Nomactu = null;
    private int Quantiteactu = 1;

    private int dataLength = 0;
    private GenericScanningLibrary scannerObj = null;
    private Codebar datacodebaractu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scannerObj = new EMDKWrapper(this, getApplicationContext());
        textViewStatus = findViewById(R.id.textViewStatus);
        checkBoxContinuous = findViewById(R.id.checkBoxContinuous);
        buttonEnvoie = findViewById(R.id.buttonenvoie);
        editTextNom =   findViewById(R.id.editTextNom);
        editTextQuantite = findViewById(R.id.editTextQuantité);


        addStartScanButtonListener();
        addStopScanButtonListener();
        addListenerOnButton();

        textViewData.setSelected(true);
        textViewData.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scannerObj != null)
            scannerObj.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background
        if (scannerObj != null)
            scannerObj.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground
        if (scannerObj != null)
            scannerObj.onResume();
    }

    private void addStartScanButtonListener() {

        Button btnStartScan = findViewById(R.id.buttonStartScan);

        btnStartScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (scannerObj != null)
                    scannerObj.startScan(checkBoxContinuous.isChecked());
            }
        });
    }

    private void addStopScanButtonListener() {

        Button btnStopScan = findViewById(R.id.buttonStopScan);

        btnStopScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (scannerObj != null)
                    scannerObj.stopScan();
            }
        });
    }


    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (scannerObj == null)
            return;
        scannerObj.setDecoders();
    }

    @Override
    public void setStatus(String status) {
        //  Received a status update from the scanner aar
        new AsyncStatusUpdate().execute(status);
    }

    @Override
    public void setData(String dataString) {
        //  Received a data update from the scanner aar
        new AsyncDataUpdate().execute(dataString);
    }

    @Override
    public void asyncUpdate(boolean b) {
        new AsyncUiControlUpdate().execute(b);
    }

    private class AsyncDataUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        protected void onPostExecute(String result) {

            if (result != null) {
                if(dataLength ++ > 100) { //Clear the cache after 100 scans
                    textViewData.setText("");
                    dataLength = 0;
                }
                textViewData.setText(result);
                editTextNom.setVisibility(View.VISIBLE);
                editTextQuantite.setVisibility(View.VISIBLE);
                buttonEnvoie.setVisibility(View.VISIBLE);
            }
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            textViewStatus.setText("Status: " + result);
        }
    }

    private class AsyncUiControlUpdate extends AsyncTask<Boolean, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean bEnable) {

        }

        @Override
        protected Boolean doInBackground(Boolean... arg0) {

            return arg0[0];
        }
    }

    public void addListenerOnButton() {

        buttonEnvoie.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (editTextNom.getText() != null ){
                    datacodebaractu.code = textViewData.getText().toString();
                    datacodebaractu.name = editTextNom.getText().toString();
                    Nomactu = editTextNom.getText().toString();
                    datacodebaractu.Quantite = Integer.parseInt(editTextQuantite.getText().toString());
                    Quantiteactu = Integer.parseInt(editTextQuantite.getText().toString());
                    String jsonInputString = "{'id': '1' ; 'codeBarre' : '"+ datacodebaractu.code +"' ; 'name': '" + datacodebaractu.name + "' ; 'quantite': '"+ datacodebaractu.Quantite +"'}";


                    URL url = null;
                    HttpURLConnection connection = null;
                    try {
                        try {
                            url = new URL("http://92.68.1.25:9090/product");
                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("POST");
                            connection.setRequestProperty("Content-Type", "application/json; utf-8");
                            connection.setRequestProperty("Accept", "application/json");
                            connection.setDoOutput(true);
                            try(OutputStream os = connection.getOutputStream()) {
                                byte[] input = jsonInputString.getBytes("utf-8");
                                os.write(input, 0, input.length);
                            } catch (IOException e) { textViewStatus.setText("Erreur connection Base de donnée");
                            }

                        } catch (MalformedURLException | ProtocolException e) {
                            textViewStatus.setText("Erreur connection Base de donnée");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally {
                    connection.disconnect();
                    }
                    editTextNom.setVisibility(View.INVISIBLE);
                    editTextQuantite.setVisibility(View.INVISIBLE);
                    buttonEnvoie.setVisibility(View.INVISIBLE);
                    editTextNom.setText(Nomactu);
                    editTextQuantite.setText(Quantiteactu);
                }

            }

        });

    }

}
