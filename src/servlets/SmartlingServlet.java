package servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: itay_shmool
 * Date: 10/20/13
 * Time: 1:24 PM
 * To change this template use File | Settings | File Templates.
 */



public class SmartlingServlet extends HttpServlet  {


    public ServletContext context;
    public LangsParser langsParserEN;
    public LangsParser langsParserES;


    public DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public static final Logger log = Logger.getLogger(SmartlingServlet.class.getName());

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

        if (smartlingEntry == null)
        {
            resp.getWriter().println("wrong usage");
            return;

        }

        String responseHtml = SmartlingRequestApi.getFileFromSmartling(smartlingEntry.getFileUri(),smartlingEntry.getProjectId(),smartlingEntry.getLocale());


        resp.getWriter().println(responseHtml);


    }

    public SmartlingKeyEntry getSmartlingEntry(HttpServletRequest req, HttpServletResponse resp)
    {

        Enumeration en = req.getParameterNames();


        String locale = "";
        String projectid = "";
        String fileuri = "";

        List<String> allowedParams = Arrays.asList("locale","projectId","fileUri");

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

            if (paramName.equals("locale")) {
                locale = req.getParameter(paramName);
            }

            if (paramName.equals("projectId")) {
                projectid = req.getParameter(paramName);
            }

            if (paramName.equals("fileUri")) {
                fileuri = req.getParameter(paramName);
            }

        }


        if (locale.isEmpty() || projectid.isEmpty() || fileuri.isEmpty())
        {

            return null;
        }

        else

            return new SmartlingKeyEntry(projectid,locale,fileuri);


    }
}