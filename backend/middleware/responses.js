/**
 * Created by manas on 15-09-2015.
 */

module.exports = function (req, res, next) {

    res.sendOk = function (data) {
        res.statusCode = 200;
        res.json({success: true, data: data});
    };

    res.sendError = function (statusCode, errorMessage) {
        res.statusCode = statusCode;
        res.json({
                success: false,
                error: {
                    code: statusCode,
                    message: errorMessage
                }}
        );
    };

    next();
};