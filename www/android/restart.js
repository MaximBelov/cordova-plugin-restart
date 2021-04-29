/**
 *  Restart plugin
 *
 *  Copyright (c) 2015 Working Edge Ltd.
 *  Copyright (c) 2012 AVANTIC ESTUDIO DE INGENIEROS
 **/
var RestartPlugin = (function(){

    /***********************
     *
     * Internal properties
     *
     *********************/
    var RestartPlugin = {};

    /**
     * Restarts the application.
     * By default, a "warm" restart will be performed in which the main Cordova activity is immediately restarted, causing the Webview instance to be recreated.
     * However, if the `cold` parameter is set to true, then the application will be "cold" restarted, meaning a system exit will be performed, causing the entire application to be restarted.
     * This is useful if you want to fully reset the native application state but will cause the application to briefly disappear and re-appear.
     *
     * Note: There is no successCallback() since if the operation is successful, the application will restart immediately before any success callback can be applied.
     *
     * @param {Function} errorCallback - function to call on failure to retrieve authorisation status.
     * This callback function is passed a single string parameter containing the error message.
     * @param {Boolean} cold - if true the application will be cold restarted. Defaults to false.
     */
    RestartPlugin.restart = function(errorCallback, cold){
        return cordova.exec(
            null,
            errorCallback,
            'RestartPlugin',
            'restart',
            [cold]
        );
    };

    /**
     * Enables debug mode, which logs native debug messages to the native and JS consoles.
     * Debug mode is initially disabled on plugin initialisation.
     *
     * @param {Function} successCallback - The callback which will be called when enabling debug is successful.
     */
    RestartPlugin.enableDebug = function(successCallback) {
      return cordova.exec(
        successCallback,
        null,
        'RestartPlugin',
        'enableDebug',
        []);
    };


  return RestartPlugin;
});
module.exports = new RestartPlugin();
