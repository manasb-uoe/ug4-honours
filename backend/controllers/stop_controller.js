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
 * Get nearby stops based on the provided geographical coordinates.
 */

router.get("/stops/nearby", authenticationMiddleware, function (req, res) {
    if (!req.query.latitude || !req.query.longitude)
        return res.sendError(400, "Latitude and longitude query params are required.");

    var coords = [req.query.longitude, req.query.latitude];
    var limit = req.query.limit || 25;
    var time = req.query.time;
    var maxDistance = req.query.max_distance || 3; // km
    // we need to convert the distance to radians the radius of Earth is approximately 6371 kilometers
    maxDistance = maxDistance / 6371; // radians
    var nearDistance = req.query.near_distance || 0.3; // km

    Stop
        .where("location")
        .near({
            center: coords,
            maxDistance: maxDistance,
            spherical: true
        })
        .limit(limit)
        .exec(function (err, stops) {
            if (err) return res.sendError(500, err.message);

            // attach departures only to nearest stops
            async.each(
                stops,
                function (stop, callback) {
                    // add distance away from provided location
                    stop.distanceAway = helpers.getDistanceBetweenPoints(coords, stop.location);

                    if (stop.distanceAway < nearDistance) {
                        async.series([
                            function (callbackA) {
                                if (stop.departures.length == 0) {
                                    stop.updateDepartures(function (err, departures) {
                                        if (err) return callbackA(err);

                                        stop.departures = departures;

                                        return callbackA();
                                    });
                                } else {
                                    return callbackA();
                                }
                            }
                        ], function (err) {
                            if (err) return callback(err);

                            // filter out departures that do not belong to provided day and limit them using
                            // the provided limit
                            stop.filterDepartures(helpers.getDayCode(), time, function () {

                                return callback();
                            });
                        });
                    } else {
                        stop.departures = [];

                        return callback();
                    }
                },
                function (err) {
                    if (err) return res.sendError(500, err.message);

                    return res.sendOk(stops);
                }
            );
        });
});


/**
 * Get list of saved stops for currently authenticated user
 */

router.get("/stops/saved", authenticationMiddleware, function (req, res) {
    var time = req.query.time; 

    User.findById(req.decodedPayload.id, function (err, user) {
        if (err) return res.sendError(500, err.message);

        console.log(user);

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
                        stop.filterDepartures(helpers.getDayCode(), time, function () {
                            if (stop.departures.length == 0) {
                                stop.updateDepartures(function (err, departures) {
                                    if (err) return callback(err);

                                    stop.departures = departures;

                                    return callback();
                                });
                            } else {
                                return callback();
                            }
                        });
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
    var time = req.query.time;

    Stop.findOne({stopId: stopId}, function (err, stop) {
        if (!stop) return res.sendError(404, "No stop with id '" + stopId +"'");

        if (err) return res.sendError(500, err.message);

        // add departures if they don't already exist
        // also filter out departures that do not belong to provided day
        stop.filterDepartures(null, time, function () {
            if (stop.departures.length == 0) {
                stop.updateDepartures(function (err, departures) {
                    if (err) return callbackA(err);

                    stop.departures = departures;

                    return res.sendOk(stop);
                });
            } else {
                return res.sendOk(stop);
            }
        });
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

        console.log(req.decodedPayload.id);
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