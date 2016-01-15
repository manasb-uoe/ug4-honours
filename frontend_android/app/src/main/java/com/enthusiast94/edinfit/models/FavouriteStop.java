package com.enthusiast94.edinfit.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.List;

/**
 * Created by manas on 09-01-2016.
 */
@Table(name = "FavouriteStops")
public class FavouriteStop extends Model {

    @Column Stop stop;
    @Column User user;

    public FavouriteStop() {
        super();
    }

    public FavouriteStop(Stop stop, User user) {
        this.stop = stop;
        this.user = user;
    }

    public Stop getStop() {
        return stop;
    }

    public User getUser() {
        return user;
    }

    /**
     * Statics
     */

    public static List<FavouriteStop> getFavouriteStops(User user) {
        return new Select()
                .from(FavouriteStop.class)
                .where("user = ?", user.getId())
                .execute();
    }

    public static FavouriteStop find(Stop stop, User user) {
        return new Select()
                .from(FavouriteStop.class)
                .where("stop = ? AND user = ?", stop.getId(), user.getId())
                .executeSingle();
    }
}
