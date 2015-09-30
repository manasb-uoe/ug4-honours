/**
 * Created by manas on 30-09-2015.
 */

var https = require("https");
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