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



public class KeepaliveServlet extends HttpServlet  {


    public ServletContext context;
    public LangsParser langsParserEN;
    public LangsParser langsParserES;


    public DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    public static final Logger log = Logger.getLogger(KeepaliveServlet.class.getName());

    public void init(ServletConfig config) throws ServletException {
        super.init(config);


    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {



        resp.getWriter().println("hello world");


    }


}