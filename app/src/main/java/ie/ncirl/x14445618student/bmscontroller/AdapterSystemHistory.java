package ie.ncirl.x14445618student.bmscontroller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

//Android Custom ListView From: http://abhiandroid.com/ui/baseadapter-tutorial-example.html
import java.util.ArrayList;

/**
 * Created by Leon on 27/04/2018.
 */

public class AdapterSystemHistory extends BaseAdapter {
    Context context;
    ArrayList<SystemHistoryReading> readingsList;
    LayoutInflater inflter;

    public AdapterSystemHistory(Context applicationContext, ArrayList<SystemHistoryReading> readingsList) {
        this.context = applicationContext;
        this.readingsList = readingsList;
        inflter = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return readingsList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflter.inflate(R.layout.adapter_system_history_layout, null);

        //Target Views for data population
        ImageView readingImg = view.findViewById(R.id.readingImg);
        TextView date = view.findViewById(R.id.dateTv);
        TextView time = view.findViewById(R.id.timeTv);
        TextView temp = view.findViewById(R.id.tempTv);
        TextView humid = view.findViewById(R.id.humidTv);
        TextView status = view.findViewById(R.id.statusTv);


        // Populate the data from the ArrayList passed from the getValues() Method in the SystemHistory Class
        date.setText(readingsList.get(i).getDate());
        time.setText(readingsList.get(i).getTime());
        temp.setText(readingsList.get(i).getTemperature());
        humid.setText(readingsList.get(i).getHumidity());
        status.setText(readingsList.get(i).getStatus());


        //If the system status contains a specific value, act accordingly (i.e. if the String contains "heating", then set the imageView to display the sun...)
        if(readingsList.get(i).getStatus().contains("Heating")){
            readingImg.setImageResource(R.drawable.heating);
        }

        else if(readingsList.get(i).getStatus().contains("Cooling")){
            readingImg.setImageResource(R.drawable.cooling);
        }

        else{
            readingImg.setImageResource(R.drawable.deadband);
        }

        return view;//Return view object to the SystemHistory class so it can be displayed
    }
}
