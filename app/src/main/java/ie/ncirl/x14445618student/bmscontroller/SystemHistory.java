package ie.ncirl.x14445618student.bmscontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;

//Android Custom ListView From: http://abhiandroid.com/ui/baseadapter-tutorial-example.html

//Android Firebase From:
//https://www.captechconsulting.com/blogs/firebase-realtime-database-android-tutorial
// https://firebase.google.com/docs/storage/android/start
//https://firebase.google.com/docs/android/setup
//https://stackoverflow.com/questions/39800547/read-data-from-firebase-database

public class SystemHistory extends AppCompatActivity {

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    DatabaseReference currentRoomConditionRef;

    String date;
    String time;
    String temperature;
    String humidity;
    String status;

    ListView systemHistoryLv;

    // Android Populating ListView using ArrayAdapter From: https://stackoverflow.com/questions/5070830/populating-a-listview-using-an-arraylist
    AdapterSystemHistory adapter;

    //Declare Arraylist of Object type SystemHistoryReading to store readings pulled from Firebase
    ArrayList<SystemHistoryReading> readingsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_history);
        setTitle("System History");
        //Add Back Button to Action Bar - From https://stackoverflow.com/questions/12070744/add-back-button-to-action-bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Firebase Declarations
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReferenceFromUrl("https://bmscontroller-bd5b4.firebaseio.com/");
        currentRoomConditionRef = databaseReference.child("systemHistory");

        //Create new list of objects when onCreate method is ran, then pass the list to the custom adapter
        readingsList = new ArrayList<>();
        adapter = new AdapterSystemHistory(this,readingsList);

        //Target listview in Activity via id
        systemHistoryLv = findViewById(R.id.systemHistoryLv);

        getValues(); //Call the get values method which pulls data from Firebase into the Custom ListView
    } //End of OnCreate

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
                //Clear list to prevent duplicate readings
                readingsList.clear();

                //Loop through the snapshot of DB from Firebase - Saving values to Variables
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    date = ds.child("date").getValue().toString();
                    time = ds.child("time").getValue().toString();
                    temperature = ds.child("temperature").getValue().toString() + " °C";
                    humidity = ds.child("humidity").getValue().toString() + " %";
                    status = ds.child("status").getValue().toString();

                    //Parse Variables to Model, then save model to the ArrayList of Objects
                    SystemHistoryReading reading = new SystemHistoryReading(date,time,temperature,humidity,status);
                    readingsList.add(reading); //Add Reading Object to the ArrayList of Objects
                     //Flip Order of ArrayList From: https://stackoverflow.com/questions/10766492/what-is-the-simplest-way-to-reverse-an-arraylist
                }
                Collections.reverse(readingsList);
                systemHistoryLv.setAdapter(adapter);//Once the ArrayList has finished populating, call the custom Adapter and Parse to the ListView in the Activity


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
