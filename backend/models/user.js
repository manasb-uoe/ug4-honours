/**
 * Created by manas on 15-09-2015.
 */

var mongoose = require("mongoose");
var bcrypt = require("bcrypt-nodejs");
var validator = require('validator');
var async = require("async");
var _ = require("underscore");

var userSchema = new mongoose.Schema({
    name: {
        type: String
    },
    email: {
        type: String,
        index: true
    },
    password: {
        type: String
    },
    createdAt: {
        type: Number
    }
});


/**
 * Instance methods
 */

userSchema.methods.validateInfo = function (options, mainCallback) {

    var settings = _.extend({
        shouldValidateName: true,
        shouldValidateEmail: true,
        shouldValidatePassword: true,
        shouldValidateCreatedAt: true
    }, options);

    var self = this;

    async.series([
        function (callback) {
            if (settings.shouldValidateName) {
                if (!self.name || self.name.trim().length == 0)
                    return mainCallback(new Error("Name is required."));
            }

            return callback(null);
        },
        function (callback) {
            if (settings.shouldValidateEmail) {
                if (!self.email)
                    return mainCallback(new Error("Email is required."));

                if (!validator.isEmail(self.email))
                    return mainCallback(new Error("Invalid email format."));

                self.model('User').findOne({email: self.email}, function (err, email) {
                    if (err)
                        return mainCallback(err);

                    if (email)
                        return mainCallback(new Error("Another user with the same email address already exists."));

                    return callback(null);
                });
            } else {
                return callback(null);
            }
        },
        function (callback) {
            if (settings.shouldValidatePassword) {
                if (!self.password) return mainCallback(new Error("Password is required."));

                self.password = self.password.trim();
                if (!validator.isLength(self.password, 6))
                    return mainCallback(new Error("Password must be at least 6 characters long."));

                if (validator.matches(self.password, /[^a-zA-Z0-9_]/))
                    return mainCallback(new Error("Password cannot contain special characters other than underscore."));
            }

            return callback(null);
        },
        function () {
            if (settings.shouldValidateCreatedAt) {
                if (!self.createdAt)
                    return mainCallback(new Error("Created At is required."));
            }

            return mainCallback();
        }
    ]);


};

userSchema.methods.hashPassword = function (callback) {
    var self = this;

    bcrypt.genSalt(10, function (err, salt) {
        if (err)
            return callback(err);

        bcrypt.hash(self.password, salt, null, function (err, hash) {
            if (err)
                return callback(err);

            self.password = hash;

            return callback(null);
        });
    });
};


module.exports = mongoose.model("User", userSchema);