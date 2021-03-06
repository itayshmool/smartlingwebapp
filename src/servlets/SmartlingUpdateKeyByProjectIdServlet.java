package servlets;

import com.google.appengine.api.datastore.*;
import com.google.appengine.labs.repackaged.org.json.JSONException;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

import static servlets.SmartlingGetMergedFullServlet.getTranslationFromDb;
import static servlets.SmartlingUpdateKeySingleLocaleServlet.getUpdateSummaryFromDbAndUpdatenLocales;


/**
 * Created with IntelliJ IDEA.
 * User: itay_shmool
 * Date: 10/20/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */



public class SmartlingUpdateKeyByProjectIdServlet extends HttpServlet  {


    public ServletContext context;



    public DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public static final Logger log = Logger.getLogger(SmartlingUpdateKeyByProjectIdServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        context = getServletContext();



    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {



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


        String projectid = "";
        String fileuri = "";
        String apiKey = "";
        String key = "";
        String val = "";
        String updateval = "";
        String locale = "";

        List<String> allowedParams = Arrays.asList("key","val","fileUri","projectId","updateval","apikey","locale");

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

            if (paramName.equals("projectId")) {
                projectid = req.getParameter(paramName);
            }

            if (paramName.equals("apikey")) {
                apiKey = req.getParameter(paramName);
            }

            if (paramName.equals("fileUri")) {
                fileuri = req.getParameter(paramName);
            }

            if (paramName.equals("val")) {
                val = req.getParameter(paramName);
            }

            if (paramName.equals("updateval")) {
                updateval = req.getParameter(paramName);
            }

            if (paramName.equals("locale")) {
                locale = req.getParameter(paramName);
            }
        }


        if (key.isEmpty() || projectid.isEmpty() || fileuri.isEmpty() || val.isEmpty() || updateval.isEmpty())
        {
            log.severe("Key:" + key + ".projectid:" + projectid + ".fileUri:" + fileuri + ".val:" + val + ".updateval:" + updateval);

            return null;
        }

        else

            return new SmartlingKeyEntry(projectid,key,val,updateval,fileuri,apiKey,locale);


    }

    public String handleUpdate(SmartlingKeyEntry updateEntry) throws IOException, JSONException {



        String responseHtml = SmartlingRequestApi.getFileFromSmartling(updateEntry.getFileUri(),updateEntry.getProjectId(),updateEntry.getLocale(),updateEntry.getApiKey());
        if (responseHtml.contains("VALIDATION_ERROR"))
            return responseHtml;

        HashMap<String,Object> result =
                new ObjectMapper().readValue(responseHtml, HashMap.class);

        String currentValInLocale = (String) result.get(updateEntry.getKey());
        if (currentValInLocale == null || currentValInLocale.isEmpty())
        {

            String error = "Key:"+updateEntry.getKey() + " Not found in ProjectId:" + updateEntry.getProjectId();
            getUpdateSummaryFromDbAndUpdatenLocales(datastore,updateEntry.getProjectId(),updateEntry.getFileUri(),updateEntry.getKey(),updateEntry.getLocale());
            log.severe(error);
            return error;
        }

        if (currentValInLocale.equals(updateEntry.getEngVal()))
        {

            String error = "Key:"+updateEntry.getKey() + "found in ProjectId:" + updateEntry.getProjectId() + " But Still not translated in locale:" + updateEntry.getLocale() + ".value="+currentValInLocale;

            getUpdateSummaryFromDbAndUpdatenLocales(datastore,updateEntry.getProjectId(),updateEntry.getFileUri(),updateEntry.getKey(),updateEntry.getLocale());
            log.info(error);
            return error;



        }

        // not translated
        logTranslationToDB(datastore,updateEntry,currentValInLocale);
        getUpdateSummaryFromDbAndUpdatenLocales(datastore,updateEntry.getProjectId(),updateEntry.getFileUri(),updateEntry.getKey(),updateEntry.getLocale());

        String error = "Key:"+updateEntry.getKey() + " .found in ProjectId:" + updateEntry.getProjectId() + " .translated in locale:" + updateEntry.getLocale() + ".value="+currentValInLocale + ".Save Value in DB";
        log.info(error);
        return error;




    }


        // keep in DB

    public static void logTranslationToDB(DatastoreService datastoreService,SmartlingKeyEntry updateEntry , String currentTranlation) throws UnsupportedEncodingException {

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
        entry.setProperty("translatedVal" ,currentTranlation);


         entry.setProperty("translatedVal", currentTranlation);



        datastoreService.put(entry);


    }





}