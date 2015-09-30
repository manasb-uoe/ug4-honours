/**
 * Created by manas on 29-09-2015.
 */

var mongoose = require("mongoose");
var helpers = require("../utils/helpers");
var async = require("async");

var stopSchema = new mongoose.Schema({
    id: false,
    stopId: {type: Number, index: {unique:true}},
    name: String,
    direction: String,
    location : {type: [Number] /* [<longitude>, <latitude?] */, index: '2d'},
    service_type: String,
    destinations: [String],
    services: [String]
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
                        Stop.findOneAndUpdate({stopId: stopJson.stopId}, stopJson, function (err) {
                            if (err) return cbB(err);

                            return cbB();
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
 * Instance methods
 */

stopSchema.methods.toJSON = function() {
    var obj = this.toObject();

    // delete _id since stopId is used as an id
    delete obj._id;

    // delete version key
    delete obj.__v;

    return obj;
};

module.exports = mongoose.model("Stop", stopSchema);