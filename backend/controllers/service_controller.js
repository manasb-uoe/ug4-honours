/**
 * Created by manas on 06-10-2015.
 */

var express = require("express");
var async = require("async");
var Service = require("../models/service");
var authenticationMiddleware = require("../middleware/authentication");

var router = express.Router();

/**
 * Get service corresponding to provided service name
 */

router.get("/services/:service_name", authenticationMiddleware, function (req, res) {
    var serviceName = req.params.service_name;

    Service.findOne({name: serviceName}, function (err, service) {
        if (!service) return res.sendError(404, "No service with name '" + serviceName +"'");

        if (err) return res.sendError(500, err.message);

        return res.sendOk(service);
    });
});

module.exports = router;