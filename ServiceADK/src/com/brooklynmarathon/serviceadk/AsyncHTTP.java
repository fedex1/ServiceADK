/*
* @author RobotGrrl.com (original source: https://github.com/RobotGrrl/ServiceADK)
* @author Yozzo, Ralph (ralph@brooklynmarathon.com)
*/
package com.brooklynmarathon.serviceadk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

//import com.google.android.DemoKit.AsyncHTTP;

public class AsyncHTTP  extends AsyncTask<String, Void, String> {

    private static final String TAG = "AsyncHTTP";
	private Exception exception;

    protected String doInBackground(String... urls) {
        try {
            return AsyncHTTP.connect(urls[0]);
        } catch (Exception e) {
            this.exception = e;
            return null;
        }
    }

    protected void onPostExecute(String feed) {
        // TODO: check this.exception 
        // TODO: do something with the feed
    }
    
    public static String connect(String url)
	{
    	String result = null;
	    HttpClient httpclient = new DefaultHttpClient();

	    // Prepare a request object
	    HttpGet httpget = new HttpGet(url); 

	    // Execute the request
	    HttpResponse response;
	    try {
	        response = httpclient.execute(httpget);
	        // Examine the response status
	        Log.i(TAG,response.getStatusLine().toString());

	        // Get hold of the response entity
	        HttpEntity entity = response.getEntity();
	        // If the response does not enclose an entity, there is no need
	        // to worry about connection release

	        if (entity != null) {

	            // A Simple JSON Response Read
	            InputStream instream = entity.getContent();
	            result= convertStreamToString(instream);
	            // now you have the string representation of the HTML request
	            instream.close();
	        }


	    } catch (Exception e) {
	    	Log.e(TAG, "QQQ: httpget: "+ e);
	    }
	    return result;
	}

	 private static String convertStreamToString(InputStream is) {
		    /*
		     * To convert the InputStream to String we use the BufferedReader.readLine()
		     * method. We iterate until the BufferedReader return null which means
		     * there's no more data to read. Each line will appended to a StringBuilder
		     * and returned as String.
		     */
		    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		    StringBuilder sb = new StringBuilder();

		    String line = null;
		    try {
		        while ((line = reader.readLine()) != null) {
		            sb.append(line + "\n");
		        }
		    } catch (IOException e) {
		        e.printStackTrace();
		    } finally {
		        try {
		            is.close();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		    }
		    return sb.toString();
		}

}

