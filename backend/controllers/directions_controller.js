/**
 * Created by manas on 11-11-2015.
 */

var express = require("express");
var authenticationMiddleware = require("../middleware/authentication");
var GoogleMapsApi = require("googlemaps");
var polyline = require("polyline");
var config = require("../config");
var helpers = require("../utils/helpers");

var router = express.Router();
var googleMapsApi = new GoogleMapsApi({
    key: config.googleMapsApiKey,
    secure: true
});


/**
 * GET walking directions from provided origin to provided destination.
 */

router.get("/walking-directions", authenticationMiddleware, function (req, res) {

    var origin = req.query.origin;
    var destination = req.query.destination;

    if (!origin || !destination) {
        return res.sendError(400, "origin and destination are required parameters");
    }

    getDirections(origin, destination, "walking", function (err, directions) {
        if (err) {
            return res.sendError(err.statusCode, err.message);
        }

        return res.sendOk(directions);
    });

});


/**
 * Helpers
 */

function getDirections(origin, destination, mode, callback) {
    googleMapsApi.directions({
        origin: origin,
        destination: destination,
        mode: mode
    }, function (err, data) {
        if (err) {
            return callback(helpers.createErrorMessage(500, err.message));
        } else {
            var output = {};

            if (data.routes.length == 0) {
                return callback(helpers.createErrorMessage(404, "No directions found"));
            }

            var leg = data.routes[0].legs[0];

            output.distance = leg.distance.value; // meters
            output.duration = leg.duration.value; // seconds
            output.distanceText = leg.distance.text;
            output.durationText = leg.duration.text;

            output.steps = [];
            leg.steps.forEach(function (step) {
                var outputStep = {
                    distance: step.distance.value,
                    distanceText: step.distance.text,
                    duration: step.duration.value,
                    durationText: step.duration.text,
                    startLocation: {
                        latitude: step.start_location.lat,
                        longitude: step.start_location.lng
                    },
                    endLocation: {
                        latitude: step.end_location.lat,
                        longitude: step.end_location.lng
                    },
                    instruction: step.html_instructions,
                    maneuver: step.maneuver
                };

                outputStep.points = [];

                var decodedPoints = polyline.decode(step.polyline.points);
                decodedPoints.forEach(function (decodedPoint) {
                    outputStep.points.push({
                        latitude: decodedPoint[0],
                        longitude: decodedPoint[1]
                    });
                });

                output.steps.push(outputStep);
            });

            output.overviewPoints = [];

            var decodedOverviewPoints = polyline.decode(data.routes[0].overview_polyline.points);
            decodedOverviewPoints.forEach(function (decodedPoint) {
                output.overviewPoints.push({
                    latitude: decodedPoint[0],
                    longitude: decodedPoint[1]
                });
            });

            return callback(null, output);
        }
    });
}


module.exports = {
    helpers: {
        getDirections: getDirections
    },
    router: router
};