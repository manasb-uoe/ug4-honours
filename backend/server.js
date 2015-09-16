/**
 * Created by manas on 15-09-2015.
 */

var express = require("express");
var morgan = require("morgan");
var mongoose = require("mongoose");
var config = require("./config");
var responsesMiddleware = require("./middleware/responses");

/**
 * Configuration
 */

// define express app
var app = express();

// use morgan to log HTTP requests to the console
app.use(morgan("dev"));

// connect to db
mongoose.connect(config.database);

// define port for the HTTP server (give priority to PORT environment variable)
var port = process.env.PORT || 4000;

// use custom middleware
app.use(responsesMiddleware);


/**
 * Register API routes
 */

// all routes will be prefixed with /api


/**
 * Start server
 */

app.listen(port);