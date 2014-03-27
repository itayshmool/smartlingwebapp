package servlets;

/**
 * Created with IntelliJ IDEA.
 * User: itays
 * Date: 3/14/13
 * Time: 11:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class WriteAsHtml {


    public static String web = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "  <head>\n" +
            "    <meta charset=\"utf-8\">\n" +
            "</head>\n" +
            "<body>\n" +
            "<h1> __HEADER__ </h1>" +

            "<table border = \"1\">\n" +
            "<tr>\n" +
            "<td>key</td>\n" +
            "<td>val</td>\n" +
            "</tr>" +

            "__ENTRIES__" +

            "  </body>\n" +
            "</html>";


    public static String entry =

            "<tr>\n" +
            "<td>__KEY__</td> \n" +
            "<td>__VAL__</td> \n" +

            "\n" +
            "</tr>";



    public static String createEntry(String key , String val) {
        return entry.replaceAll("__KEY__", key).replaceAll("__VAL__", val);

    }

    public static String getFullHTML(String entries,String header) {


            return web.replaceAll("__ENTRIES__", entries).replaceAll("__HEADER__",header);



    }



}
