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

    Service.findByNameWithDetailedRouteInfo(serviceName, function (err, service) {
        if (err) {
            return res.sendError(err.statusCode, err.message);
        } else {
            return res.sendOk(service);
        }
    });
});


/**
 * Get list of services corresponding to provided list of service names, exluding any route information.
 */

router.get("/services/", function (req, res) {
    var serviceNames = req.query.services;

    Service
        .where("name")
        .in(serviceNames)
        .exec(function (err, services) {
            if (err) return res.sendError(500, err.message);

            // remove route info 
            services.forEach(function (service) {
                service.routes = undefined;
            });

            return res.sendOk(services);
        });
});


module.exports = router;