/**
 * Created by manas on 23-09-2015.
 */

var mongoose = require("mongoose");
var User = require("../models/user");
var assert = require("chai").assert;
var util = require("./util");
var server = require("../server");
server.start(server.ApiExecutionModeEnum.TEST); // start server in test execution mode
var api = require("supertest")(server.app);

describe("Test suite", function () {

    after(function (done) {
        mongoose.connection.close();
        return done();
    });

    describe("Models", function () {

        // drop database all model tests have been executed
        after(function (done) {
            mongoose.connection.db.dropDatabase();
            return done();
        });

        describe("User", function () {

            var savedUser;

            // create a new user and save to db
            before(function (done) {
                var user = new User(util.user1_sample_data);

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
                assert(savedUser.name, util.user1_sample_data.name);
                assert.typeOf(savedUser.name, "string");
                return done();
            });

            it("should have an email [String]", function (done) {
                assert.equal(savedUser.email, util.user1_sample_data.email);
                assert.typeOf(savedUser.email, "string");
                return done();
            });

            it("should have a password [String]", function (done) {
                assert.equal(savedUser.password, util.user1_sample_data.password);
                assert.typeOf(savedUser.password, "string");
                return done();
            });

            it("should have a created at timestamp [Number]", function (done) {
                assert.equal(savedUser.createdAt, util.user1_sample_data.createdAt);
                assert.typeOf(savedUser.createdAt, "number");
                return done();
            });
        });

    });

    describe("Routes", function () {

        // drop database after all routing tests have been executed
        after(function (done) {
            mongoose.connection.db.dropDatabase();
            return done();
        });

        describe("User + Authentication", function () {

            describe("POST new user", function () {

                it("should return bad request error when trying to create user without name", function (done) {
                    api.post("/api/users")
                        .expect("Content-Type", /json/)
                        .expect(400)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 400);
                            assert.equal(res.body.error.message, "Name is required.");

                            return done();
                        });
                });
            });
        });
    });
});