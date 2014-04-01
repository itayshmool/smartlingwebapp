package servlets;

import com.google.appengine.api.datastore.*;
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
import java.util.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: itay_shmool
 * Date: 10/20/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */


public class SmartlingGetMergedServlet extends HttpServlet {


    public ServletContext context;
    public LangsParser langsParserEN;
    public LangsParser langsParserES;


    public DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public static final Logger log = Logger.getLogger(SmartlingGetMergedServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        context = getServletContext();

//        String pathEN = getServletContext().getRealPath("/WEB-INF") + "/messages_en.json";
//
//       this.langsParserEN = new LangsParser(pathEN);
//
//
//        String pathES = getServletContext().getRealPath("/WEB-INF") + "/messages_es.json";
//
//        this.langsParserES = new LangsParser(pathES);

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {


        Enumeration en = req.getParameterNames();
        SmartlingKeyEntry smartlingEntry = getSmartlingEntry(req, resp);

        if (smartlingEntry == null) {
            resp.getWriter().println("wrong usage");
            return;

        }

        String responseHtml = "Fail";
        try {
            responseHtml = getMergedfile(smartlingEntry);
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        resp.setContentType("text/x-json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.getWriter().println(responseHtml);


    }

    public SmartlingKeyEntry getSmartlingEntry(HttpServletRequest req, HttpServletResponse resp) {

        Enumeration en = req.getParameterNames();


        String projectid = "";
        String fileuri = "";
        String locale = "";

        List<String> allowedParams = Arrays.asList("fileUri", "projectId", "locale");

        while (en.hasMoreElements()) {

            String paramName = (String) en.nextElement();
            if (!allowedParams.contains(paramName)) {
                try {
                    resp.getWriter().println("Syntax Error !!! parameter:" + paramName + " is not allowed.list of allowed parameters is(" + allowedParams.toString() + ")");
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }

            }


            if (paramName.equals("projectId")) {
                projectid = req.getParameter(paramName);
            }

            if (paramName.equals("fileUri")) {
                fileuri = req.getParameter(paramName);
            }

            if (paramName.equals("locale")) {
                locale = req.getParameter(paramName);
            }


        }


        if ( projectid.isEmpty() || fileuri.isEmpty() || locale.isEmpty() ) {
            log.severe("locale:" + locale + ".projectid:" + projectid + ".fileUri:" + fileuri);

            return null;
        } else

            return new SmartlingKeyEntry(projectid, locale,  fileuri);


    }

    public String getMergedfile(SmartlingKeyEntry updateEntry) throws IOException, JSONException {


        // for each locale (for the specific project) , get translated file from smartling
        updateEntry.setLocale("es");
        String responseHtml = SmartlingRequestApi.getFileFromSmartling(updateEntry.getFileUri(), updateEntry.getProjectId(), updateEntry.getLocale());
        if (responseHtml.contains("VALIDATION_ERROR"))
            return responseHtml;


        // Get List of updated Strings from App Engine DB
        List<SmartlingKeyEntry> dbFile = getTranslationFromDb(datastore, updateEntry.getProjectId(), updateEntry.getLocale(), updateEntry.getFileUri());
        if (dbFile.isEmpty()) {

            log.info("No Updated Strings for ProjectId:" + updateEntry.getProjectId() + ";File:" + updateEntry.getFileUri() + ";Locale:" + updateEntry.getLocale());
            return responseHtml;
        }


        // build map from Json (smarling)
        HashMap<String, Object> smartlingFile = new ObjectMapper().readValue(responseHtml, HashMap.class);




        for (int i = 0; i < dbFile.size(); i++) {


            String keyInDb = dbFile.get(i).getKey();
            String updatedEngVal = dbFile.get(i).getUpdatedEngVal();
            String tranlatedVal = dbFile.get(i).getTranslationInLocale();



            String currentValInLocaleFile = (String) smartlingFile.get(keyInDb);

            if (currentValInLocaleFile == null || currentValInLocaleFile.isEmpty()) {

                String error = "Key:" + keyInDb + " Not found in ProjectId:" + updateEntry.getProjectId() + ", But it's on DB. should we remove from DB???";
                log.severe(error);
                continue;
            }

            if (currentValInLocaleFile.equals(updatedEngVal)) {

                String error = "Key:" + keyInDb + "found in ProjectId:" + updateEntry.getProjectId() + " But Still not translated in locale:" + updateEntry.getLocale() + ".value=" + currentValInLocaleFile;
                smartlingFile.put(keyInDb,tranlatedVal);
                log.info(error);
                continue;
            }

            if (!currentValInLocaleFile.equals(updatedEngVal)) {

                String error = "Key:" + keyInDb + "found in ProjectId:" + updateEntry.getProjectId() + " re-translated in locale:" + updateEntry.getLocale() + ".value=" + currentValInLocaleFile;
                log.info(error);


                // remove from DB !!!
                removeKeyFromDb(datastore, updateEntry.getProjectId(), updateEntry.getLocale(), updateEntry.getFileUri(), keyInDb);
                continue;
            }




        }

        JSONObject returnJson = new JSONObject();

        Iterator it = smartlingFile.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            if (!pairs.getKey().toString().equalsIgnoreCase("smartling"))
                returnJson.put(pairs.getKey().toString() , pairs.getValue().toString());
        }

        return returnJson.toString();

    }


    // keep in DB

    public static void logTranslationToDB(DatastoreService datastoreService, SmartlingKeyEntry updateEntry, String currentTranlation) {

        Date date = new Date();
        Entity entry = new Entity("updateEntry");

        entry.setProperty("date", date);
        entry.setProperty("projectId", updateEntry.getProjectId());
        entry.setProperty("locale", updateEntry.getLocale());
        entry.setProperty("key", updateEntry.getKey());
        entry.setProperty("fileUri", updateEntry.getFileUri());

        Text valEnText = new Text(updateEntry.getEngVal());
        entry.setProperty("valEn", valEnText);

        Text valEnUpdatedText = new Text(updateEntry.getUpdatedEngVal());
        entry.setProperty("valEnUpdated", valEnUpdatedText);

        Text translatedText = new Text(currentTranlation);
        entry.setProperty("translatedVal", translatedText);


        datastoreService.put(entry);


    }

    public static List<SmartlingKeyEntry> getTranslationFromDb(DatastoreService datastore, String projectId, String locale, String fileUri) {

        List<SmartlingKeyEntry> list = new ArrayList<>();

        List<Query.Filter> filterList = new ArrayList<>();


        Query.Filter projectId_Filter =
                new Query.FilterPredicate("projectId",
                        Query.FilterOperator.EQUAL,
                        projectId);


        Query.Filter localeFilter =
                new Query.FilterPredicate("locale",
                        Query.FilterOperator.EQUAL,
                        locale);


        Query.Filter fileUriFilter =
                new Query.FilterPredicate("fileUri",
                        Query.FilterOperator.EQUAL,
                        fileUri);


        //Use CompositeFilter to combine multiple filters
        Query.Filter compositeFilter =
                Query.CompositeFilterOperator.and(projectId_Filter, localeFilter, fileUriFilter);


        Query query = new Query("updateEntry").setFilter(compositeFilter);


        List<Entity> records = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1024));


        for (int i = 0; i < records.size(); i++) {

            String key = records.get(i).getProperty("key").toString();

            String valEnUpdated = ((Text)records.get(i).getProperty("valEnUpdated")).getValue();
            String translatedVal = ((Text)records.get(i).getProperty("translatedVal")).getValue();

            SmartlingKeyEntry smartlingKeyEntry = new SmartlingKeyEntry();
            smartlingKeyEntry.setKey(key);
            smartlingKeyEntry.setUpdatedEngVal(valEnUpdated);
            smartlingKeyEntry.setTranslationInLocale(translatedVal);

            list.add(smartlingKeyEntry);
            log.info("Key:" + key + " ;updatedVal=" + valEnUpdated + ";translatedVal=" + translatedVal);
        }


        return list;
    }

