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

/**
 * Configuration
 */

// define express app
var app = express();

// use morgan to log HTTP requests to the console
app.use(morgan("dev"));

// configure body parser, which will let us get data from a POST request
app.use(bodyParser.urlencoded({extended: true}));
app.use(bodyParser.json());

// connect to db
mongoose.connect(config.database);

// define port for the HTTP server (give priority to PORT environment variable)
var port = process.env.PORT || 4000;

// use custom middleware
app.use(responsesMiddleware);


/**
 * Register API routes. All routes will be prefixed with /api.
 */

app.use("/api", userRoutes);
app.use("/api", authRoutes);


/**
 * Start server
 */

app.listen(port);