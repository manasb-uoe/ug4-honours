/**
 * Created by manas on 30-09-2015.
 */

var https = require("https");
var moment = require("moment");
var config = require("../config");

/**
 * API GET request helper
 */

module.exports.getApiJson = function(path, onResult) {
    var options = {
        host: "tfe-opendata.com",
        headers: {Authorization: "Token " + config.tfeApiKey}
    };

    if (!path) throw new Error("path is required.");

    options.path = path;

    https.get(options, function (res) {
        var output = "";

        res.on("data", function (chunk) {
            output += chunk;
        });

        res.on("end", function () {
            var jsonOutput = JSON.parse(output);
            onResult(res.statusCode, jsonOutput);
        });
    });
};

module.exports.getDayCode = function (day) {
    // set day to today if it is undefined
    day = day || moment().format("dddd").toLowerCase();

    var weekdays = ["monday", "tuesday", "wednesday", "thursday", "friday"];

    if (weekdays.indexOf(day) > -1) {
        return 0;
    } else if (day == "saturday") {
        return 5;
    } else {
        return 6;
    }
};

module.exports.getDistanceBetweenPoints = function(pt1, pt2) {
    var rad = function (x) {
        return x * Math.PI / 180;
    };

    var R = 6378137; // Earth’s mean radius in meter
    var dLat = rad(pt2[1] - pt1[1]);
    var dLong = rad(pt2[0] - pt1[0]);
    var a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(rad(pt1[1])) * Math.cos(rad(pt2[1])) *
        Math.sin(dLong / 2) * Math.sin(dLong / 2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    var d = R * c;

    return d/1000; // returns the distance in km
};