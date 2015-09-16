/**
 * Created by manas on 16-09-2015.
 */

var jwt = require("jsonwebtoken");
var config = require("../config");


/**
 * Callback for receiving newly issued token after token generation is complete.
 *
 * @callback issueTokenCallback
 * @param err {Error}
 * @param token {string}
 */

/**
 * Generates a new token using the provided payload and a secret key which
 * will be used to sign the token.
 * @param payload {string}
 * @param callback {issueTokenCallback} The callback to call when token generation is done
 */
module.exports.issueToken = function(payload, callback) {
    if (!config.secret) return callback(new Error("No secret found in config file."), null);

    return callback(null, jwt.sign(payload, config.secret));
};


/**
 * Callback for receiving decoded token after verification is complete.
 *
 * @callback verifyTokenCallback
 * @param err {Error}
 * @param token {string} decoded token payload
 */

/**
 * Verifies that the provided token hasn't been tampered with.
 * @param token {string} The token to verify
 * @param callback {verifyTokenCallback} The callback to call when verification is done
 */
module.exports.verifyToken = function(token, callback) {
    if (!config.secret) return callback(new Error("No secret found in config file."), null);

    return jwt.verify(token, config.secret, {} /* no options */, callback);
};