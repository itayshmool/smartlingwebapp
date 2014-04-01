package service;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.appengine.repackaged.com.google.api.client.util.Base64;
import com.google.appengine.repackaged.com.google.common.base.Pair;

import javax.mail.NoSuchProviderException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: itays
 * Date: 1/24/13
 * Time: 2:20 PM
 * To change this template use File | servelets.SettingsServlet | File Templates.
 */
public class SmartlingRequestApi {


    public static final String SMARTLING_GET_FILE_URL = "https://api.smartling.com/v1/file/get";
//    private static final String SMARTLING_API_KEY = "d7cc5b45-072d-4182-be6f-e8105275368a";
    private static final String SMARTLING_API_KEY = "1dcf51a4-ee17-401a-bb27-96064ec2f93c";







    public static String getFileFromSmartling(String fileUri , String projectId , String locale) throws IOException {


        String request = SMARTLING_GET_FILE_URL + "?";
        request +=  "apiKey=" + SMARTLING_API_KEY;
        request +=  "&fileUri=" + fileUri;
        request +=  "&projectId=" + projectId;
        if (!locale.equalsIgnoreCase("en"))
            request +=  "&locale=" + locale;


        //apiKey=d7cc5b45-072d-4182-be6f-e8105275368a&fileUri=/files/messages_en.json&projectId=7241ba9c9&locale=es" "https://api.smartling.com/v1/file/get"



        HttpURLConnection connection = prepareConnection(request, "GET");


        String resp = openConnection(connection);

        return resp;


    }

//    public static List<String> parseIssuegetCommentsRespGetIds(String resp) throws JSONException {
//        List<String> idList = new ArrayList<>();
//
//        JSONObject myjson = new JSONObject(resp);
//
//
//        int totalComments = (int)myjson.get("total");
//
//        // no comments on issue
//        if (totalComments == 0)
//            return idList;
//
//        // get list of commits
//        JSONArray the_json_array = myjson.getJSONArray("comments");
//
//        int size = the_json_array.length();
//        for (int i = 0; i < size; i++) {
//            JSONObject another_json_object = the_json_array.getJSONObject(i);
//
//            String id = (String) another_json_object.get("id");
//
//            idList.add(id);
//
//        }
//        return idList;
//
//    }




    // method - GET , POST , PUT
    public static HttpURLConnection prepareConnection(String urlString, String method) throws IOException {


        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(60000);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("charset", "utf-8");

        return connection;

    }



    public static String openConnection(HttpURLConnection connection) throws IOException {

        //Get Response
        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuffer response = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            response.append(line);
            response.append('\r');
        }

        if (connection != null) {
            connection.disconnect();
        }
        rd.close();
        return response.toString();

    }


}



