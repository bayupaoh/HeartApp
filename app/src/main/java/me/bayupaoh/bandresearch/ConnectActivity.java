package me.bayupaoh.bandresearch;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandPendingResult;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.SampleRate;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.lang.ref.WeakReference;

public class ConnectActivity extends AppCompatActivity {

    private BandClient client = null;
    Button btnTap;
    Button btnConsen;
    Button btnDisconnect;
    Button btnHelp;
    ProgressBar progressBar;
    TextView status;
    TextView denyut;
    LinearLayout linDenyut;
    LinearLayout linStatus;
    boolean accept;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);


        declarationWidget();
        final WeakReference<Activity> reference = new WeakReference<Activity>(this);
        btnTap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HeartRateSubscriptionTask().execute();
                new HeartRateConsentTask().execute(reference);
            }
        });

        btnConsen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HeartRateConsentTask().execute(reference);
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (client != null) {
                    try {
                        client.disconnect().await();
                        linDenyut.setVisibility(View.GONE);
                        linStatus.setVisibility(View.GONE);
                        btnConsen.setVisibility(View.GONE);
                        btnDisconnect.setVisibility(View.GONE);
                        btnTap.setVisibility(View.VISIBLE);
                    } catch (InterruptedException e) {
                        // Do nothing as this is happening during destroy
                    } catch (BandException e) {
                        // Do nothing as this is happening during destroy
                    }
                }
            }
        });
    }

    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(final BandHeartRateEvent event) {
            if (event != null) {
                String denyut = String.valueOf(event.getHeartRate())+" beat/minute";
                String status = String.valueOf(event.getQuality());

                appendToUI("",denyut,status);
            }
        }
    };


    private void declarationWidget() {
        btnTap = (Button) findViewById(R.id.btn_connect_btntap);
        progressBar = (ProgressBar) findViewById(R.id.progressbar_connect_connect);
        status = (TextView) findViewById(R.id.txtStatus);
        denyut = (TextView) findViewById(R.id.txtDenyut);
        btnConsen = (Button) findViewById(R.id.btn_connect_heartrateconsent);
        btnDisconnect = (Button) findViewById(R.id.btn_connect_disconnect);
        btnHelp = (Button) findViewById(R.id.btn_connect_help);
        linDenyut = (LinearLayout) findViewById(R.id.linear_conect_denyut);
        linStatus = (LinearLayout) findViewById(R.id.linear_conect_status);
    }


    private class HeartRateSubscriptionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (getConnectedBandClient()) {
                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    } else {
                        appendToUI("You have not given this application consent to access heart rate data yet."
                                + " Please press the Heart Rate Consent button.\n","","");
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n","","");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage,"","");

            } catch (Exception e) {
                appendToUI(e.getMessage(),"","");
            }
            return null;
        }
    }

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                                accept = consentGiven;
                            }
                        });

                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n","","");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage,"","");

            } catch (Exception e) {
                appendToUI(e.getMessage(),"","");
            }
            return null;
        }
    }

    private void appendToUI(final String stringPesan,final String stringDenyut,final String stringStatus) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(stringStatus.trim().equalsIgnoreCase("")){
                    Toast.makeText(getApplicationContext(),stringPesan,Toast.LENGTH_LONG).show();
                }else {
                    if(accept){
                        linDenyut.setVisibility(View.VISIBLE);
                        linStatus.setVisibility(View.VISIBLE);
                        status.setText(stringStatus);
                        denyut.setText(stringDenyut);
                        btnDisconnect.setVisibility(View.VISIBLE);
                        btnConsen.setVisibility(View.VISIBLE);
                        btnTap.setVisibility(View.GONE);
                    }
                }
            }
        });
    }

    private boolean getConnectedBandClient() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                appendToUI("Band isn't paired with your phone.\n","","");
                return false;
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        } else if (ConnectionState.CONNECTED == client.getConnectionState()) {
            accept = true;
            return true;
        }

        appendToUI("Band is connecting...\n","","");
        return ConnectionState.CONNECTED == client.connect().await();
    }    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException e) {
                // Do nothing as this is happening during destroy
            } catch (BandException e) {
                // Do nothing as this is happening during destroy
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        status.setText("");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (client != null) {
            try {
                client.getSensorManager().unregisterHeartRateEventListener(mHeartRateEventListener);
            } catch (BandIOException e) {
                appendToUI(e.getMessage(),"","");
            }
        }
    }



}
