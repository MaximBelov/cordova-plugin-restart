/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.restart;

/*
 * Imports
 */
import java.util.HashMap;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.util.Log;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Restart plugin implementation for Android
 */
public class RestartPlugin extends CordovaPlugin{


    /*************
     * Constants *
     *************/

    /**
     * Tag for debug log messages
     */
    public static final String TAG = "RestartPlugin";


    /**
     * Map of permission request code to callback context
     */
    protected HashMap<String, CallbackContext> callbackContexts = new HashMap<String, CallbackContext>();


    /************
     * Variables *
     *************/

    /**
     * Singleton class instance
     */
    public static RestartPlugin instance = null;

    boolean debugEnabled = false;


    /**
     * Current Cordova callback context (on this thread)
     */
    protected CallbackContext currentContext;

    protected Context applicationContext;

    protected SharedPreferences sharedPref;
    protected SharedPreferences.Editor editor;

    /*************
     * Public API
     ************/

    /**
     * Constructor.
     */
    public RestartPlugin() {}

    public static RestartPlugin getInstance(){
        return instance;
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "initialize()");
        instance = this;

        applicationContext = this.cordova.getActivity().getApplicationContext();
        sharedPref = cordova.getActivity().getSharedPreferences(TAG, Activity.MODE_PRIVATE);
        editor = sharedPref.edit();

        super.initialize(cordova, webView);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        currentContext = callbackContext;

        try {
            if (action.equals("enableDebug")){
                debugEnabled = true;
                logDebug("Debug enabled");
                callbackContext.success();
            } else if(action.equals("restart")) {
                this.restart(args);
            } else {
                handleError("Invalid action");
                return false;
            }
        }catch(Exception e ) {
            handleError("Exception occurred: ".concat(e.getMessage()));
            return false;
        }
        return true;
    }

    public void restart(JSONArray args) throws Exception{
        boolean cold = args.getBoolean(0);
        if(cold){
            doColdRestart();
        }else{
            doWarmRestart();
        }
    }

    /************
     * Internals
     ***********/

    public void logDebug(String msg) {
        if(debugEnabled){
            Log.d(TAG, msg);
            executeGlobalJavascript("console.log(\""+TAG+"[native]: "+escapeDoubleQuotes(msg)+"\")");
        }
    }

    public void logInfo(String msg){
        Log.i(TAG, msg);
        if(debugEnabled){
            executeGlobalJavascript("console.info(\""+TAG+"[native]: "+escapeDoubleQuotes(msg)+"\")");
        }
    }

    public void logError(String msg){
        Log.e(TAG, msg);
        if(debugEnabled){
            executeGlobalJavascript("console.error(\""+TAG+"[native]: "+escapeDoubleQuotes(msg)+"\")");
        }
    }

    public String escapeDoubleQuotes(String string){
        String escapedString = string.replace("\"", "\\\"");
        escapedString = escapedString.replace("%22", "\\%22");
        return escapedString;
    }

    /**
     * Handles an error while executing a plugin API method  in the specified context.
     * Calls the registered Javascript plugin error handler callback.
     * @param errorMsg Error message to pass to the JS error handler
     */
    public void handleError(String errorMsg, CallbackContext context){
        try {
            logError(errorMsg);
            context.error(errorMsg);
        } catch (Exception e) {
            logError(e.toString());
        }
    }

    /**
     * Handles an error while executing a plugin API method in the current context.
     * Calls the registered Javascript plugin error handler callback.
     * @param errorMsg Error message to pass to the JS error handler
     */
    public void handleError(String errorMsg) {
        handleError(errorMsg, currentContext);
    }

    /**
     * Handles error during a runtime permissions request.
     * Calls the registered Javascript plugin error handler callback
     * then removes entries associated with the request ID.
     * @param errorMsg Error message to pass to the JS error handler
     * @param requestId The ID of the runtime request
     */
    public void handleError(String errorMsg, int requestId){
        CallbackContext context;
        String sRequestId = String.valueOf(requestId);
        if (callbackContexts.containsKey(sRequestId)) {
            context = callbackContexts.get(sRequestId);
        }else{
            context = currentContext;
        }
        handleError(errorMsg, context);
        clearRequest(requestId);
    }

    protected void clearRequest(int requestId){
        String sRequestId = String.valueOf(requestId);
        if (!callbackContexts.containsKey(sRequestId)) {
            return;
        }
        callbackContexts.remove(sRequestId);
    }

    public void executeGlobalJavascript(final String jsString){
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:" + jsString);
            }
        });
    }

    /**
     * Performs a warm app restart - restarts only Cordova main activity
     */
    protected void doWarmRestart() {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    logInfo("Warm restarting main activity");
                    instance.cordova.getActivity().recreate();
                } catch (Exception ex) {
                    handleError("Unable to warm restart main activity: " + ex.getMessage());
                }
            }
        });
    }

    /**
     * Performs a full cold app restart - restarts application
     * https://stackoverflow.com/a/22345538/777265
     */
    protected void doColdRestart() {
        String baseError = "Unable to cold restart application: ";
        try {
            logInfo("Cold restarting application");
            Context c = applicationContext;
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        //mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        Log.i(TAG,"Killing application for cold restart");
                        //kill the application
                        System.exit(0);
                    } else {
                        handleError(baseError+"StartActivity is null");
                    }
                } else {
                    handleError(baseError+"PackageManager is null");
                }
            } else {
                handleError(baseError+"Context is null");
            }
        } catch (Exception ex) {
            handleError(baseError+ ex.getMessage());
        }
    }
}
