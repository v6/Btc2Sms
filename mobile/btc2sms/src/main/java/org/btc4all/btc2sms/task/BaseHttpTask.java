package org.btc4all.btc2sms.task;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Replaced all "org.apache.http." with "android.net.http.". 
// I have done this to fix the error thrown by proguard upon compilation for release of our Android client. 
// The error thrown by proguard follows: 
/*
[INFO] Warning: library class android.net.http.AndroidHttpClient extends or implements program class org.apache.http.client.HttpClient
[DEBUG] Note: the configuration refers to the unknown class 'com.android.vending.licensing.ILicensingService'
[DEBUG] Note: there were 1 references to unknown classes.
[DEBUG]       You should check your configuration for typos.
[INFO] Warning: there were 1 instances of library classes depending on program classes.
[INFO]          You must avoid such dependencies, since the program classes will
[INFO]          be processed, while the library classes will remain unchanged.
[INFO] java.io.IOException: Please correct the above warnings first.
[INFO] 	at proguard.Initializer.execute(Initializer.java:321)
[INFO] 	at proguard.ProGuard.initialize(ProGuard.java:211)
[INFO] 	at proguard.ProGuard.execute(ProGuard.java:86)
[INFO] 	at proguard.ProGuard.main(ProGuard.java:492)
*/
import android.net.http.Header;
import android.net.http.HttpEntity;
import android.net.http.HttpResponse;
import android.net.http.client.HttpClient;
import android.net.http.client.entity.UrlEncodedFormEntity;
import android.net.http.client.methods.HttpPost;
import android.net.http.entity.mime.FormBodyPart;
import android.net.http.entity.mime.MultipartEntityBuilder;
import android.net.http.message.BasicNameValuePair;
import org.btc4all.btc2sms.App;
import org.btc4all.btc2sms.JsonUtils;
import org.btc4all.btc2sms.R;
import org.btc4all.btc2sms.XmlUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;

import android.os.AsyncTask;
import android.os.Build;

public class BaseHttpTask extends AsyncTask<String, Void, HttpResponse> {
       
    protected App app;
    protected String url;    
    protected List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();    

    private List<FormBodyPart> formParts;
    protected boolean useMultipartPost = false;    
    protected HttpPost post;
    protected Throwable requestException;
    
    public BaseHttpTask(App app, String url, BasicNameValuePair... paramsArr)
    {
        this.url = url;
        this.app = app;                
        params = new ArrayList<BasicNameValuePair>(Arrays.asList(paramsArr));
        
        params.add(new BasicNameValuePair("version", "" + app.getPackageInfo().versionCode));
    }
    
    public void addParam(String name, String value)
    {
        params.add(new BasicNameValuePair(name, value));
    }    
    
    public void setFormParts(List<FormBodyPart> formParts)
    {
        useMultipartPost = true;
        this.formParts = formParts;
    }                     

    protected HttpPost makeHttpPost() throws Exception
    {
        HttpPost httpPost = new HttpPost(url);
                
        httpPost.setHeader("User-Agent", app.getText(R.string.app_name) + "/" + app.getPackageInfo().versionName + " (Android; SDK "+Build.VERSION.SDK_INT + "; " + Build.MANUFACTURER + "; " + Build.MODEL+")");

        if (useMultipartPost)
        {
            MultipartEntityBuilder entity = MultipartEntityBuilder.create();//HttpMultipartMode.BROWSER_COMPATIBLE);

            Charset charset = Charset.forName("UTF-8");

            for (BasicNameValuePair param : params)
            {
            	entity.addTextBody(param.getName(),param.getValue());
            }

            for (FormBodyPart formPart : formParts)
            {
                entity.addPart(formPart.getName(), formPart.getBody());
            }
            HttpEntity he = entity.build();
            httpPost.setEntity(he); 
        }
        else
        {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
        }        
        
        return httpPost;
    }
    
    protected HttpResponse doInBackground(String... ignored) 
    {    
        try
        {
            post = makeHttpPost();
            
            HttpClient client = app.getHttpClient();
            return client.execute(post);            
        }     
        catch (Throwable ex) 
        {
            requestException = ex;
            
            try
            {
                String message = ex.getMessage();
                // workaround for https://issues.apache.org/jira/browse/HTTPCLIENT-881
                if ((ex instanceof IOException) 
                        && message != null && message.equals("Connection already shutdown"))
                {
                    // app.log("Retrying request");
                    post = makeHttpPost();
                    HttpClient client = app.getHttpClient();
                    
                    return client.execute(post);  
                }
            }
            catch (Throwable ex2)
            {
                requestException = ex2;
            }            
        }   
        
        return null;
    }    
           
    protected String getErrorText(HttpResponse response)    
            throws Exception
    {
        String contentType = getContentType(response);
        String error = null;
        
        if (contentType.startsWith("application/json"))
        {
            JSONObject json = JsonUtils.parseResponse(response);
            error = JsonUtils.getErrorText(json);
        }
        else if (contentType.startsWith("text/xml"))
        {
            Document xml = XmlUtils.parseResponse(response);
            error = XmlUtils.getErrorText(xml);
        }
        
        if (error == null)
        {
            error = "HTTP " + response.getStatusLine().getStatusCode();
        }
        return error;
    }
    
    protected String getContentType(HttpResponse response)
    {
        Header contentTypeHeader = response.getFirstHeader("Content-Type");
        return (contentTypeHeader != null) ? contentTypeHeader.getValue() : "";
    }
    
    @Override
    protected void onPostExecute(HttpResponse response) {
        if (response != null)
        {                
            try
            {
                int statusCode = response.getStatusLine().getStatusCode();                
                
                if (statusCode == 200) 
                {
                    handleResponse(response);
                } 
                else if (statusCode >= 400 && statusCode <= 499)
                {
                    handleErrorResponse(response);
                    handleFailure();
                }
                else
                {
                    throw new Exception("HTTP " + statusCode);
                }
            }
            catch (Throwable ex)
            {
                post.abort();
                handleResponseException(ex);
                handleFailure();
            }
            
            try
            {
                response.getEntity().consumeContent();
            }
            catch (IOException ex)
            {
            }
        }
        else
        {
            handleRequestException(requestException);
            handleFailure();
        }
    }
    
    protected void handleResponse(HttpResponse response) throws Exception
    {
    }
    
    protected void handleErrorResponse(HttpResponse response) throws Exception
    {
    }        
    
    protected void handleFailure()
    {
    }            

    protected void handleRequestException(Throwable ex)
    {       
    }

    protected void handleResponseException(Throwable ex)
    {       
    }    
        
}
