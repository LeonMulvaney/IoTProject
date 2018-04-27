package ie.ncirl.x14445618student.bmscontroller;

/**
 * Created by Leon on 27/04/2018.
 */

public class SystemHistoryReading {
    String date;
    String time;
    String temperature;
    String humidity;
    String status;

    public SystemHistoryReading() {
    }

    public SystemHistoryReading(String date, String time, String temperature, String humidity, String status) {
        this.date = date;
        this.time = time;
        this.temperature = temperature;
        this.humidity = humidity;
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
