/**
 * Created by manas on 30-09-2015.
 */

var express = require("express");
var router = express.Router();
var Stop = require("../models/stop");
var authenticationMiddleware = require("../middleware/authentication");


/**
 * Get nearby stops based on the provided geographical coordinates.
 */

router.get("/stops/nearby", authenticationMiddleware, function (req, res, next) {
    if (!req.query.latitude || !req.query.longitude)
        return res.sendError(400, "Latitude and longitude query params are required.");

    var coords = [req.query.longitude, req.query.latitude];
    var limit = req.query.limit || 25;
    var maxDistance = req.query.max_distance || 3;

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

            res.sendOk(stops);
        });
});

module.exports = router;