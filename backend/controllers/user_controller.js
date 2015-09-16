/**
 * Created by manas on 15-09-2015.
 */

var express = require("express");
var router = express.Router();
var User = require("../models/user");
var async = require("async");

router.route("/users")

    /**
     * POST new user
     */

    .post(function (req, res) {
        var user = new User({
            name: req.body.name,
            email: req.body.email,
            password: req.body.password,
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

                    return res.sendOk();
                })
            });
        });
    })


    /**
     * GET all users
     */

    .get(function (req, res) {
        User.find(function (err, users) {
            if (err) return res.sendError(500, err.message);

            res.sendOk(users);
        });
    });


module.exports = router;