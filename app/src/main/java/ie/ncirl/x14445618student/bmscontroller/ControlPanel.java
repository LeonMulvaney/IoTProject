package ie.ncirl.x14445618student.bmscontroller;

import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.UUID;

import static ie.ncirl.x14445618student.bmscontroller.TestConnection.LOG_TAG;

public class ControlPanel extends AppCompatActivity {
    // --- Constants to modify per your configuration ---

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "awgcjdf2esnf9.iot.eu-west-1.amazonaws.com";
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "eu-west-1:6031d169-6bb0-41f2-873f-cd4fc8251724";
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static final String AWS_IOT_POLICY_NAME = "BmsPiPolicy";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.EU_WEST_1;
    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "f53e54d260fdb97cb082d4d07f33608d723b4a718341333afa275d26a1f2088b";

    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePassword;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;

    //Declare Views & Variables

    //Set Sample Interval default to 5 seconds
    int sampleInterval =5;

    //Set Circuits to False as Default
    String automaticControlCircuit = "False";
    String heatingCircuit = "False";
    String coolingCircuit = "False";
    String lightingCircuit = "False";

    TextView tvClientId;
    TextView tvStatus;

    EditText topicEt;
    EditText sampleIntervalEt;;

    //Automatic Control Switch and Associated Views
    Switch automateSwitch;
    TextView automateStatusTv;

    //Heating Control Switch and Associated Views
    Switch heatingSwitch;
    TextView heatingStatusTv;

    //Cooling Control Switch and Associated Views
    Switch coolingSwitch;
    TextView coolingStatusTv;

    //Lighting Control Switch and Associated Views
    Switch lightingSwitch;
    TextView lightingStatusTv;