    public static void removeKeyFromDb(DatastoreService datastore, String projectId, String locale, String fileUri, String key) {

        Key dbKey = getTranslationEntryFromDb(datastore, projectId, locale, fileUri, key);
        if (dbKey != null)
            datastore.delete(dbKey);


    }


    public static Key getTranslationEntryFromDb(DatastoreService datastore, String projectId, String locale, String fileUri, String key) {

        List<SmartlingKeyEntry> list = new ArrayList<>();

        List<Query.Filter> filterList = new ArrayList<>();


        Query.Filter projectId_Filter =
                new Query.FilterPredicate("projectId",
                        Query.FilterOperator.EQUAL,
                        projectId);


        Query.Filter localeFilter =
                new Query.FilterPredicate("locale",
                        Query.FilterOperator.EQUAL,
                        locale);


        Query.Filter fileUriFilter =
                new Query.FilterPredicate("fileUri",
                        Query.FilterOperator.EQUAL,
                        fileUri);

        Query.Filter keyFilter =
                new Query.FilterPredicate("key",
                        Query.FilterOperator.EQUAL,
                        key);


        //Use CompositeFilter to combine multiple filters
        Query.Filter compositeFilter =
                Query.CompositeFilterOperator.and(projectId_Filter, localeFilter, fileUriFilter, keyFilter);


        Query query = new Query("updateEntry").setFilter(compositeFilter);


        List<Entity> records = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1024));

        if (records.isEmpty())
        {
            String error = "Key:" + key + " Not found in DB. ProjectId:" +projectId;
            log.severe(error);
            return null;
        }

        if (records.size() > 1)
        {
            String error = "Duplicate Key:" + key + "found in DB. ProjectId:" +projectId + ".Number of Entries:" + records.size();
            log.warning(error);
        }



        return records.get(0).getKey();


    }

}