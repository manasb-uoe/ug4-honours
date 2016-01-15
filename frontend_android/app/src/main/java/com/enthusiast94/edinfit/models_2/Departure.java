package com.enthusiast94.edinfit.models_2;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

/**
 * Created by manas on 09-01-2016.
 */
@Table(name = "Departures")
public class Departure extends Model {

    @Column private Stop stop; // foreign key
    @Column private String serviceName;
    @Column private String time;
    @Column private String destination;
    @Column private int day;
    @Column private Stop destinationStop;

    public Departure() {
        super();
    }

    public Departure(Stop stop, String serviceName, String time, String destination, int day,
                     Stop destinationStop) {
        this.stop = stop;
        this.serviceName = serviceName;
        // ensure time is formatted properly (eg: 5:01 must become 05:01)
        this.time = time.length() == 4  ? "0" + time : time;
        this.destination = destination;
        this.day = day;
        this.destinationStop = destinationStop;
    }

    public Stop getStop() {
        return stop;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getTime() {
        return time;
    }

    public String getDestination() {
        return destination;
    }

    public int getDay() {
        return day;
    }

    public Stop getDestinationStop() {
        return destinationStop;
    }

    /**
     * Statics
     */

    public static Departure findById(long id) {
        return new Select()
                .from(Departure.class)
                .where("Id = ?", id)
                .executeSingle();
    }
}
