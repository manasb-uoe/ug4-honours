/**
 * Created by manas on 16-09-2015.
 */

var express = require("express");
var router = express.Router();
var User = require("../models/user");
var authTokenService = require("../services/auth_token");
var helpers = require("../utils/helpers");
var config = require("../config");
var userControllerHelpers = require("./user_controller").helpers;


router.post("/authenticate", function (req, res) {
    var email = req.body.email ? req.body.email.trim() : req.body.email;
    var password = req.body.password ? req.body.password.trim() : req.body.password;

    if (!email || !password) return res.sendError(401, "Email and password are required.");

    User.findOneByEmail(email, function (err, user) {
        if (err) return res.sendError(500, err);

        if (!user) return res.sendError(401, "Incorrect email or password.");

        user.comparePassword(password, function (err, match) {
            if (err) return res.sendError(500, err);

            if (!match) return res.sendError(401, "Incorrect email or password.");

            // issue token using user's id as the payload
            authTokenService.issueToken(user.id, function (err, token) {
                if (err) return res.sendError(err.message);

                return res.sendOk({token: token});
            });
        });
    });
});


router.post("/authenticate/google", function (req, res) {
    var idToken = req.body.id_token;

    if (!idToken) return res.sendError(400, "ID token is required");

    // validate token ID using google's tokeninfo endpoint
    helpers.getJson("https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + idToken, function (statusCode, json) {
        if (statusCode == 200) {
            User.findOneByEmail(json.email, function (err, user) {
                if (err) return res.sendError(500, err);

                // If user does not already exist, create one. Else, simply return authentication token.
                if (!user) {
                    userControllerHelpers.createUser(json.name, json.email, config.default_user_password, function (err, user) {
                        if (err) return res.sendError(err.statusCode, err.message);

                        authTokenService.issueToken(user.id, function (err, token) {
                            if (err) return res.sendError(err.message);

                            return res.sendOk({token: token});
                        });
                    });
                } else {
                    user.comparePassword(config.default_user_password, function (err, match) {
                        if (err) return res.sendError(500, err);

                        // if password is not the same as default password, it implies that user was created manually and
                        // not via this endpoint.
                        if (!match) return res.sendError(401, "Another user with the same email address already exists.");

                        authTokenService.issueToken(user.id, function (err, token) {
                            if (err) return res.sendError(err.message);

                            return res.sendOk({token: token});
                        });
                    });
                }
            });
        } else {
            return res.sendError(400, "Invalid ID token");
        }
    });
});

module.exports = router;