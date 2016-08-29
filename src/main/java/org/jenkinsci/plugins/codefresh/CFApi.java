/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codefresh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hudson.util.Secret;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;

/**
 *
 * @author antweiss
 */
public class CFApi {

    private SSLSocketFactory sf = null;
    private String httpsUrl = "https://g.codefresh.io/api";
    private Secret cfToken;
    private TrustManager[] trustAllCerts;
    private static final Logger LOGGER = Logger.getLogger(CFApi.class.getName());


    public CFApi(Secret cfToken) throws MalformedURLException, IOException {

        this.cfToken = cfToken;
        trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            this.sf = sc.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(this.sf);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

    }

    public List<CFService> getServices() throws MalformedURLException, IOException
    {
        String serviceUrl = httpsUrl + "/services";
        HttpsURLConnection conn = getConnection(serviceUrl);
        List<CFService> services = new ArrayList<CFService>();
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonArray serviceList = new JsonParser().parse(jsonString).getAsJsonArray();
        for (int i = 0; i < serviceList.size(); i++) {
            JsonObject obj = (JsonObject)serviceList.get(i);
            services.add(new CFService(cfToken, obj.get("name").getAsString(), 
                                                obj.get("_id").getAsString(),
                                                obj.get("repoOwner").getAsString(),
                                                obj.get("repoName").getAsString()));
        }
        return services;
    }

    public String getUser() throws MalformedURLException, IOException
    {
        String userUrl = httpsUrl + "/user";
        HttpsURLConnection conn = getConnection(userUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject user = new JsonParser().parse(jsonString).getAsJsonObject();
        String userName = user.get("userName").getAsString();
        return userName;
    }

    public String startBuild(String serviceId, String branch) throws MalformedURLException, IOException
    {
        String buildUrl = httpsUrl + "/builds/" + serviceId ;
        String buildOptions = "";
        HttpsURLConnection conn = getConnection(buildUrl);
        conn.setRequestMethod("POST");
        
        if (! branch.isEmpty())
        {
            conn.setRequestProperty("Content-Type","application/json");
            JsonObject options = new JsonObject();
            options.addProperty("branch", branch);
            buildOptions = options.toString();
        }
        
        try (OutputStreamWriter outs = new OutputStreamWriter(conn.getOutputStream(),"UTF-8")) {
            outs.write(buildOptions);
            outs.flush();
        }
        catch (Exception e)
        {
            throw e;
        }


        InputStream is = conn.getInputStream();
        return IOUtils.toString(is).replace("\"", "");
    }

    public HttpsURLConnection getConnection(String urlString) throws MalformedURLException, IOException {
        if ( urlString.isEmpty())
        {
            urlString = httpsUrl;
        }
        URL connUrl = new URL(urlString);
        HttpsURLConnection conn = (HttpsURLConnection) connUrl.openConnection();
        conn.setRequestProperty("x-access-token", cfToken.getPlainText());
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        HttpsURLConnection.setFollowRedirects(true);
        conn.setInstanceFollowRedirects(true);
        return conn;
    }

    String getBuildProgress(String buildId) throws IOException {
        String buildUrl = httpsUrl + "/builds/" + buildId;
        HttpsURLConnection conn = getConnection(buildUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject build = new JsonParser().parse(jsonString).getAsJsonObject();
        String progress = build.get("progress_id").getAsString();
        return progress;
    }

    String getProgressStatus(String progressId) throws IOException {
        String progressUrl = httpsUrl + "/progress/" + progressId;
        HttpsURLConnection conn = getConnection(progressUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject progress = new JsonParser().parse(jsonString).getAsJsonObject();
        String status = progress.get("status").getAsString();
        return status;
    }

    String getBuildUrl(String progressId) throws IOException {
        String buildUrl = "https://g.codefresh.io" + "/process/" + progressId;
        return buildUrl;
    }

    String launchService(String serviceId, String repoOwner, String repoName, String branch) throws Exception {
        String launchUrl = httpsUrl + "/runtime/testit";
        String launchOptions = "";
       // URL launchEP = new URL(launchUrl);
        HttpsURLConnection conn = getConnection(launchUrl);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/json");
      
        JsonObject options = new JsonObject();
        
        options.addProperty("repoOwner", repoOwner);
        options.addProperty("repoName", repoName);
        options.addProperty("branch", branch);
      
        launchOptions = options.toString();
        
        try (OutputStreamWriter outs = new OutputStreamWriter(conn.getOutputStream(),"UTF-8")) {
            outs.write(launchOptions);
            outs.flush();
        }
        catch (Exception e)
        {
            throw e;
        }


        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject progress = new JsonParser().parse(jsonString).getAsJsonObject();
        String progressId = progress.get("id").getAsString();
        return progressId;
    }
    
    String launchComposition(String compositionId) throws Exception {
        String launchUrl = httpsUrl + "/compositions/"+compositionId+"/run";
        String launchOptions = "";
        HttpsURLConnection conn = getConnection(launchUrl);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/json");
      
        //JsonObject options = new JsonObject();
        //launchOptions = options.toString();
        
        try (OutputStreamWriter outs = new OutputStreamWriter(conn.getOutputStream(),"UTF-8")) {
            outs.write(launchOptions);
            outs.flush();
        }
        catch (Exception e)
        {
            throw e;
        }


        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject progress = new JsonParser().parse(jsonString).getAsJsonObject();
        String progressId = progress.get("id").getAsString();
        return progressId;
    }

    String getEnvUrl(String progressId) throws IOException {
        String progressUrl = httpsUrl + "/progress/" + progressId;
        HttpsURLConnection conn = getConnection(progressUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject progress = new JsonParser().parse(jsonString).getAsJsonObject();
        JsonObject data = progress.getAsJsonObject("data");
        String envUrl = data.get("testitUrl").getAsString();
        
        return envUrl;
    }
    
    public List<CFComposition> getCompositions() throws MalformedURLException, IOException
    {
        String compositionUrl = httpsUrl + "/compositions";
        HttpsURLConnection conn = getConnection(compositionUrl);
        List<CFComposition> compositions = new ArrayList<CFComposition>();
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonArray compositionList = new JsonParser().parse(jsonString).getAsJsonArray();
        for (int i = 0; i < compositionList.size(); i++) {
            JsonObject obj = (JsonObject)compositionList.get(i);
            compositions.add(new CFComposition(obj.get("name").getAsString(), 
                                                obj.get("_id").getAsString()));
        }
        return compositions;
    }
    
}
