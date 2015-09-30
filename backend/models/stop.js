/**
 * Created by manas on 29-09-2015.
 */

var mongoose = require("mongoose");

var stopSchema = new mongoose.Schema({
    id: false,
    stopId: {type: Number, index: {unique:true}},
    name: String,
    direction: String,
    location : {type: [Number] /* [<longitude>, <latitude?] */, index: '2d'},
    service_type: String,
    destinations: [String],
    services: [String]
});

/**
 * Instance methods
 */

stopSchema.methods.toJSON = function() {
    var obj = this.toObject();

    // delete _id since stopId is used as an id
    delete obj._id;

    // delete version key
    delete obj.__v;

    return obj;
};

module.exports = mongoose.model("Stop", stopSchema);