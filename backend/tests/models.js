/**
 * Created by manas on 22-09-2015.
 */

var mongoose = require("mongoose");
var config = require("../config");
var User = require("../models/user");
var assert = require("chai").assert;


describe("Models", function () {

    // connect to test db before running any model tests
    before(function (done) {
        mongoose.connect(config.database.test);
        return done();
    });

    // drop database after all model tests have completed
    after(function (done) {
        mongoose.connection.db.dropDatabase();
        return done();
    });

    describe("User", function () {

        var sampleData = {
            name: "Test Name",
            email: "testemail@test.com",
            password: "123456",
            createdAt: '5000'
        };
        var savedUser;

        // create a new user and save to db
        before(function (done) {
            var user = new User(sampleData);

            user.save(function (err) {
                if (err) return done(err);

                savedUser = User.find(function (err, users) {
                    if (err) return done(err);

                    // make sure that only one user exists in db
                    assert.equal(users.length, 1);

                    savedUser = users[0];

                    return done(err);
                });
            });
        });

        it("should have a name [String]", function (done) {
            assert(savedUser.name, sampleData.name);
            assert.typeOf(savedUser.name, "string");
            return done();
        });

        it("should have an email [String]", function (done) {
            assert.equal(savedUser.email, sampleData.email);
            assert.typeOf(savedUser.email, "string");
            return done();
        });

        it("should have a password [String]", function (done) {
            assert.equal(savedUser.password, sampleData.password);
            assert.typeOf(savedUser.password, "string");
            return done();
        });

        it("should have a created at timestamp [Number]", function (done) {
            assert.equal(savedUser.createdAt, sampleData.createdAt);
            assert.typeOf(savedUser.createdAt, "number");
            return done();
        });
    });

});