/**
 * Created by manas on 30-09-2015.
 */

var express = require("express");
var async = require("async");
var Stop = require("../models/stop");
var User = require("../models/user");
var helpers = require("../utils/helpers");
var authenticationMiddleware = require("../middleware/authentication");

var router = express.Router();

/**
 * Get all stops
 */

router.get("/stops", function (req, res) {
    Stop
        .find({})
        .select("-serviceType -departures")
        .exec(function (err, stops) {
            if (err) {
                return res.sendError(500, err.message);
            }

            return res.sendOk(stops);
        });
});


/**
 * Get nearby stops based on the provided geographical coordinates.
 */

router.get("/stops/nearby", authenticationMiddleware, function (req, res) {
    if (!req.query.latitude || !req.query.longitude)
        return res.sendError(400, "Latitude and longitude query params are required.");

    var coords = [req.query.longitude, req.query.latitude];
    var stops_limit = req.query.stops_limit || 25;
    var maxDistance = req.query.max_distance || 3; // km
    // we need to convert the distance to radians the radius of Earth is approximately 6371 kilometers
    maxDistance = maxDistance / 6371; // radians

    Stop
        .where("location").near({
            center: coords,
            maxDistance: maxDistance,
            spherical: true
        })
        .select("-service_type -departures")
        .limit(stops_limit)
        .exec(function (err, stops) {
            if (err) return res.sendError(500, err.message);

            stops.forEach(function (stop) {
                // add distance away from provided location
                stop.distanceAway = helpers.getDistanceBetweenPoints(coords, stop.location);
            });

            return res.sendOk(stops);
        });
});


/**
 * Get list of saved stops for currently authenticated user
 */

router.get("/stops/saved", authenticationMiddleware, function (req, res) {
    var time = req.query.time;
    var departures_limit = req.query.departures_limit;

    User.findById(req.decodedPayload.id, function (err, user) {
        if (err) return res.sendError(500, err.message);

        Stop
            .where("stopId")
            .in(user.savedStops)
            .exec(function (err, stops) {
                if (err) return res.sendError(500, err.message);

                async.each(
                    stops,
                    function (stop, callback) {
                        // add departures if they don't already exist
                        // also filter out departures that are not for today
                        if (stop.departures.length == 0) {
                            stop.updateDepartures(function (err, departures) {
                                if (err) return callback(err);

                                stop.departures = departures;

                                stop.filterDepartures(helpers.getDayCode(), time, departures_limit, function () {
                                    return callback();
                                });
                            });     
                        } else {
                            stop.filterDepartures(helpers.getDayCode(), time, departures_limit, function () {
                                return callback();
                            });
                        }
                    },
                    function (err) {
                        if (err) return res.sendError(500, err.message);

                        return res.sendOk(stops);
                    }
                )
            });
    });
});


/**
 * Get stop corresponding to provided stop_id
 */

router.get("/stops/:stop_id", authenticationMiddleware, function (req, res) {
    var stopId = req.params.stop_id;
    var day = req.query.day;
    var time = req.query.time;
    var departures_limit = req.query.departures_limit;

    Stop.findOne({stopId: stopId}, function (err, stop) {
        if (!stop) return res.sendError(404, "No stop with id '" + stopId +"'");

        if (err) return res.sendError(500, err.message);

        // add departures if they don't already exist
        // also filter out departures that are not for 
        // provided day
        if (stop.departures.length == 0) {
            stop.updateDepartures(function (err, departures) {
                if (err) return callback(err);

                stop.departures = departures;

                stop.filterDepartures(day, time, departures_limit, function () {
                    return res.sendOk(stop);
                });
            });     
        } else {
            stop.filterDepartures(day, time, departures_limit, function () {
                return res.sendOk(stop);
            });
        }
    });
});


/**
 * Save stop corresponding to provided stop_id
 */

router.post("/stops/:stop_id/save", authenticationMiddleware, function (req, res) {
    saveOrUnsave(req, res, true);
});


/**
 * Remove saved stop corresponding to provided stop_id
 */

router.delete("/stops/:stop_id/save", authenticationMiddleware, function (req, res) {
    saveOrUnsave(req, res, false);
});


/**
 * Helper functions
 */

function saveOrUnsave(req, res, shouldSave) {
    var stopId = req.params.stop_id;

    Stop.findOne({stopId: stopId}, function (err, stop) {
        if (!stop) return res.sendError(404, "No stop with id '" + stopId +"'");

        if (err) return res.sendError(500, err.message);

        User.findById(req.decodedPayload.id, function (err, user) {
            if (err) return res.sendError(500, err.message);

            var index = user.savedStops.indexOf(stopId);

            if (index == -1) {
                if (shouldSave) {
                    user.savedStops.push(stopId);
                }
            } else {
                if (!shouldSave) {
                    user.savedStops.splice(index, 1);
                }
            }

            user.save(function (err) {
                if (err) return res.sendError(500, err.message);

                res.sendOk();
            });
        });

    });
}

module.exports = router;