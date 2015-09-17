/**
 * Created by manas on 15-09-2015.
 */

var express = require("express");
var morgan = require("morgan");
var mongoose = require("mongoose");
var bodyParser = require("body-parser");
var config = require("./config");
var responsesMiddleware = require("./middleware/responses");
var userRoutes = require("./controllers/user_controller");
var authRoutes = require("./controllers/auth_controller");
var errorHandlerMiddleware = require("./middleware/404_error_handler");


// define express app
var app = express();

var ApiExecutionModeEnum = Object.freeze({
    DEV: "dev",
    TEST: "test"
});

function configure(apiExecutionMode) {
    if (apiExecutionMode !== ApiExecutionModeEnum.DEV && apiExecutionMode !== ApiExecutionModeEnum.TEST) {
        throw new Error("Invalid api execution mode. Allowed values are: " + Object.keys(ApiExecutionModeEnum));
    }

    // use morgan to log HTTP requests to the console
    app.use(morgan("dev"));

    // configure body parser, which will let us get data from a POST request
    app.use(bodyParser.urlencoded({extended: true}));
    app.use(bodyParser.json());

    // connect to db depending on the provided execution mode
    mongoose.connect(apiExecutionMode === ApiExecutionModeEnum.DEV ? config.database.dev : config.database.test);

    // add custom methods for sending API responses to res object
    app.use(responsesMiddleware);

    // Register API routes. All routes will be prefixed with /api.
    app.use("/api", userRoutes);
    app.use("/api", authRoutes);

    // add 404 error handling middleware in order to send custom message when no route matches client's request
    app.use(errorHandlerMiddleware);

}

function start(apiExecutionMode, port) {
    configure(apiExecutionMode);
    app.listen(port);
}

module.exports = {
    start: start,
    ApiExecutionModeEnum: ApiExecutionModeEnum
};