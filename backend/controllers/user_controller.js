/**
 * Created by manas on 15-09-2015.
 */

var express = require("express");
var router = express.Router();
var User = require("../models/user");
var async = require("async");
var authTokenService = require("../services/auth_token");
var authenticationMiddleware = require("../middleware/authentication");
var ownUserMiddleware = require("../middleware/own_user");

router.route("/users")

    /**
     * POST new user.
     */

    .post(function (req, res) {
        var name = req.body.name ? req.body.name.trim() : undefined;
        var email = req.body.email ? req.body.email.trim() : undefined;
        var password = req.body.password ? req.body.password.trim() : undefined;

        var user = new User({
            name: name,
            email: email,
            password: password,
            createdAt: Date.now()
        });

        // perform validation
        user.validateInfo({}, function (err) {
            if (err) return res.sendError(400, err.message);

            // now that validation has been performed, hash password before saving user
            user.hashPassword(function (err) {
                if (err) return res.sendError(500, err.message);

                user.save(function (err) {
                    if (err) return res.sendError(500, err.message);

                    // finally, issue auth token
                    authTokenService.issueToken(user, function (err, token) {
                        if (err) return res.sendError(err.message);

                        return res.sendOk({token: token});
                    });
                });
            });
        });
    })


    /**
     * GET all users.
     */

    .get(authenticationMiddleware, function (req, res) {
        User.find(function (err, users) {
            if (err) return res.sendError(500, err.message);

            return res.sendOk(users);
        });
    });


router.route("/users/:user_id")

    /**
     * GET user.
     */

    .get(authenticationMiddleware, function (req, res) {
        User.findById(req.params.user_id, function (err, user) {
            if (!user) return res.sendError(404, "No user found with id '" + req.params.user_id +"'");

            if (err) return res.sendError(500, err.message);

            return res.sendOk(user);
        });
    })

    /**
     * PUT updated user info.
     */

    .put(authenticationMiddleware, ownUserMiddleware, function (req, res) {
        var name = req.body.name ? req.body.name.trim() : undefined;
        var email = req.body.email ? req.body.email.trim() : undefined;
        var password = req.body.password ? req.body.password.trim() : undefined;

        User.findById(req.params.user_id, function (err, user) {
            if (!user) return res.sendError(404, "No user found with id '" + req.params.user_id +"'");

            if (err) return res.sendError(500, err.message);

            // only validate email if it is provided and is not the same as the current email address
            var shouldValidateEmail = (email != undefined) && (email !== user.email);

            if (name) user.name = name;
            if (email) user.email = email;
            if (password) user.password = password;

            async.series([
                function (callback) {
                    // perform validation
                    user.validateInfo({
                        shouldValidateEmail: shouldValidateEmail,
                        shouldValidatePassword: password != undefined // only validate password if it is provided
                    }, function (err) {
                        if (err)
                            return res.sendError(400, err.message);

                        return callback(null);
                    });
                },
                function (callback) {
                    // now that validation has been performed, hash password before saving user
                    if (password) {
                        user.hashPassword(function (err) {
                            if (err)
                                return res.sendError(500, err.message);

                            return callback(null);
                        });
                    } else {
                        return callback(null);
                    }
                },
                function () {
                    // finally, save user
                    user.save(function (err) {
                        if (err)
                            return res.sendError(500, err.message);

                        return res.sendOk();
                    });
                }
            ]);
        });
    })


    /**
     * DELETE user.
     */

    .delete(authenticationMiddleware, ownUserMiddleware, function (req, res) {
        User.findByIdAndRemove(req.params.user_id, function (err, user) {
            if (!user)
                return res.sendError(404, "No user found with id '" + req.params.user_id +"'");

            if (err)
                return res.sendError(500, err.message);

            return res.sendOk();
        });
    });

module.exports = router;