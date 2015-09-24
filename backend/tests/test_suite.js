/**
 * Created by manas on 23-09-2015.
 */

var mongoose = require("mongoose");
var User = require("../models/user");
var assert = require("chai").assert;
var util = require("./util");
var server = require("../server");
var supertest = require("supertest");
var authTokenService = require("../services/auth_token");

server.start(server.ApiExecutionModeEnum.TEST); // start server in test execution mode
var api = supertest(server.app);

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

        // clear user collection after every test
        afterEach(function (done) {
            User.remove({}, function (err) {
                if (err) return done(err);
                return done();
            });
        });

        describe("User + Authentication", function () {

            describe("POST api/users", function () {

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

                it("should return bad request error when trying to create user without email", function (done) {
                    api.post("/api/users")
                        .expect("Content-Type", /json/)
                        .send({
                            name: util.user1_sample_data.name
                        })
                        .expect(400)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 400);
                            assert.equal(res.body.error.message, "Email is required.");

                            return done();
                        });
                });

                it("should return bad request error when trying to create user with invalid email " +
                    "format", function (done) {
                    api.post("/api/users")
                        .expect("Content-Type", /json/)
                        .send({
                            name: util.user1_sample_data.name,
                            email: util.user1_sample_data.email + "@@@"
                        })
                        .expect(400)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 400);
                            assert.equal(res.body.error.message, "Invalid email format.");

                            return done();
                        });
                });

                it("should return bad request error when trying to create user without password", function (done) {
                    api.post("/api/users")
                        .expect("Content-Type", /json/)
                        .send({
                            name: util.user1_sample_data.name,
                            email: util.user1_sample_data.email
                        })
                        .expect(400)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 400);
                            assert.equal(res.body.error.message, "Password is required.");

                            return done();
                        });
                });

                it("should return bad request error when trying to create user with invalid password " +
                    "(<6 characters)", function (done) {
                    api.post("/api/users")
                        .expect("Content-Type", /json/)
                        .send({
                            name: util.user1_sample_data.name,
                            email: util.user1_sample_data.email,
                            password: util.user1_sample_data.password.substr(0, 5)
                        })
                        .expect(400)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 400);
                            assert.equal(res.body.error.message, "Password must be at least 6 characters long.");

                            return done();
                        });
                });

                it("should return bad request error when trying to create user with invalid password " +
                    "(includes special characters other than underscore)", function (done) {
                    api.post("/api/users")
                        .expect("Content-Type", /json/)
                        .send({
                            name: util.user1_sample_data.name,
                            email: util.user1_sample_data.email,
                            password: util.user1_sample_data.password + "@#$"
                        })
                        .expect(400)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 400);
                            assert.equal(res.body.error.message, "Password cannot contain special characters other than underscore.");

                            return done();
                        });
                });

                it("should successfully create new user and return auth token if input validation passes",
                    function (done) {
                        api.post("/api/users")
                            .expect("Content-Type", /json/)
                            .send({
                                name: util.user1_sample_data.name,
                                email: util.user1_sample_data.email,
                                password: util.user1_sample_data.password
                            })
                            .expect(200)
                            .end(function (err, res) {
                                if (err) return done(err);

                                assert.equal(res.body.success, true);
                                assert.isDefined(res.body.data.token, true);

                                User.find(function (err, users) {
                                    if (err) return done(err);

                                    // make sure only one user is created
                                    assert.equal(users.length, 1);

                                    var user = users[0];

                                    // make sure that new user's name and password match the provided ones
                                    assert.equal(user.name, util.user1_sample_data.name);
                                    assert.equal(user.email, util.user1_sample_data.email);

                                    // make sure stored password is not the same as the one provided
                                    // (i.e. it should be hashed)
                                    assert.notEqual(user.password, util.user1_sample_data.password);

                                    // make sure createdAt is defined
                                    assert.isDefined(user.createdAt);

                                    return done();
                                });
                            });
                    });

                it("should return bad request error when trying to create new user with existing user's" +
                    " email", function (done) {
                    var user = new User(util.user1_sample_data);

                    user.save(function (err) {
                        if (err) return done(err);

                        api.post("/api/users")
                            .expect("Content-Type", /json/)
                            .send({
                                name: util.user1_sample_data.name,
                                email: util.user1_sample_data.email,
                                password: util.user1_sample_data.password
                            })
                            .expect(400)
                            .end(function (err, res) {
                                if (err) return done(err);

                                assert.equal(res.body.success, false);
                                assert.equal(res.body.error.code, 400);
                                assert.equal(res.body.error.message,
                                    "Another user with the same email address already exists.");

                                return done();
                            });
                    });
                })
            });

            describe("GET api/users", function () {

                it("should return unauthorized error when user is not authenticated", function (done) {
                    api.get("/api/users")
                        .expect("Content-Type", /json/)
                        .expect(401)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 401);

                            return done();
                        });
                });

                it("should return a list of all users when user is authenticated", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        api.get("/api/users")
                            .set({"Authorization": "Bearer " + res.body.data.token})
                            .expect(200)
                            .end(function (err, res) {
                                if (err) return done(err);

                                assert.equal(res.body.success, true);

                                // make sure that the returned details match, and only required fields are returned
                                var users = res.body.data;
                                assert.equal(users.length, 1);
                                assert.equal(users[0].name, util.user1_sample_data.name);
                                assert.equal(users[0].email, util.user1_sample_data.email);
                                assert.isDefined(users[0].createdAt);
                                assert.isUndefined(users[0].password);
                                assert.isDefined(users[0].id);

                                return done();
                            });
                    });
                });
            });

            describe("GET api/users/user_id", function () {

                it("should return unauthorized error when user is not authenticated", function (done) {
                    api.get("/api/users/bla")
                        .expect("Content-Type", /json/)
                        .expect(401)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 401);

                            return done();
                        });
                });

                it("should return not found error when user is authenticated but user_id param is the same as their own user id",
                    function (done) {
                        util.createUser(api, util.user1_sample_data, function (err, res) {
                            if (err) return done(err);

                            api.get("/api/users/bla")
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .expect(404)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, false);
                                    assert.equal(res.body.error.code, 404);

                                    return done();
                                });
                        });
                    });

                it("should return user when user is authenticated and user_id param is the same as their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        // retrieve user id from token
                        authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                            if (err) return done(err);

                            api.get("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .expect(200)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, true);

                                    // make sure that the returned details match, and only required fields are returned
                                    var user = res.body.data;
                                    assert.equal(user.name, util.user1_sample_data.name);
                                    assert.equal(user.email, util.user1_sample_data.email);
                                    assert.isDefined(user.createdAt);
                                    assert.isUndefined(user.password);
                                    assert.isDefined(user.id);

                                    return done();
                                });
                        });
                    });
                });
            });

            describe("PUT api/users/user_id", function () {

                it("should return unauthorized error when user is not authenticated", function (done) {
                    api.put("/api/users/bla")
                        .expect("Content-Type", /json/)
                        .expect(401)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 401);

                            return done();
                        });
                });

                it("should return forbidden error when user is authenticated but user_id param is not the same " +
                    "as their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        api.put("/api/users/bla")
                            .set({"Authorization": "Bearer " + res.body.data.token})
                            .expect(403)
                            .end(function (err, res) {
                                if (err) return done(err);

                                assert.equal(res.body.success, false);
                                assert.equal(res.body.error.code, 403);

                                return done();
                            });
                    });
                });

                it("should return bad request error if new email is of incorrect format, when user is authenticated and user_id param " +
                    "is their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        // retrieve user id from token
                        authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                            if (err) return done(err);

                            api.put("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .send({
                                    email: util.user2_sample_data.email + "@@"
                                })
                                .expect(400)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, false);
                                    assert.equal(res.body.error.code, 400);

                                    return done();
                                });
                        });
                    });
                });

                it("should return bad request error if new email belongs to existing user, when user is authenticated and user_id param " +
                    "is their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        util.createUser(api, util.user2_sample_data, function (err, res) {
                            if (err) return done(err);

                            // retrieve user id from token
                            authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                                if (err) return done(err);

                                api.put("/api/users/" + decoded.id)
                                    .set({"Authorization": "Bearer " + res.body.data.token})
                                    .send({
                                        email: util.user1_sample_data.email
                                    })
                                    .expect(400)
                                    .end(function (err, res) {
                                        if (err) return done(err);

                                        assert.equal(res.body.success, false);
                                        assert.equal(res.body.error.code, 400);

                                        return done();
                                    });
                            });
                        });
                    });
                });

                it("should return bad request error if new password is too short, when user is authenticated and user_id param " +
                    "is their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        // retrieve user id from token
                        authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                            if (err) return done(err);

                            api.put("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .send({
                                    password: util.user2_sample_data.password.substring(0, 5)
                                })
                                .expect(400)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, false);
                                    assert.equal(res.body.error.code, 400);

                                    return done();
                                });
                        });
                    });
                });

                it("should return bad request error if new password contains special characters other than " +
                    "underscore, when user is authenticated and user_id param is their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        // retrieve user id from token
                        authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                            if (err) return done(err);

                            api.put("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .send({
                                    password: util.user2_sample_data.password + "@%#"
                                })
                                .expect(400)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, false);
                                    assert.equal(res.body.error.code, 400);

                                    return done();
                                });
                        });
                    });
                });

                it("should not return bad request error if new email is the same as previous email, when user is" +
                    " authenticated and user_id param is their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        // retrieve user id from token
                        authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                            if (err) return done(err);

                            api.put("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .send({
                                    email: util.user1_sample_data.email
                                })
                                .expect(200)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, true);

                                    return done();
                                });
                        });
                    });
                });

                it("should successfully update user with new details, when user is authenticated and user_id param" +
                    " is their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        var token = res.body.data.token;

                        // retrieve user id from token
                        authTokenService.verifyToken(token, function (err, decoded) {
                            if (err) return done(err);

                            api.put("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .send({
                                    name: util.user2_sample_data.name,
                                    email: util.user2_sample_data.email
                                })
                                .expect(200)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, true);

                                    // retrieve user and make sure user details have been updated
                                    api.get("/api/users/" + decoded.id)
                                        .set({"Authorization": "Bearer " + token})
                                        .end(function (err, res) {
                                            if (err) return done(err);

                                            var user = res.body.data;
                                            assert.equal(user.name, util.user2_sample_data.name);
                                            assert.equal(user.email, util.user2_sample_data.email);

                                            return done();
                                        });
                                });
                        });
                    });
                });
            });

            describe("DELETE api/users/user_id", function () {

                it("should return unauthorized error when user is not authenticated", function (done) {
                    api.delete("/api/users/bla")
                        .expect("Content-Type", /json/)
                        .expect(401)
                        .end(function (err, res) {
                            if (err) return done(err);

                            assert.equal(res.body.success, false);
                            assert.equal(res.body.error.code, 401);

                            return done();
                        });
                });

                it("should return forbidden error when user is authenticated but user_id param is not the same " +
                    "as their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        api.delete("/api/users/bla")
                            .set({"Authorization": "Bearer " + res.body.data.token})
                            .expect(403)
                            .end(function (err, res) {
                                if (err) return done(err);

                                assert.equal(res.body.success, false);
                                assert.equal(res.body.error.code, 403);

                                return done();
                            });
                    });
                });

                it("should successfully delete user when user is authenticated and user_id param is the same as " +
                    "their own user id", function (done) {
                    util.createUser(api, util.user1_sample_data, function (err, res) {
                        if (err) return done(err);

                        // retrieve user id from token
                        authTokenService.verifyToken(res.body.data.token, function (err, decoded) {
                            if (err) return done(err);

                            api.delete("/api/users/" + decoded.id)
                                .set({"Authorization": "Bearer " + res.body.data.token})
                                .expect(200)
                                .end(function (err, res) {
                                    if (err) return done(err);

                                    assert.equal(res.body.success, true);

                                    // make sure that the user has been deleted
                                    User.find(function (err, users) {
                                        if (err) return done(err);

                                        assert.equal(users.length, 0);

                                        return done();
                                    });
                                });
                        });
                    });
                });
            });
        });
    });
});
