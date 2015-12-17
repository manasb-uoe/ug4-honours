/**
 * Created by manas on 06-10-2015.
 */

var express = require("express");
var async = require("async");
var Service = require("../models/service");
var Stop = require("../models/stop");
var authenticationMiddleware = require("../middleware/authentication");
var helpers = require("../utils/helpers");

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
 * All services are returned if no service names are provided. 
 */

router.get("/services/", authenticationMiddleware, function (req, res) {
    var serviceNames = req.query.services || [];

    async.waterfall([
        function (callback) {
            if (serviceNames.length == 0) {
                Service
                    .find({})
                    .select("name description")
                    .exec(function (err, services) {
                        if (err != null) {
                            return helpers.createFailureMessage(500, err.message);
                        }

                        return callback(null, services);
                    });
            } else {
                Service
                    .where("name")
                    .in(serviceNames)
                    .select("name description")
                    .exec(function (err, services) {
                        if (err != null) {
                            return helpers.createFailureMessage(500, err.message);
                        }

                        return callback(null, services);
                    });            
            }
        },
        function (services, callback) {
            // remove route info 
            services.forEach(function (service) {
                service.routes = undefined;
            });

            return callback(null, services);
        }
    ], function(err, services) {
        if (err != null) {
            return res.sendError(err.statusCode, err.message);
        }

        return res.sendOk(services);
    });
});


module.exports = router;