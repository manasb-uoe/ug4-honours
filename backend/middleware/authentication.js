/**
 * Created by manas on 16-09-2015.
 */

var authTokenService = require("../services/auth_token");

module.exports = function (req, res, next) {
    if (!req.headers || !req.headers.authorization) return res.sendError(401, "Authorization header not found.");

    var parts = req.headers.authorization.split(" ");

    if (parts.length != 2)
        return res.sendError(401, "Invalid authorization header format. Format should be: 'Authorization: Bearer [token]'");

    var scheme = parts[0];
    var token = parts[1];

    if (scheme !== "Bearer")
        return res.sendError(401, "Invalid authorization header format. Format should be: 'Authorization: Bearer [token]'");

    authTokenService.verifyToken(token, function (err, decoded) {
        if (err) return res.sendError(401, "Invalid access token.");

        // add decoded payload to req object so that it can be used by other middleware
        req.decodedPayload = decoded;

        next();
    });
};