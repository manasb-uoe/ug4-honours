/**
 * Created by manas on 29-09-2015.
 */

var mongoose = require("mongoose");
var helpers = require("../utils/helpers");
var async = require("async");

var serviceSchema = new mongoose.Schema({
    name: {type: String, index: {unique: true}},
    description: String,
    serviceType: String,
    routes: [{
        destination: String,
        points: [{
            latitude: Number,
            longitude: Number
        }],
        stops: [String]
    }]
});

/**
 * Instance methods
 */

serviceSchema.methods.toJSON = function() {
    var obj = this.toObject();

    // delete _id since stopId is used as an id
    delete obj._id;

    // delete version key
    delete obj.__v;

    // delete route and point id
    if (obj.routes) {
        obj.routes.forEach(function (route) {
            delete route._id;

            route.points.forEach(function (point) {
                delete point._id;
            });
        });    
    }

    return obj;
};

/**
 * Static methods
 */

serviceSchema.statics.upsertAll = function (cbA) {

    var Service = mongoose.model("Service");

    Service.find({}, function (err, services) {
        if (err) return cbA(err);

        helpers.getApiJson("/api/v1/services", function (statusCode, servicesJson) {
            if (statusCode != 200) return cbA(Error("HTTP status code not OK (" + statusCode + ")."));

            async.each(
                servicesJson.services,
                function (serviceJson, cbB) {
                    // ensure that keys match schema
                    serviceJson.serviceType = serviceJson.service_type;

                    // only keep required data
                    delete serviceJson.service_type;

                    // if services already exist, simply update them with new data, else create and insert new services
                    if (services.length == 0) {
                        var service = new Service(serviceJson);
                        service.save(function (err) {
                            if (err) return cbB(err);

                            return cbB();
                        });
                    } else {
                        Service.findOneAndUpdate({name: serviceJson.name}, serviceJson, function (err, updatedService) {
                            if (err) return cbB(err);

                            // if there's a service that doesn't already exist in db, add it
                            if (!updatedService) {
                                console.log("Inserting new service: " + serviceJson.name);

                                var service = new Service(serviceJson);
                                service.save(function (err) {
                                    if (err) return cbB(err);

                                    return cbB();
                                });
                            } else {
                                return cbB();
                            }
                        });
                    }
                },
                function (err) {
                    return cbA(err);
                }
            )
        });
    });
};

serviceSchema.statics.findByNameWithDetailedRouteInfo = function (name, callback) {

    var Service = mongoose.model("Service");
    var Stop = mongoose.model("Stop");

    Service.findOne({name: name}, function (err, service) {
        if (!service) return callback(helpers.createErrorMessage(404, "No service with name '" + name +"'"));

        if (err) return callback(helpers.createErrorMessage(500, err.message));

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
                if (err) return callback(helpers.createErrorMessage(500, err.message));

                return callback(null, service);
            }
        );
    });
};

module.exports = mongoose.model('Service', serviceSchema);