package ie.ncirl.x14445618student.bmscontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

    String last_update;
    String temperature;
    String humidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_status);

        //Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://bmscontroller-bd5b4.firebaseio.com/");
        currentRoomConditionRef = databaseReference.child("currentRoomCondition");

        lastUpdatedTv = findViewById(R.id.lastUpdatedTv);
        temperatureTv = findViewById(R.id.temperatureTv);
        humidityTv = findViewById(R.id.humidityTv);

        getValues();
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
                }
                lastUpdatedTv.setText(last_update);
                temperatureTv.setText(temperature);
                humidityTv.setText(humidity);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
