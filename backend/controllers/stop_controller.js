/**
 * Created by manas on 30-09-2015.
 */

var express = require("express");
var async = require("async");
var Stop = require("../models/stop");
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
    var maxDistance = req.query.max_distance || 3;
    var limit = req.query.limit || 25;
    var nearestStopsLimit = req.query.nearest_stops_limit || 3;
    var nearestStopsDeparturesLimit = req.query.nearest_stops_departures_limit || -1;
    var onlyIncludeUpcomingDepartures = req.query.only_include_upcoming_departures === "true";

    // we need to convert the distance to radians
    // the radius of Earth is approximately 6371 kilometers
    maxDistance = maxDistance / 6371;

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
                    if (stops.indexOf(stop) < nearestStopsLimit) {
                        async.series([
                            function (callbackA) {
                                if (stop.departures.length == 0) {
                                    stop.updateDepartures(function (err) {
                                        if (err) return callbackA(err);

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
                            stop.filterDepartures(helpers.getTodayApiCode(), nearestStopsDeparturesLimit,
                                onlyIncludeUpcomingDepartures, function () {

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

module.exports = router;