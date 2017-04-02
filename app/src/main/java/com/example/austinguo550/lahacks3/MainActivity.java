package com.example.austinguo550.lahacks3;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import static com.example.austinguo550.lahacks3.SessionService.SessionStatus.PLAYING;


public class MainActivity extends AppCompatActivity {
    /** Called when the activity is first created. */
    // Loopback l = null;


    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode) {
            case 200://REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) finish();
    }

    private SessionService mSessionService;
    private boolean mIsBound;

    private ServiceConnection mSessionConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service. Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            mSessionService = ((SessionService.SessionBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            mSessionService = null;
        }
    };

    void doBindService() {
        // Establish a connection with the service. We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, SessionService.class), mSessionConnection,
                Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mSessionConnection);
            mIsBound = false;
        }
    }

    Timer refreshTimer = null;

    Handler mHandler = new Handler();

    TextView textStatus, textListen;

    List<RadioButton> radioButtons = new ArrayList<RadioButton>();

    Uri mCreateDataUri = null;
    String mCreateDataType = null;
    String mCreateDataExtraText = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        //ActivityCompat.requestPermissions(this,permissions,200);


        textStatus = (TextView) findViewById(R.id.TextStatus);
        textListen = (TextView) findViewById(R.id.TextListen);

        Button t = (Button) findViewById(R.id.button);
        t.setOnClickListener(mPlayListener);

        t = (Button) findViewById(R.id.button7);
        t.setOnClickListener(mListenListener);

        radioButtons.add((RadioButton) findViewById(R.id.radioButton6));
        radioButtons.add((RadioButton) findViewById(R.id.radioButton8));
        radioButtons.add((RadioButton) findViewById(R.id.radioButton9));
        radioButtons.add((RadioButton) findViewById(R.id.radioButton10));
        radioButtons.add((RadioButton) findViewById(R.id.radioButton11));

        for (RadioButton button : radioButtons) {
            button.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                public void onCheckedChanged(CompoundButton buttonView,
                                             boolean isChecked) {
                    if (isChecked)
                        processRadioButtonClick(buttonView);
                }
            });
        }

        final Intent intent = getIntent();
        final String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {

            mCreateDataUri = intent.getData();
            mCreateDataType = intent.getType();

            if (mCreateDataUri == null) {
                mCreateDataUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            }

            mCreateDataExtraText = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (mCreateDataUri == null)
                mCreateDataType = null;

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(null));
        }

        doBindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    View.OnClickListener mPlayListener = new View.OnClickListener() {
        public void onClick(View v) {
            EditText e = (EditText) findViewById(R.id.editText);
            String s = e.getText().toString();
            byte[] input = s.getBytes();


            mSessionService.startSession(s);

            int numSegs = input.length/100;
            int i = 0;
            long millisPlayTime = -1;
            do {
                byte[] testBytes;
                if (input.length < i*100 + 100) {
                    testBytes = Arrays.copyOfRange(input, i*100, input.length);
                }
                else {
                    testBytes = Arrays.copyOfRange(input, i*100, i*100+100);
                }
                long tPlay = playByteSized(testBytes, playStatus, delay);
                if (tPlay > -1)
                    // start listening when playing is finished
                    mTimer.schedule(new StatusUpdateTimerTask(SessionStatus.LISTENING), tPlay);

                setTimeout(correctBroadcast, true);

                if (tPlay == -1) {
                    millisPlayTime = -1;
                }
                else {
                    millisPlayTime += tPlay;
                }
                i++;
            } while (i < numSegs);

            return millisPlayTime;
        }
    };

    View.OnClickListener mListenListener = new View.OnClickListener() {
        public void onClick(View v) {
            mSessionService.sessionReset();
            if (mSessionService.getStatus() == SessionService.SessionStatus.NONE
                    || mSessionService.getStatus() == SessionService.SessionStatus.FINISHED) {
                mSessionService.listen();
                ((Button) v).setText("Stop listening");
            } else {
                mSessionService.stopListening();
                ((Button) v).setText("Listen");
            }

        }
    };

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {

        // if( l != null )
        // l.stopLoop();

        super.onPause();

        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }

        mSessionService.stopListening();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        String sent = null;

        if (mCreateDataExtraText != null) {
            sent = mCreateDataExtraText;
        } else if (mCreateDataType != null
                && mCreateDataType.startsWith("text/")) {
            // read the URI into a string

            byte[] b = readDataFromUri(this.mCreateDataUri);
            if (b != null)
                sent = new String(b);

        }

        if (sent != null) {
            EditText e = (EditText) findViewById(R.id.editText);
            e.setText(sent);
        }

        refreshTimer = new Timer();

        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                mHandler.post(new Runnable() // have to do this on the UI thread
                {
                    public void run() {
                        updateResults();
                    }
                });

            }
        }, 500, 500);

    }

    private void processRadioButtonClick(CompoundButton buttonView) {
        for (RadioButton button : radioButtons) {
            if (button != buttonView)
                button.setChecked(false);
        }
    }

    private void setRadioGroupUnchecked() {
        for (RadioButton button : radioButtons) {
            button.setChecked(false);
        }
    }

    private void setRadioGroupChecked(SessionService.SessionStatus s) {
        RadioButton rb = null;
        switch (s) {
            case PLAYING:
                rb = (RadioButton) findViewById(R.id.radioButton6);
                break;
            case LISTENING:
                rb = (RadioButton) findViewById(R.id.radioButton8);
                break;
            case HELPING:
                rb = (RadioButton) findViewById(R.id.radioButton9);
                break;
            case SOS:
                rb = (RadioButton) findViewById(R.id.radioButton10);
                break;
            case FINISHED:
                rb = (RadioButton) findViewById(R.id.radioButton11);
                break;
            case NONE:
                setRadioGroupUnchecked();
                return;
        }
        rb.setChecked(true);
    }

    private void updateResults() {
        if (mSessionService.getStatus() == SessionService.SessionStatus.LISTENING) {
            textStatus.setText(mSessionService.getBacklogStatus());
            Log.d("DEBUG", textStatus.toString());
            textListen.setText(mSessionService.getListenString());

            Button b = (Button) findViewById(R.id.button7);
            b.setText("Stop listening");
        } else if (mSessionService.getStatus() == SessionService.SessionStatus.FINISHED) {
            Button b = (Button) findViewById(R.id.button7);
            b.setText("Listen");
            textStatus.setText("");
        } else {
            textStatus.setText("");
        }
        setRadioGroupChecked(mSessionService.getStatus());
    }

	/*
	 * private void encode( String inputFile, String outputFile ) {
	 *
	 * try {
	 *
	 * //There was an output file specified, so we should write the wav
	 * System.out.println("Encoding " + inputFile);
	 * AudioUtils.encodeFileToWav(new File(inputFile), new File(outputFile));
	 *
	 * } catch (Exception e) { System.out.println("Could not encode " +
	 * inputFile + " because of " + e); }
	 *
	 * }
	 */

    private byte[] readDataFromUri(Uri uri) {
        byte[] buffer = null;

        try {
            InputStream stream = getContentResolver().openInputStream(uri);

            int bytesAvailable = stream.available();
            // int maxBufferSize = 1024;
            int bufferSize = bytesAvailable; // Math.min(bytesAvailable,
            // maxBufferSize);
            int totalRead = 0;
            buffer = new byte[bufferSize];

            // read file and write it into form...
            int bytesRead = stream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                bytesRead = stream.read(buffer, totalRead, bufferSize);
                totalRead += bytesRead;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

        return buffer;
    }

}
