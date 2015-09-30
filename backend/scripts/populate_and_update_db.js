/**
 * Created by manas on 30-09-2015.
 */

var mongoose = require("mongoose");
var config = require("../config");
var Stop = require("../models/stop");
var Service = require("../models/service");
var async = require("async");


mongoose.connect(config.database.dev);
mongoose.connection.once("open", function () {
    console.log("Started...");
    console.time("\nExecution time: ");

    async.series([
            function (cb) {
                Stop.upsertAll(function (err) {
                    if (err) return cb(err);
                    console.log("Upserted stops.");
                    return cb();
                });
            },
            function (cb) {
                Service.upsertAll(function (err) {
                    if (err) return cb(err);
                    console.log("Upserted services.");
                    return cb();
                });
            }
        ],
        function (err) {
            if (err) throw err;

            console.log("----------------------");
            console.timeEnd("\nExecution time: ");
            process.exit(0);
        });
});