/**
 * Created by manas on 25-11-2015.
 */

var mongoose = require("mongoose");
var _ = require("underscore");

var activitySchema = new mongoose.Schema({
    type: String,
    start: Number, /* unix timestamp in millis */
    end: Number, /* unix timestamp in millis */
    distance: Number, /* in meters */
    averageSpeed: Number, /* in meters/second */
    points: [{
        latitude: Number,
        longitude: Number,
        timestamp: Number /* unix timestamp in millis */,
        speed: Number /* in meters/second */
    }]
});

var ActivityTypeEnum = Object.freeze({
    WAIT_OR_WALK: "WAIT_OR_WALK"
});


/**
 * Instance methods
 */

activitySchema.methods.toJSON = function() {
    var obj = this.toObject();

    // replace _id key with id
    obj.id = obj._id;
    delete obj._id;

    // delete _id from each of the points
    obj.points.forEach(function (point) {
        delete point._id;
    });

    return obj;
};

activitySchema.methods.validateInfo = function () {
    var possibleActivityTypes = _.map(Object.keys(ActivityTypeEnum), function (key) {
        return ActivityTypeEnum[key];
    });

    if (!"type" in this || !"start" in this ||
        !"end" in this || !"points" in this) {
        return new Error("Invalid activity");
    }

    if (possibleActivityTypes.indexOf(this.type) == -1) {
        return new Error("activity type can only be one of: " + possibleActivityTypes.toString());
    }

    for (var i=0; i<this.points.length; i++) {
        var point = this.points[i];

        if (!"latitude" in point || !"longitude" in point ||
            !"timestamp" in point) {
            return new Error("Invalid activity");
        }
    }

    return null;
};

module.exports = mongoose.model("Activity", activitySchema);