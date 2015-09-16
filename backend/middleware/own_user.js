/**
 * Created by manas on 16-09-2015.
 */

/**
 * This middleware must be used AFTER authentication middleware since it requires the decoded
 * payload to be added to req object.
 */
module.exports = function (req, res, next) {
    var requestedUserId = req.params.user_id;
    var currentUserId = req.decodedPayload.id;

    if (requestedUserId !== currentUserId) return res.sendError(403, "You are not allowed to do that.");

    return next();
};