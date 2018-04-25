package ie.ncirl.x14445618student.bmscontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SystemStatus extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    DatabaseReference currentRoomConditionRef;

    TextView lastUpdatedTv;
    TextView temperatureTv;
    TextView humidityTv;
    TextView statusTv;

    String last_update;
    String temperature;
    String humidity;
    String status;

    ImageView panelImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_status);
        setTitle("System Status");
        //Add Back Button to Action Bar - From https://stackoverflow.com/questions/12070744/add-back-button-to-action-bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://bmscontroller-bd5b4.firebaseio.com/");
        currentRoomConditionRef = databaseReference.child("currentRoomCondition");

        lastUpdatedTv = findViewById(R.id.lastUpdatedTv);
        temperatureTv = findViewById(R.id.temperatureTv);
        humidityTv = findViewById(R.id.humidityTv);
        statusTv = findViewById(R.id.statusTv);
        panelImg = findViewById(R.id.panelImg);



        getValues();
    }//End of OnCreate

    //Function to return to home when back button is pressed From --> Same link as "Add Back Button" above
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    public void getValues() {
//Get Data From Firebase From: https://firebase.google.com/docs/database/android/read-and-write
        currentRoomConditionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                     last_update = ds.child("last_updated").getValue().toString();
                     temperature = ds.child("temperature").getValue().toString() + " °C";
                     humidity = ds.child("humidity").getValue().toString() + " %";
                     status = ds.child("status").getValue().toString();
                }
                lastUpdatedTv.setText(last_update);
                temperatureTv.setText(temperature);
                humidityTv.setText(humidity);
                statusTv.setText(status);

                //Android Programatically Set Image Resource From : https://stackoverflow.com/questions/2974862/changing-imageview-source
                //Check if the status contains heating, cooling or other, if so change the images to parallel the mcb panel - Images created using https://www.draw.io/
                if(status.toLowerCase().contains("heating")){
                    panelImg.setImageResource(R.drawable.heatingimg);
                }
                else if(status.toLowerCase().contains("cooling")){
                    panelImg.setImageResource(R.drawable.coolingimg);
                }

                else{
                    panelImg.setImageResource(R.drawable.nocircuitimg);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
