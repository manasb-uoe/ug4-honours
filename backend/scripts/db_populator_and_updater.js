/**
 * Created by manas on 29-09-2015.
 */

var https = require("https");
var config = require("../config");
var mongoose = require("mongoose");
var async = require("async");
var Stop = require("../models/stop");
var Service = require("../models/service");


/**
 * HTTPS GET request helper
 */

var options = {
    host: "tfe-opendata.com",
    headers: {Authorization: "Token " + config.tfeApiKey}
};

function getJSON(path, onResult) {
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
}


/**
 * DB collection population/update methods
 */

function upsertStops(cbA) {
    console.log("Upserting Stops...");

    Stop.find({}, function (err, stops) {
        if (err) throw err;

        getJSON("/api/v1/stops", function (statusCode, stopsJson) {
            if (statusCode != 200) throw new Error("HTTP status code not OK (" + statusCode + ").");

            async.each(
                stopsJson.stops,
                function (stopJson, cbB) {
                    // ensure that keys match schema
                    stopJson.stopId = stopJson.stop_id;
                    stopJson.location = [0, 0];

                    // only keep required data
                    delete stopJson.atco_code;
                    delete stopJson.identifier;
                    delete stopJson.orientation;
                    delete stopJson.locality;
                    delete stopJson.latitude;
                    delete stopJson.longitude;

                    // if stops already exist, simply update them with new data, else create and insert new stops
                    if (stops.length == 0) {
                        var stop = new Stop(stopJson);
                        stop.save(function (err) {
                            if (err) throw err;

                            return cbB();
                        });
                    } else {
                        Stop.findOneAndUpdate({stopId: stopJson.stopId}, stopJson, function (err) {
                            if (err) throw err;

                            return cbB();
                        });
                    }
                },
                function () {
                    console.log("DONE\n");
                    return cbA();
                }
            );
        });
    });
}

function upsertServices(cbA) {
    console.log("Upserting Services...");

    Service.find({}, function (err, services) {
        if (err) throw err;

        getJSON("/api/v1/services", function (statusCode, servicesJson) {
            if (statusCode != 200) throw new Error("HTTP status code not OK (" + statusCode + ").");

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
                            if (err) throw err;

                            cbB();
                        });
                    } else {
                        Service.findOneAndUpdate({name: serviceJson.name}, serviceJson, function (err) {
                            if (err) throw err;

                            cbB();
                        });
                    }
                },
                function () {
                    console.log("DONE\n");
                    cbA();
                }
            )
        });
    });
}


/**
 * Script entry point
 */

mongoose.connect(config.database.test);
mongoose.connection.once("open", function () {
    console.time("\nExecution time: ");

    async.series([
        upsertStops,
        upsertServices,
        function () {
            console.log("----------------------");
            console.timeEnd("\nExecution time: ");
            process.exit(0);
        }
    ]);
});

