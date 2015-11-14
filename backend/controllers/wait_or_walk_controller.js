/**
 * Created by manas on 11-11-2015.
 */

var express = require("express");
var async = require("async");
var Stop = require("../models/stop");
var Service = require("../models/service");
var helpers = require("../utils/helpers");
var authenticationMiddleware = require("../middleware/authentication");
var moment = require("moment");
var directionHelpers = require("../controllers/directions_controller").helpers;

var router = express.Router();

router.get("/wait-or-walk-suggestions", authenticationMiddleware, function (req, res) {

    var originStopId = req.query.origin_stop;
    var serviceName = req.query.service;
    var destinationStopId = req.query.destination_stop;
    var routeDestination = req.query.route;
    var userLocation = req.query.user_location; // format: lat,lng
    var maxNumberOfStopsToSkip = req.query.max_number_of_stops_to_skip;

    if (originStopId == null) {
        return res.sendError(400, "'origin_stop' is a required parameter");
    }

    if (serviceName == null) {
        return res.sendError(400, "'service' is a required parameter");
    }

    if (destinationStopId == null) {
        return res.sendError(400, "'destination_stop' is a required parameter");
    }

    if (routeDestination == null) {
        return res.sendError(400, "'route' is a required parameter");
    }

    if (userLocation == null) {
        return res.sendError(400, "'user_location' is a required parameter");
    }

    if (maxNumberOfStopsToSkip == null) {
        return res.sendError(400, "'max_number_of_stops_to_skip' is a required parameter");
    }

    async.waterfall([
            function (callback) {
                // retrieve service corresponding to the provided service name
                Service.findByNameWithDetailedRouteInfo(serviceName, function (err, service) {
                    if (err != null) {
                        return callback(err);
                    } else {
                        return callback(null, service);
                    }
                });
            },
            function (service, callback) {
                // retrieve route corresponding to the provided route destination
                var route = null;

                for (var i=0; i<service.routes.length; i++) {
                    var currentRoute = service.routes[i];
                    if (currentRoute.destination == routeDestination) {
                        route = currentRoute;
                        break;
                    }
                }

                if (route == null) {
                    return callback(helpers.createErrorMessage(
                        404, "No route found with destination '" + routeDestination +"'"));
                } else {
                    return callback(null, route)
                }
            },
            function (route, callback) {
                // Find a list of potential walking destination stop ids. Note that the first element in
                // this list is the origin stop since it will be needed when the final result is to WAIT.
                var stopIds = [];

                for (var i=0; i<route.stops.length; i++) {
                    var currentStop = route.stops[i];

                    if (currentStop.stopId == originStopId) {
                        var counter = 0;

                        while (counter <= maxNumberOfStopsToSkip) {
                            var index = i + counter;
                            if (index < route.stops.length) {
                                stopIds.push(route.stops[index].stopId);
                            }

                            if (route.stops[index].stopId ==  destinationStopId) {
                                break;
                            }

                            counter++;
                        }

                        break;
                    }
                }

                return callback(null, stopIds);
            },
            function (stopIds, callback) {
                // retrieve stops with departures (for current day and time) for the corresponding stop ids
                var currentTime = moment();
                var currentTimeUnix = currentTime.unix();
                var currentTime24h = currentTime.format("HH:mm");

                Stop.findByIdWithDepartures(stopIds, helpers.getDayCode(), currentTime24h, function (err, stops) {

                    if (err != null) {
                        return callback(err);
                    } else {
                        return callback(null, currentTimeUnix, stops);
                    }
                });
            },
            function (currentTimeUnix, stops, callback) {
                var shouldStopProcessing = false;
                var waitOrWalkSuggestions = [];

                async.eachSeries(
                    stops,
                    function (stop, callbackA) {
                        console.log(stop.name);
                        var stopIndex = stops.indexOf(stop);

                        if (shouldStopProcessing || stopIndex == 0 /* exclude origin stop */) {
                            return callbackA(null);
                        }

                        // retrieve upcoming departure corresponding to the provided service name
                        var upcomingDeparture = null;

                        for (var i=0; i<stop.departures.length; i++) {
                            var departure = stop.departures[i];

                            if (departure.serviceName == serviceName) {
                                upcomingDeparture = departure;
                                break;
                            }
                        }

                        if (upcomingDeparture == null) {
                            return callbackA(helpers.createErrorMessage(
                                    404,
                                    "No upcoming departures for service '" + serviceName + "' at '" + stop.name + "'")
                            );
                        }

                        // retrieve walking directions from user's location to stop's location, along with
                        // time remaining for upcoming departure
                        var remainingTimeForDepartureSeconds =
                            moment(upcomingDeparture.time, "HH:mm").unix() - currentTimeUnix;

                        console.log("upcoming: " + upcomingDeparture.time);
                        console.log(remainingTimeForDepartureSeconds / (60) + " mins");

                        directionHelpers.getDirections(userLocation, stop.location[1] +
                            "," + stop.location[0], "walking", function (err, walkingDirections) {

                            if (err != null) {
                                return callbackA(err);
                            }

                            console.log(walkingDirections.durationText + "\n");

                            // compute wait or walk suggestion
                            if (walkingDirections.duration <= remainingTimeForDepartureSeconds) {
                                waitOrWalkSuggestions.push({
                                    stop: stop,
                                    upcomingDeparture: upcomingDeparture,
                                    remainingTimeMillis: remainingTimeForDepartureSeconds * 1000,
                                    type: "WALK",
                                    walkingDirections: walkingDirections
                                });

                                return callbackA(null);
                            } else {
                                if (stopIndex != 1) {
                                    shouldStopProcessing = true;
                                    console.log("\nshould stop processing now.\n");
                                    return callbackA(null);
                                }

                                var originStop = stops[0];

                                var upcomingDepartureAtOriginStop = null;

                                for (var i4=0; i<originStop.departures.length; i4++) {
                                    var departure = originStop.departures[i4];

                                    if (departure.serviceName == serviceName) {
                                        upcomingDepartureAtOriginStop = departure;
                                        break;
                                    }
                                }

                                if (upcomingDepartureAtOriginStop == null) {
                                    return callbackA(helpers.createErrorMessage(
                                            404,
                                            "No upcoming departures for service '" + serviceName + "' at '" + stop.name + "'")
                                    );
                                }

                                var remainingTimeSecondsForUpcomingDepartureAtOriginStop =
                                    moment(upcomingDepartureAtOriginStop.time, "HH:mm").unix() - currentTimeUnix;

                                directionHelpers.getDirections(userLocation,
                                    originStop.location[1] + "," + originStop.location[0],
                                    "walking", function (err, walkingDirections) {

                                        if (err != null) {
                                            return callbackA(err);
                                        }

                                        waitOrWalkSuggestions.push({
                                            stop: originStop,
                                            upcomingDeparture: upcomingDepartureAtOriginStop,
                                            remainingTimeMillis:
                                            remainingTimeSecondsForUpcomingDepartureAtOriginStop * 1000,
                                            type: "WAIT",
                                            walkingDirections: walkingDirections
                                        });

                                        shouldStopProcessing = true;
                                        console.log("\nshould stop processing now.\n");

                                        return callbackA(null);
                                    });
                            }
                        });
                    },
                    function (err) {
                        if (err != null) {
                            return callback(err);
                        }

                        console.log(waitOrWalkSuggestions.length);
                        return callback(null, waitOrWalkSuggestions);
                    }
                );
            }
        ],
        function (err, waitOrWalkSuggestions) {
            if (err != null) {
                return res.sendError(err.statusCode, err.message);
            }

            return res.sendOk(waitOrWalkSuggestions);
        }
    );
});


module.exports = router;