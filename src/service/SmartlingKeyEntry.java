package service;

/**
 * Created with IntelliJ IDEA.
 * User: itay_shmool
 * Date: 4/1/14
 * Time: 1:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class SmartlingKeyEntry {


    //apiKey=d7cc5b45-072d-4182-be6f-e8105275368a&fileUri=/files/messages_en.json&projectId=7241ba9c9&locale=es" "https://api.smartling.com/v1/file/get"

    public String apiKey;
    public String projectId;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getEngVal() {
        return engVal;
    }

    public void setEngVal(String engVal) {
        this.engVal = engVal;
    }

    public String getUpdatedEngVal() {
        return updatedEngVal;
    }

    public void setUpdatedEngVal(String updatedEngVal) {
        this.updatedEngVal = updatedEngVal;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getTranslationInLocale() {
        return translationInLocale;
    }

    public void setTranslationInLocale(String translationInLocale) {
        this.translationInLocale = translationInLocale;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String engVal;
    public String updatedEngVal;
    public String locale;
    public String translationInLocale;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String fileUri;
    public String key;


    public SmartlingKeyEntry(String projectId,  String key , String engVal, String updateval, String fileUri) {

        this.projectId = projectId;
        this.key = key;
        this.updatedEngVal = updateval;
        this.engVal = engVal;
        this.fileUri = fileUri;

    }

    public SmartlingKeyEntry(String projectId,  String key , String engVal, String updateval, String fileUri,String apiKey) {

        this.projectId = projectId;
        this.key = key;
        this.updatedEngVal = updateval;
        this.engVal = engVal;
        this.fileUri = fileUri;
        this.apiKey = apiKey;

    }

    public SmartlingKeyEntry(String projectId,  String key , String engVal, String updateval, String fileUri,String apiKey,String locale) {

        this.projectId = projectId;
        this.key = key;
        this.updatedEngVal = updateval;
        this.engVal = engVal;
        this.fileUri = fileUri;
        this.apiKey = apiKey;
        this.locale =  locale;

    }

    // This C-tor is being used just for pulling file from smartling
    public SmartlingKeyEntry(String projectId, String locale, String fileUri) {
        this.projectId = projectId;
        this.fileUri = fileUri;
        this.locale = locale;

    }

    // This C-tor is being used just for pulling file from smartling
    public SmartlingKeyEntry(String projectId, String locale, String fileUri,String apiKey) {
        this.projectId = projectId;
        this.fileUri = fileUri;
        this.locale = locale;
        this.apiKey = apiKey;

    }

    // This C-tor is being used just for pulling file from smartling
    public SmartlingKeyEntry() {


    }
}
