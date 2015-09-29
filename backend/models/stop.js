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

module.exports = mongoose.model("Stop", stopSchema);