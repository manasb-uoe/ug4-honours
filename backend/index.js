/**
 * Created by manas on 17-09-2015.
 */

var server = require("./server");


/**
 * Main entry point
 */

server.start(server.ApiExecutionModeEnum.DEV, 4000);