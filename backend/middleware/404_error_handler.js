/**
 * Created by manas on 16-09-2015.
 */

module.exports = function (req, res) {
    req.statusCode = 404;
    return res.sendError(404, "The requested URL was not found.");
};