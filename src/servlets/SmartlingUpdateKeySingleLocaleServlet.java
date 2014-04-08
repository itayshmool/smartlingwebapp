package servlets;

import com.google.appengine.api.datastore.*;
import com.google.appengine.labs.repackaged.com.google.common.base.Pair;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper;
import service.ShardedCounter;
import service.SmartlingKeyEntry;
import service.SmartlingRequestApi;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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


public class SmartlingUpdateKeySingleLocaleServlet extends HttpServlet {


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

    public static final Logger log = Logger.getLogger(SmartlingUpdateKeySingleLocaleServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
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

        if (smartlingEntry == null) {
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

    public SmartlingKeyEntry getSmartlingEntry(HttpServletRequest req, HttpServletResponse resp) {

        Enumeration en = req.getParameterNames();


        String project = "";
        String fileuri = "";
        String key = "";
        String val = "";
        String updateval = "";
        String jsonfile = "";

        List<String> allowedParams = Arrays.asList("key", "project", "updateval", "jsonfile");

        while (en.hasMoreElements()) {

            String paramName = (String) en.nextElement();
            if (!allowedParams.contains(paramName)) {
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


        if (key.isEmpty() || project.isEmpty() || updateval.isEmpty() || jsonfile.isEmpty()) {
            log.severe("Key:" + key + ".project:" + project + ".updateval:" + updateval + ".jsonfile=" + jsonfile);

            return null;
        } else {
            // first - project Id
            // second - api key

            Pair<String, String> smartlingCred = getProjectcredentials(project);

            return new SmartlingKeyEntry(smartlingCred.getFirst(), key, val, updateval, jsonfile, smartlingCred.getSecond());
        }

    }

    public String handleUpdate(SmartlingKeyEntry updateEntry) throws IOException, JSONException {


        List<String> locales = Arrays.asList("de-DE", "en-GB", "es", "en-US", "fr-FR", "it-IT", "ja-JP", "ko-KR", "pl-PL", "pt-PT", "ru-RU", "tr-TR");

        String currentVal = getCurrentValue(updateEntry);
        if (currentVal == null) {
            String error = "Key:" + updateEntry.getKey() + " Not found in ProjectId:" + updateEntry.getProjectId();
            return error;

        }


        updateEntry.setEngVal(currentVal);

        String resultSet = "";
        for (int i = 0; i < locales.size(); i++) {
            String err = handleUpdateByLocale(updateEntry, locales.get(i));
            resultSet += err + "\n\n";
        }


        return resultSet;


    }


    // keep in DB

    public static void logTranslationToDB(DatastoreService datastoreService, SmartlingKeyEntry updateEntry, String currentTranlation) {

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
    public Pair<String, String> getProjectcredentials(String projectName) {
        if (projectName.equalsIgnoreCase("editor")) {

            return new Pair<>("7646e9dda", "cd863b54-0220-4c0f-8a6b-dc207af32f8e");
        }


        return new Pair<>("be7b3bd3f", "1dcf51a4-ee17-401a-bb27-96064ec2f93c");
    }


    public String getCurrentValue(SmartlingKeyEntry updateEntry) throws IOException, JSONException {


        String responseHtml = SmartlingRequestApi.getFileFromSmartling(updateEntry.getFileUri(), updateEntry.getProjectId(), "en", updateEntry.getApiKey());
        if (responseHtml.contains("VALIDATION_ERROR"))
            return responseHtml;

        HashMap<String, Object> result =
                new ObjectMapper().readValue(responseHtml, HashMap.class);

        String currentValInLocale = (String) result.get(updateEntry.getKey());


        if (currentValInLocale == null || currentValInLocale.isEmpty()) {

            String error = "Key:" + updateEntry.getKey() + " Not found in ProjectId:" + updateEntry.getProjectId();
            log.severe(error);
            return null;
        }

        return currentValInLocale;


    }


    public String handleUpdateByLocale(SmartlingKeyEntry updateEntry, String locale) throws IOException, JSONException {


        String responseHtml = SmartlingRequestApi.getFileFromSmartling(updateEntry.getFileUri(), updateEntry.getProjectId(), locale, updateEntry.getApiKey());
        if (responseHtml.contains("VALIDATION_ERROR"))
            return responseHtml;

        HashMap<String, Object> result =
                new ObjectMapper().readValue(responseHtml, HashMap.class);


        String currentValInLocale = (String) result.get(updateEntry.getKey());
        if (currentValInLocale == null || currentValInLocale.isEmpty()) {

            String error = "Key:" + updateEntry.getKey() + " .Not found in ProjectId:" + updateEntry.getProjectId();
            getUpdateSummaryFromDbAndUpdatenLocales(datastore, updateEntry.getProjectId(), updateEntry.getFileUri(), updateEntry.getKey(),updateEntry.getLocale());
            return error;
        }


        if (currentValInLocale.equals(updateEntry.getEngVal())) {

            String error = "Key:" + updateEntry.getKey() + "found in ProjectId:" + updateEntry.getProjectId() + " .But Still not translated in locale:" + locale + ".value=" + currentValInLocale;
            getUpdateSummaryFromDbAndUpdatenLocales(datastore, updateEntry.getProjectId(), updateEntry.getFileUri(), updateEntry.getKey(),updateEntry.getLocale());
            return error;

        }

        // not translated
        logTranslationToDB(datastore, updateEntry, currentValInLocale);
        getUpdateSummaryFromDbAndUpdatenLocales(datastore, updateEntry.getProjectId(), updateEntry.getFileUri(), updateEntry.getKey(),updateEntry.getLocale());

        String error = "Key:" + updateEntry.getKey() + " ,found in ProjectId:" + updateEntry.getProjectId() + " .translated in locale :" + locale + ".value=" + currentValInLocale + " .Save Value in DB";
        return error;


    }


    public static void getUpdateSummaryFromDbAndUpdatenLocales(DatastoreService datastore, String projectId, String fileUri, String key,String locale) {


        ShardedCounter shardedCounter = new ShardedCounter();
        shardedCounter.increment();

//        Transaction txn = datastore.beginTransaction();
//        try {
//
//            Query.Filter projectId_Filter =
//                    new Query.FilterPredicate("projectId",
//                            Query.FilterOperator.EQUAL,
//                            projectId);
//
//
//            Query.Filter fileUriFilter =
//                    new Query.FilterPredicate("fileUri",
//                            Query.FilterOperator.EQUAL,
//                            fileUri);
//
//            Query.Filter keyFilter =
//                    new Query.FilterPredicate("key",
//                            Query.FilterOperator.EQUAL,
//                            key);
//
//
//            //Use CompositeFilter to combine multiple filters
//            Query.Filter compositeFilter =
//                    Query.CompositeFilterOperator.and(projectId_Filter, fileUriFilter, keyFilter);
//
//
//            Query query = new Query("updateEntrySummary").setFilter(compositeFilter);
//
//
//            List<Entity> records = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1024));
//
//            if (records.isEmpty()) {
//                String error = "Key:" + key + " Not found in summary  DB. ProjectId:" + projectId;
//                log.severe(error);
//                return;
//            }
//
//            if (records.size() > 1) {
//                String error = "Duplicate Key:" + key + "found in DB. ProjectId:" + projectId + ".Number of Summary Entries:" + records.size();
//                log.warning(error);
//            }
//
//
//            Entity entry = records.get(0);
//
//            String locales = entry.getProperty("translatedLocales").toString();
//            log.info("Current translatedLocales = " + locale + ".Project = " + projectId + ".Key = " + key + " .fileUri=" + fileUri);
//
//                entry.setProperty("translatedLocales", locales + "," + locale);
//                log.info("update translatedLocales To = " + locales + "," + locale + ".Project = " + projectId + ".Key = " + key + " .fileUri=" + fileUri);
//
//
//
//
//
//            datastore.put(entry);
//            txn.commit();
//        } finally {
//            if (txn.isActive()) {
//                txn.rollback();
//            }
//
//
//        }
//
//
   }
}