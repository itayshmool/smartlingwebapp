package servlets;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import service.LangsParser;


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



public class MainServlet extends HttpServlet  {


    public ServletContext context;
    public LangsParser langsParserEN;
    public LangsParser langsParserES;


    public DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public static final Logger log = Logger.getLogger(MainServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        context = getServletContext();

        String pathEN = getServletContext().getRealPath("/WEB-INF") + "/messages_en.json";

       this.langsParserEN = new LangsParser(pathEN);


        String pathES = getServletContext().getRealPath("/WEB-INF") + "/messages_es.json";

        this.langsParserES = new LangsParser(pathES);

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {



        Enumeration en = req.getParameterNames();
        String lang = "EN";
        LangsParser langsParser = getLangsByUserLang(req,resp);





        int total = langsParser.getKeyValMap().size() -1;

        String responseHtml = "";
        String entries = "";


        for (int i = 0 ; i < total ; i++)
        {
            String key = "key" + (i+1);
            String val =   langsParser.getKeyValMap().get(key);

            entries += WriteAsHtml.createEntry(key, val);
            entries += "\n";
        }

        responseHtml = WriteAsHtml.getFullHTML(entries, langsParser.getValByKey("header"));
        resp.getWriter().println(responseHtml);


    }

    LangsParser getLangsByUserLang(HttpServletRequest req, HttpServletResponse resp)
    {

        Enumeration en = req.getParameterNames();
        String lang = "EN";


        List<String> allowedParams = Arrays.asList("lang");

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

            if (paramName.equals("lang")) {
                lang = req.getParameter(paramName);
            }

        }




        if (lang.equalsIgnoreCase("ES"))
            return this.langsParserES;

        return this.langsParserEN;



    }
}