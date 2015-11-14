/**
 * Created by manas on 29-09-2015.
 */

var mongoose = require("mongoose");
var helpers = require("../utils/helpers");
var async = require("async");
var moment = require("moment");

var stopSchema = new mongoose.Schema({
    id: false,
    stopId: {type: Number, index: {unique:true}},
    name: String,
    direction: String,
    location : {type: [Number] /* [<longitude>, <latitude?] */, index: '2d'},
    service_type: String,
    destinations: [String],
    services: [String],
    departures: [{
        serviceName: String,
        time: String,
        destination: String,
        day: Number,
        validFrom: Number
    }],
    distanceAway: Number
});


/**
 * Static methods
 */

stopSchema.statics.upsertAll = function (cbA) {

    var Stop = mongoose.model("Stop");

    Stop.find({}, function (err, stops) {
        if (err) return cbA(err);

        helpers.getApiJson("/api/v1/stops", function (statusCode, stopsJson) {
            if (statusCode != 200) return cbA(new Error("HTTP status code not OK (" + statusCode + ")."));

            async.each(
                stopsJson.stops,
                function (stopJson, cbB) {
                    // ensure that keys match schema
                    stopJson.stopId = stopJson.stop_id;
                    stopJson.location = [stopJson.longitude, stopJson.latitude];

                    // only keep required data
                    delete stopJson.atco_code;
                    delete stopJson.identifier;
                    delete stopJson.orientation;
                    delete stopJson.locality;
                    delete stopJson.latitude;
                    delete stopJson.longitude;

                    // if stops already exist, simply update them with new data, else create and insert new stops
                    if (stops.length == 0) {
                        var stop = new Stop(stopJson);
                        stop.save(function (err) {
                            if (err) return cbB(err);

                            return cbB();
                        });
                    } else {
                        Stop.findOneAndUpdate({stopId: stopJson.stopId}, stopJson, function (err, updatedStop) {
                            if (err) return cbB(err);

                            if (updatedStop) {
                               if (updatedStop.departures.length > 0) {
                                   updatedStop.updateDepartures(function (err) {
                                       if (err) return cbB(err);

                                       return cbB();
                                   });
                               } else {
                                   return cbB();
                               }
                            } else {
                                // if there's a stop that doesn't already exist in db, add it
                                console.log("Inserting new stop: " + stopJson.stopId);

                                var stop = new Stop(stopJson);
                                stop.save(function (err) {
                                    if (err) return cbB(err);

                                    return cbB();
                                });
                            }
                        });
                    }
                },
                function (err) {
                    return cbA(err);
                }
            );
        });
    });
};


/**
 * Get stops corresponding to the provided stop ids along with filtered departures
 */

stopSchema.statics.findByIdWithDepartures = function (stopIds, day, time, callback) {

    var Stop = mongoose.model("Stop");

    Stop
        .where("stopId")
        .in(stopIds)
        .exec(function (err, stops) {
            if (err) return callback(helpers.createErrorMessage(500, err.message));

            async.each(
                stops,
                function (stop, callbackA) {
                    // add departures if they don't already exist
                    // also filter out departures that are not for
                    // provided day
                    if (stop.departures.length == 0) {
                        stop.updateDepartures(function (err, departures) {
                            if (err) return callbackA(err);

                            stop.departures = departures;

                            stop.filterDepartures(day, time, function () {
                                return callbackA(null);
                            });
                        });
                    } else {
                        stop.filterDepartures(day, time, function () {
                            return callbackA(null);
                        });
                    }
                },
                function (err) {
                    if (err) return callback(helpers.createErrorMessage(500, err.message));

                    return callback(null, stops);
                }
            )
        });
};


/**
 * Instance methods
 */

stopSchema.methods.toJSON = function() {
    var obj = this.toObject();

    // delete _id since stopId is used as an id
    delete obj._id;

    // delete version key
    delete obj.__v;

    // delete departure id
    obj.departures.forEach(function (departure) {
        delete departure._id;
    });

    return obj;
};

stopSchema.methods.updateDepartures = function (callback) {
    var self = this;

    helpers.getApiJson("/api/v1/timetables/" + this.stopId, function (statusCode, timetableJson) {
        if (statusCode != 200) return callback(Error("HTTP status code not OK (" + statusCode + ")."));

        // ensure that keys match schema and only keep required data
        timetableJson.departures.forEach(function (departure) {
            departure.serviceName = departure.service_name;
            departure.validFrom = departure.valid_from;

            delete departure.service_name;
            delete departure.valid_from;
            delete departure.note_id;
        });
        delete timetableJson.stop_id;
        delete timetableJson.stop_name;

        self.update({departures: timetableJson.departures}, function (err) {
            if (err) return callback(err);

            // need to query again in order to return the updated departures
            // we could simply have returned timetableJson.departures, but there could be some pre processing
            // of departures before saving, so we must return the actual departures subdocument
            mongoose.model("Stop").findOne({stopId: self.stopId}, function (err, stop) {
                if (err) return callback(err);

                return callback(null, stop.departures);
            });
        });
    });
};

stopSchema.methods.filterDepartures = function (day, time, callback) {
    time = time ? moment(time, "HH:mm") : undefined;

    this.departures = this.departures.filter(function (departure) {
        var doesDayMatch = true;
        var isUpcoming = true;

        if (day) {
            doesDayMatch = departure.day == day;
        }

        if (time) {
            var due = moment(departure.time, "HH:mm");
            isUpcoming = due >= time;
        }

        return doesDayMatch && isUpcoming;
    });

    return callback();
};

module.exports = mongoose.model("Stop", stopSchema);