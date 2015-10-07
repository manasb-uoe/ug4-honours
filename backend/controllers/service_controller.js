/**
 * Created by manas on 06-10-2015.
 */

var express = require("express");
var async = require("async");
var Service = require("../models/service");
var Stop = require("../models/stop");
var authenticationMiddleware = require("../middleware/authentication");

var router = express.Router();

/**
 * Get service corresponding to provided service name, replacing route stop ids with stop data.
 */

router.get("/services/:service_name", authenticationMiddleware, function (req, res) {
    var serviceName = req.params.service_name;

    Service.findOne({name: serviceName}, function (err, service) {
        if (!service) return res.sendError(404, "No service with name '" + serviceName +"'");

        if (err) return res.sendError(500, err.message);

        async.each(
            service.routes,
            function (route, callbackA) {
                async.each(
                    route.stops,
                    function (stopId, callbackB) {
                        Stop.findOne({stopId: stopId}, function (err, stop) {
                            if (err) return callbackB(err);

                            var index = route.stops.indexOf(stop.stopId);

                            route.stops[index] = {stopId: stop.stopId, name: stop.name, location: stop.location};

                            return callbackB();
                        });
                    },
                    function (err) {
                        if (err) return callbackA(err);

                        return callbackA();
                    }
                )
            },
            function (err) {
                if (err) return res.sendError(500, err.message);

                return res.sendOk(service);
            }
        );
    });
});

module.exports = router;