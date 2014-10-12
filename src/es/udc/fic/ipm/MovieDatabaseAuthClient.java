package es.udc.fic.ipm;


import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.http.message.BasicNameValuePair;


import java.io.IOException;
import org.apache.http.client.ClientProtocolException;



public class MovieDatabaseAuthClient {

    private SharedPreferences _preferences;

    public MovieDatabaseAuthClient(SharedPreferences preferences) {
        _preferences = preferences;
    }


    public String userLogIn(String user, String pass, String authType) throws Exception {
        String url;
        String username;

        if (user.endsWith(IPMAuthenticator.ACCOUNT_NAME_DEVELOPMENT_SERVER_SUFFIX)) {
            url = "http://10.0.2.2:5000/login";
            username = user.substring(0, user.lastIndexOf(IPMAuthenticator.ACCOUNT_NAME_DEVELOPMENT_SERVER_SUFFIX));
        }
        else {
            url = "http://ipm-movie-database.herokuapp.com/login";
            username = user.substring(0, user.lastIndexOf(IPMAuthenticator.ACCOUNT_NAME_PRODUCTION_SERVER_SUFFIX));
        }

        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(IPMAuthenticator.HTTP_USER_AGENT);
        HttpPost request = new HttpPost(url);
        HttpResponse response = null;
        JSONObject responseObject = null;

        try {
            List<NameValuePair> data = new ArrayList<NameValuePair>(2);
            data.add(new BasicNameValuePair("username", username));
            data.add(new BasicNameValuePair("passwd", pass));
            request.setEntity(new UrlEncodedFormEntity(data));

            response = httpClient.execute(request);
            responseObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            throw new Exception("Network error");
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception("Network error");
        } catch (JSONException e) {
            e.printStackTrace();
            throw new Exception("Network error");
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new Exception(String.format("Server error: %d %s",
                    response.getStatusLine().getStatusCode(),
                    response.getStatusLine().getReasonPhrase()));
        }
        if (responseObject.getString("result").equals("failure")) {
            throw new Exception("Invalid credentials");
        }

        if (response.getLastHeader("Set-Cookie") == null) {
            throw new Exception("Invalid login: " + responseObject.toString());
        }

        String setCookie = response.getLastHeader("Set-Cookie").getValue();
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

}
