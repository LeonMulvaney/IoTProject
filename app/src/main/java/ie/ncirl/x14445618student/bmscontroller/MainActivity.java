package ie.ncirl.x14445618student.bmscontroller;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import junit.framework.Test;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Dashboard");
    }

    public void viewControlPanel(View view){
        Intent intent = new Intent(this,ControlPanel.class);
        startActivity(intent);
    }

    public void viewTestConnection(View view){
        Intent intent = new Intent(this, TestConnection.class);
        startActivity(intent);
    }
}
