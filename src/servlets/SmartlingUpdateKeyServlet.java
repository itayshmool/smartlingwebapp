package servlets;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.*;
import com.google.appengine.labs.repackaged.com.google.common.base.Pair;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper;
import service.LangsParser;
import service.SmartlingKeyEntry;
import service.SmartlingRequestApi;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: itay_shmool
 * Date: 10/20/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */


// http://wixsmartlingapp.appspot.com/updatekey?project=editor&key=key3&val=current value&updateval=updated val


public class SmartlingUpdateKeyServlet extends HttpServlet  {


    public ServletContext context;
    public String toplogyJson = "{\n" +
            "    \"editor\": {\n" +
            "        \"projectID\": \"7646e9dda\",\n" +
            "        \"apiKey\": \"cd863b54-0220-4c0f-8a6b-dc207af32f8e\"\n" +
            "    }\n" +
            "\n" +
            "}";

 //   wixsmartlingapp.appspot.com/updatekey?fileUri=/files/messages_en.json&projectId=be7b3bd3f&key=key3&val=this is key 2 updated&updateval=this is new eng val for key 3


    public DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public static final Logger log = Logger.getLogger(SmartlingUpdateKeyServlet.class.getName());

    public static final com.google.appengine.api.taskqueue.Queue queue = QueueFactory.getDefaultQueue();

    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
        context = getServletContext();

    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        // pre-flight request processing
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, x-request, x-requested-with");
    }


    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {


        resp.setHeader("Access-Control-Allow-Origin", "*");

        Enumeration en = req.getParameterNames();
        SmartlingKeyEntry smartlingEntry = getSmartlingEntry(req, resp);

        if (smartlingEntry == null)
        {
            resp.getWriter().println("wrong usage");
            return;

        }

        String responseHtml = "Fail";
        try {
            responseHtml = handleUpdate(smartlingEntry);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


        resp.getWriter().println(responseHtml);


    }

    public SmartlingKeyEntry getSmartlingEntry(HttpServletRequest req, HttpServletResponse resp)
    {

        Enumeration en = req.getParameterNames();


        String project = "";
        String fileuri = "";
        String key = "";
        String val = "";
        String updateval = "";
        String jsonfile = "";

        List<String> allowedParams = Arrays.asList("key","project","updateval","jsonfile");

        while (en.hasMoreElements()) {

            String paramName = (String) en.nextElement();
            if (!allowedParams.contains(paramName))
            {
                try {
                    resp.getWriter().println("Syntax Error !!! parameter:" + paramName + " is not allowed.list of allowed parameters is(" + allowedParams.toString() + ")");
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }

            if (paramName.equals("key")) {
                key = req.getParameter(paramName);
            }

            if (paramName.equals("project")) {
                project = req.getParameter(paramName);
            }

            if (paramName.equals("jsonfile")) {
                jsonfile = req.getParameter(paramName);
            }

            if (paramName.equals("updateval")) {
                updateval = req.getParameter(paramName);
            }
        }


        if (key.isEmpty() || project.isEmpty() || updateval.isEmpty() || jsonfile.isEmpty() )
        {
            log.severe("Key:" + key + ".project:" + project + ".updateval:" + updateval + ".jsonfile=" + jsonfile);

            return null;
        }

        else
        {
            // first - project Id
            // second - api key

            Pair<String,String> smartlingCred = getProjectcredentials(project);

            return new SmartlingKeyEntry(smartlingCred.getFirst(),key,val,updateval,jsonfile,smartlingCred.getSecond());
        }

    }

    public String handleUpdate(SmartlingKeyEntry updateEntry) throws IOException, JSONException {




        List<String> locales = Arrays.asList("de-DE","en-GB","es","en-US","fr-FR","it-IT","ja-JP","ko-KR","pl-PL","pt-PT","ru-RU","tr-TR");

        String currentVal = getCurrentValue(updateEntry);
        if (currentVal == null)
        {
            String error = "Key:"+updateEntry.getKey() + " Not found in ProjectId:" + updateEntry.getProjectId();
            return error;

        }



        // add one entry for saving the values from smartling
        logUpdateSummaryToDB(datastore,updateEntry,locales);


        updateEntry.setEngVal(currentVal);

        String resultSet = "";
        for (int i = 0 ; i < locales.size() ; i++)
        {
           String err =  handleUpdateByLocale(updateEntry,locales.get(i));
           resultSet += err+ "\n\n";
        }


        return resultSet;



    }


        // keep in DB

    public static void logTranslationToDB(DatastoreService datastoreService,SmartlingKeyEntry updateEntry , String currentTranlation) {

        Date date = new Date();
        Entity entry = new Entity("updateEntry");

        entry.setProperty("date", date);
        entry.setProperty("projectId", updateEntry.getProjectId());
        entry.setProperty("fileUri", updateEntry.getFileUri());
        entry.setProperty("locale", updateEntry.getLocale());
        entry.setProperty("key", updateEntry.getKey());

        Text valEnText = new Text(updateEntry.getEngVal());
        entry.setProperty("valEn", valEnText);

        Text valEnUpdatedText = new Text(updateEntry.getUpdatedEngVal());
        entry.setProperty("valEnUpdated", valEnUpdatedText);

        Text translatedText = new Text(currentTranlation);
        entry.setProperty("translatedVal", translatedText);



        datastoreService.put(entry);


    }

    // first is projectId
    // second is apiKey
    public Pair<String,String> getProjectcredentials(String projectName)
    {
               if (projectName.equalsIgnoreCase("editor"))
               {

                   return new Pair<>("7646e9dda","cd863b54-0220-4c0f-8a6b-dc207af32f8e");
               }


        return new Pair<>("be7b3bd3f","1dcf51a4-ee17-401a-bb27-96064ec2f93c");
    }


    public String getCurrentValue(SmartlingKeyEntry updateEntry) throws IOException, JSONException {


        String responseHtml = SmartlingRequestApi.getFileFromSmartling(updateEntry.getFileUri(),updateEntry.getProjectId(),"en",updateEntry.getApiKey());
        if (responseHtml.contains("VALIDATION_ERROR"))
            return responseHtml;

        HashMap<String,Object> result =
                new ObjectMapper().readValue(responseHtml, HashMap.class);

        String currentValInLocale = (String) result.get(updateEntry.getKey());


        if (currentValInLocale == null || currentValInLocale.isEmpty())
        {

            String error = "Key:"+updateEntry.getKey() + " Not found in ProjectId:" + updateEntry.getProjectId();
            log.severe(error);
            return null;
        }

        return currentValInLocale;


    }


    public String handleUpdateByLocale(SmartlingKeyEntry updateEntry,String locale) throws IOException, JSONException {


        updateEntry.setLocale(locale);

        return pushToQueue(datastore,updateEntry);




    }

    public String pushToQueue(DatastoreService datastore, SmartlingKeyEntry smartlingKeyEntry) {

        // url encoding

        String ret = "";

        try {


            com.google.appengine.api.datastore.Transaction txn = datastore.beginTransaction();



            String url = "/updatekeybysinglelocale?";


            url += "key=" + smartlingKeyEntry.getKey() + "&";
            url += "projectId=" + smartlingKeyEntry.getProjectId() + "&";
            url += "apikey=" + smartlingKeyEntry.getApiKey() + "&";
            url += "fileUri=" +smartlingKeyEntry.getFileUri() + "&";
            url += "val=" + URLEncoder.encode(smartlingKeyEntry.getEngVal(), "UTF-8") + "&";
            url += "updateval=" + URLEncoder.encode(smartlingKeyEntry.getUpdatedEngVal(), "UTF-8") + "&";
            url += "locale=" +smartlingKeyEntry.getLocale();


            ret = "push to queue:" + url;

            log.info("push to queue:" + url);
            queue.add(TaskOptions.Builder.withUrl(url).method(TaskOptions.Method.GET));
            txn.commit();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return ret;


    }

    public static void logUpdateSummaryToDB(DatastoreService datastoreService,SmartlingKeyEntry updateEntry ,  List<String> nLocales) {

        Date date = new Date();
        Entity entry = new Entity("updateEntrySummary");

        entry.setProperty("date", date);
        entry.setProperty("projectId", updateEntry.getProjectId());
        entry.setProperty("fileUri", updateEntry.getFileUri());
        entry.setProperty("locales",nLocales.toString().replaceAll("]","").replaceAll("\\[",""));
        entry.setProperty("translatedLocales","");
        entry.setProperty("key", updateEntry.getKey());


        datastoreService.put(entry);


    }






}