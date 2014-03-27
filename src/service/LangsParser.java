package service;



import com.google.appengine.repackaged.org.codehaus.jackson.JsonGenerationException;
import com.google.appengine.repackaged.org.codehaus.jackson.map.JsonMappingException;
import com.google.appengine.repackaged.org.codehaus.jackson.map.ObjectMapper;
import com.google.appengine.repackaged.org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Created by IntelliJ IDEA.
 * User: itays
 * Date: 1/2/12
 * Time: 6:01 PM
 * parses a xml file that represents the expected component (propertyPanel, sidePanel...) to a map.
 * for each xml item it generates its matching BasicXmlItem and map it with its ID.
 */


public class LangsParser {


    // version and it's alias
    private Map<String, String> keyValMap = new HashMap<String, String>();
    private String jsonFileName;

    public Map<String, String> getKeyValMap() {
        return keyValMap;
    }

    public String getJsonFileName() {
        return jsonFileName;
    }

    public String getValByKey(String key) {
        return keyValMap.get(key);
    }


    public LangsParser(String fileName) {
        this.jsonFileName = fileName;

        this.buildMap();
    }

    private void buildMap() {
        ObjectMapper mapper = new ObjectMapper();

        try {

            // read JSON from a file
            Map<String, Object> userInMap = mapper.readValue(
                    new File(this.jsonFileName),
                    new TypeReference<Map<String, Object>>() {
                    });


            Iterator it = userInMap.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry pairs = (Map.Entry) it.next();
                System.out.println(pairs.getKey() + " = " + pairs.getValue());
                if (pairs.getKey().equals("smartling"))
                    continue;

                this.keyValMap.put(pairs.getKey().toString(), pairs.getValue().toString());

            }


        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}