    //Firebase
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    DatabaseReference currentRoomConditionRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);
        setTitle("Control Panel");
        //Add Back Button to Action Bar - From https://stackoverflow.com/questions/12070744/add-back-button-to-action-bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Target Views
        tvClientId = findViewById(R.id.clientIdTv);
        tvStatus = findViewById(R.id.statusTv);

        //Set MQTT Topic in the OnCreate method
        topicEt = findViewById(R.id.topicEt);
        topicEt.setText("leonspi/controlPanel");

        //Sample Interval
        sampleIntervalEt = findViewById(R.id.sampleIntervalEt);
        sampleIntervalEt.setText("5");

        //Automatic Control Switch and Associated Views
        automateSwitch = findViewById(R.id.automateSwitch);
        automateStatusTv = findViewById(R.id.automateStatusTv);

        //Heating  Switch and Associated Views
        heatingSwitch = findViewById(R.id.heatingSwitch);
        heatingStatusTv = findViewById(R.id.heatingStatusTv);

        //Cooling Switch and Associated Views
        coolingSwitch = findViewById(R.id.coolingSwitch);
        coolingStatusTv = findViewById(R.id.coolingStatusTv);

        //Lighting Switch and Associated Views
        lightingSwitch = findViewById(R.id.lightingSwitch);
        lightingStatusTv = findViewById(R.id.lightingStatusTv);

        //Firebase Declarations
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://bmscontroller-bd5b4.firebaseio.com/");
        currentRoomConditionRef = databaseReference.child("currentRoomCondition");


        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        keystorePath = getFilesDir().getPath();
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                mIotAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPrincipalPolicyRequest policyAttachRequest =
                                new AttachPrincipalPolicyRequest();
                        policyAttachRequest.setPolicyName(AWS_IOT_POLICY_NAME);
                        policyAttachRequest.setPrincipal(createKeysAndCertificateResult
                                .getCertificateArn());
                        mIotAndroidClient.attachPrincipalPolicy(policyAttachRequest);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            }
                        });
                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }

        //Target Automatic Control Switch and Listen for changes
        automateSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try{
                }
                catch(NumberFormatException nfe) {
                    System.out.println(nfe);
                };

                if(isChecked ==true){
                    automaticControlCircuit = "true"; //Set Automatic Control Circuit to True
                    automateStatusTv.setText("System Automation: ON");

                    //Heating or Cooling Circuits cannot be Manually Overridden if System is in Automation Mode, set both to False
                    heatingSwitch.setChecked(false);
                    coolingSwitch.setChecked(false);

                }
                else{
                    automaticControlCircuit = "false";//Turn Sensor Off
                    automateStatusTv.setText("System Automation: OFF");
                }
            }
        });

        //Target Heating Circuit Switch and Listen for changes
        heatingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try{
                }
                catch(NumberFormatException nfe) {
                    System.out.println(nfe);
                };

                if(isChecked ==true){
                    heatingCircuit = "true"; //Set Heating Circuit to True
                    heatingStatusTv.setText("Heating Circuit: ON");

                    //Automatic or Cooling Circuits cannot be running if Heating is Manually Overridden, set both to False
                    automateSwitch.setChecked(false);
                    coolingSwitch.setChecked(false);

                }
                else{
                    heatingCircuit = "false";//Set Heating Circuit to False
                    heatingStatusTv.setText("Heating Circuit: OFF");
                }
            }
        });

        //Target Cooling Circuit Switch and Listen for changes
        coolingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try{
                }
                catch(NumberFormatException nfe) {
                    System.out.println(nfe);
                };

                if(isChecked ==true){
                    coolingCircuit = "true"; //Set Cooling Circuit to True
                    coolingStatusTv.setText("Cooling Circuit: ON");

                    //Automatic or Heating Circuits cannot be running if Cooling is Manually Overridden, set both to False
                    automateSwitch.setChecked(false);
                    heatingSwitch.setChecked(false);

                }
                else{
                    coolingCircuit = "false";//Set Cooling Circuit to True
                    coolingStatusTv.setText("Cooling Circuit: OFF");
                }
            }
        });

        //Target Lighting Circuit Switch and Listen for changes
        lightingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try{
                }
                catch(NumberFormatException nfe) {
                    System.out.println(nfe);
                };

                if(isChecked ==true){
                    lightingCircuit = "true"; //Set Lighting Circuit to True
                    lightingStatusTv.setText("Lighting Circuit: ON");

                    //Lighting Circuit Does not Affect the Automatic, Heaing or Cooling Circuits

                }
                else{
                    lightingCircuit = "false";//Set Lighting Circuit to True
                    lightingStatusTv.setText("Lighting Circuit: OFF");
                }
            }
        });

        //Automatically Call connect method - Thus, negating the need for a button to do so
        connect();
        updateSwitches(); //Call the updateSwitches Method at the end of onCreate which grabs the current status value from Firebase, then uses this value to place switches in respective positions

    } //End of OnCreate -------------------------------------

    //Function to return to home when back button is pressed From --> Same link as "Add Back Button" above
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    //Method run when the connect button is clicked - Changed from source code to suit project
    public void connect(){
        Log.d(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                tvStatus.setText("Connecting...");

                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                tvStatus.setText("Connected");
                                //Set text color programatically From: https://stackoverflow.com/questions/8472349/how-to-set-text-color-to-a-text-view-programmatically
                                tvStatus.setTextColor(Color.parseColor("#008000"));
                                Toast.makeText(getApplicationContext(),"Successfully Connected to AWS IoT",Toast.LENGTH_LONG).show();


                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                tvStatus.setText("Reconnecting");
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                if (throwable != null) {
                                    Log.e(LOG_TAG, "Connection error.", throwable);
                                }
                                tvStatus.setText("Disconnected");
                                tvStatus.setTextColor(Color.parseColor("#8B0000"));
                            } else {
                                tvStatus.setText("Disconnected");


                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            tvStatus.setText("Error!  " + e.getMessage());
        }

    }

    //Disconnect Method - Not Used
    public void disconnect(View view){
        try {
            mqttManager.disconnect();
            Toast.makeText(getApplicationContext(),"Successfully Disconnected to AWS IoT",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }


    //Publish data via MQTT to Message Broker (AWS IoT)
    public void publish(View view){
        final String topic = topicEt.getText().toString();
        sampleInterval = Integer.parseInt(sampleIntervalEt.getText().toString());

        //Counteract Unrealistic Sampling Intervals - If lower than 5, Default to 5 Seconds
        if(sampleInterval<5){
            Toast.makeText(getApplicationContext(),"Sampling Rate too low - Defaulting to 5 Seconds...",Toast.LENGTH_SHORT).show();
            sampleInterval =5;
        }

        //Parse data as JSON Object before sending to Broker
        JSONObject data = new JSONObject();
        try{
            data.put("sampleInterval",sampleInterval);
            data.put("automaticControlCircuit",automaticControlCircuit);
            data.put("heatingCircuit",heatingCircuit);
            data.put("coolingCircuit",coolingCircuit);
            data.put("lightingCircuit",lightingCircuit);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            mqttManager.publishString(data.toString(), topic, AWSIotMqttQos.QOS0);
            Toast.makeText(getApplicationContext(),"Successfully Published to Broker: \n" + data.toString(),Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
            Toast.makeText(this,"Publish error : " + e.toString(),Toast.LENGTH_LONG).show();
        }
    }


//This method uses the "status" variable which is sent from the Pi to Firebase to move the switches to their respective positions when the Activity is Closed and Reopened when the BMS system is running
    public void updateSwitches() {
        //Get Data From Firebase From: https://firebase.google.com/docs/database/android/read-and-write
        currentRoomConditionRef.addListenerForSingleValueEvent(new ValueEventListener() { //Use a Single Event Listener so it can place the Switches in the correct positions when the Activity is closed and then reopened
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    String status = ds.child("status").getValue().toString();
                    String lightStatus = ds.child("light_status").getValue().toString();

                    //Light Switch
                    if(lightStatus.equals("Lighting On")){
                        lightingSwitch.setChecked(true);
                    }
                    else{
                        lightingSwitch.setChecked(false);
                    }

                    //Heating, Cooling and Automatic Control Switches
                    if(status.equals("BMS IDLE")){
                        automateSwitch.setChecked(false);
                        heatingSwitch.setChecked(false);
                        coolingSwitch.setChecked(false);

                    }
                    else if(status.equals("Heating Circuit Running (Manual Override)")){
                        automateSwitch.setChecked(false);
                        heatingSwitch.setChecked(true);
                        coolingSwitch.setChecked(false);

                    }

                    else if(status.equals("Cooling Circuit Running (Manual Override)")){
                        automateSwitch.setChecked(false);
                        heatingSwitch.setChecked(false);
                        coolingSwitch.setChecked(true);

                    }

                    else if(status.equals("Automatic Control Active - Heating Operational")){
                        automateSwitch.setChecked(true);
                        heatingSwitch.setChecked(false);
                        coolingSwitch.setChecked(false);
                    }

                    else if(status.equals("Automatic Control Active - Cooling Operational")){
                        automateSwitch.setChecked(true);
                        heatingSwitch.setChecked(false);
                        coolingSwitch.setChecked(false);
                    }

                    else if(status.equals("Automatic Control Active - DEAD BAND")){
                        automateSwitch.setChecked(true);
                        heatingSwitch.setChecked(false);
                        coolingSwitch.setChecked(false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
