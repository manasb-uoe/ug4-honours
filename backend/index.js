/**
 * Created by manas on 23-09-2015.
 */

var server = require("./server");

/**
 * Main entry point for running API in development mode
 */

server.start(server.ApiExecutionModeEnum.DEV);