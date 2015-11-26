/**
 * Created by manas on 25-11-2015.
 */

var express = require("express");
var User = require("../models/user");
var Activity = require("../models/activity");
var authenticationMiddleware = require("../middleware/authentication");

var router = express.Router();

router.route("/activities")

    .post(authenticationMiddleware, function (req, res) {
        var activityJson = req.body.activity;

        if (!activityJson) {
            return res.sendError(400, "activity is a required field");
        }

        User.findById(req.decodedPayload.id, function (err, user) {
            if (err) return res.sendError(500, err.message);

            try {
                var activity = JSON.parse(activityJson);
            } catch (e) {
                return res.sendError(400, "Failed to parse activity");
            }

            var activityObj = new Activity(activity);
            var validationErr = activityObj.validateInfo();

            if (validationErr) {
                return res.sendError(400, validationErr.message);
            }

            user.activities.push(activityObj);

            user.save(function (err) {
                if (err) return res.sendError(500, err.message);

                return res.sendOk();
            });
        });
    })

    .get(authenticationMiddleware, function (req, res) {
        User.findById(req.decodedPayload.id, function (err, user) {
            if (err) return res.sendError(500, err.message);

            return res.sendOk(user.activities);
        });
    });

module.exports = router;