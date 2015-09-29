/**
 * Created by manas on 29-09-2015.
 */

var mongoose = require("mongoose");

var serviceSchema = new mongoose.Schema({
    id: false,
    name: {type: String, index: {unique: true}},
    description: String,
    serviceType: String,
    routes: [{
        destination: String,
        points: [{
            latitude: Number,
            longitude: Number
        }],
        stops: [String]
    }]
});

module.exports = mongoose.model('Service', serviceSchema);