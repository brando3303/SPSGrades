package com.brandon.scraper;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.codename1.ui.CN.log;

public class ScraperServer {

    private static final String GET_USER = "https://the-source-scraper.herokuapp.com/get_user?username=USERNAME";
    private static final String DELETE_INBOX_ITEM = "https://the-source-scraper.herokuapp.com/delete_inbox_item";
    private static final String CREATE_USER = "https://the-source-scraper.herokuapp.com/create_user"; //requires username and password arguments (username & pwd)
    private static final String DEACTIVATE_USER = "https://the-source-scraper.herokuapp.com/deactivate_user"; //requires a username argument
    private static final String UPDATE_USER = "https://the-source-scraper.herokuapp.com/update_user"; //requires username arguments (username)
    private static final String SECRETKEY = "BEipdUg9yp";


    public static Student getStudentFromDataBase(String username, String password) throws InvalidLoginInfo{
        ConnectionRequest r = new ConnectionRequest();
        r.setUrl(StringUtil.replaceAll(GET_USER,"USERNAME",username) + "&secret=" + SECRETKEY);
        r.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(r);
        try {
            Map<String, Object> studentJson = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            //checks that the Json actually contains a Student, rare case that that the login info no longer works in the db
            if(studentJson.keySet().size() == 0){
                log("there was an error trying to get this user from the db, probably no matching username");
                throw new InvalidLoginInfo("no user");
            }
            return createStudentFromMap(studentJson, username, password);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Student createNewUser(String username, String password) throws InvalidLoginInfo{
        ConnectionRequest r = new ConnectionRequest();
        r.setUrl(CREATE_USER);
        r.addArgument("username", username);
        r.addArgument("pwd",password);
        r.addArgument("secret",SECRETKEY);
        NetworkManager.getInstance().addToQueueAndWait(r);
        try {
            log(Util.readToString(new ByteArrayInputStream(r.getResponseData())) + "was the response data");
        } catch (IOException e) {
            e.printStackTrace();
            log("error trying to read it");
        }


        try {
            Map<String, Object> studentJson = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            //checks that the Json actually contains a Student, essentially that that user name cn sign into the source

            if(studentJson.containsKey("user")) {
                log("the user already existed in the database");
                return createStudentFromMap((Map<String, Object>) studentJson.get("user"), username, password);

            }
            else if(studentJson.containsKey("error")){
                log("there was an error creating this user in the db, likely not connected to a source user");
                throw new InvalidLoginInfo("no user exists on the source");
            }
            else{
                log("created a new user in the db");
                return createStudentFromMap(studentJson, username, password);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log("there was an error trying to get this user from the db, probably no username");

        }
        return null;
    }

    private static Student createStudentFromMap(Map<String,Object> studentJson, String username, String password){
        Student returnStudent = new Student(username, password);

        LinkedHashMap<String, Object> courses = (LinkedHashMap<String, Object>) studentJson.get("lastCourses");
        for(int period = 1; period <= 6; period++){
            Course course = new Course();

            log(Integer.toString(period));
            LinkedHashMap<String,Object> courseMap = (LinkedHashMap<String, Object>) courses.get(Integer.toString(period));
            course.courseName = (String)courseMap.get("courseName");
            course.frn = (String)courseMap.get("frn");
            course.gradeLetter = (String)courseMap.get("gradeLetter");
            course.gradePercent = (String)courseMap.get("gradePercent");
            course.period = (String)courseMap.get("period");
            course.teacher = (String)courseMap.get("teacher");

            //getting Assignemnts
            ArrayList<LinkedHashMap<String,Object>> assignmentList = (ArrayList<LinkedHashMap<String, Object>>) ((LinkedHashMap<String, Object>)studentJson.get("lastAssignments")).get(Integer.toString(period));
            for(LinkedHashMap<String,Object> assignmentMap : assignmentList){
                Assignment a = new Assignment();
                a.modifiedDate = (String)assignmentMap.get("modifiedDate");

                if(a.modifiedDate != null) {
                    try {
                        a.epochDate = new SimpleDateFormat("yyyy-MM-dd").parse(a.modifiedDate).getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                a.name = (String)assignmentMap.get("name");
                if(assignmentMap.containsKey("points")) {
                    a.points = (Double) assignmentMap.get("points");
                }
                a.total = (Double)assignmentMap.get("total");
                course.assignments.add(a);
            }

            returnStudent.courses.add(course);
        }

        Inbox inbox = new Inbox();
        if(studentJson.containsKey("inbox")) {
            LinkedHashMap<String, Object> inboxMap = (LinkedHashMap<String, Object>) studentJson.get("inbox");
            Set<String> set = inboxMap.keySet();
            for (String s : set) {
                LinkedHashMap<String, Object> inboxItemMap = (LinkedHashMap<String, Object>) inboxMap.get(s);
                InboxItem item = new InboxItem();

                item.index = s;
                item.courseName = (String) inboxItemMap.get("courseName");
                item.deleted = inboxItemMap.get("deleted") == "true";
                item.gradeBefore = (String) inboxItemMap.get("gradeBefore");
                item.gradeNow = (String) inboxItemMap.get("gradeNow");
                item.time = (Double) inboxItemMap.get("time");
                item.timeReadable = (String) inboxItemMap.get("timeReadable");

                ArrayList<LinkedHashMap<String, Object>> assignmentChangesMap = (ArrayList<LinkedHashMap<String, Object>>) inboxItemMap.get("assignmentChanges");
                if (assignmentChangesMap != null) {
                    for (LinkedHashMap<String, Object> acMap : assignmentChangesMap) {
                        AssignmentChange ac = new AssignmentChange();
                        ac.name = (String) acMap.get("name");
                        ac.type = (String) acMap.get("type");
                        if (ac.type.equals("modified")) {
                            ac.pointsBefore = (Double) acMap.get("pointsBefore");
                            ac.pointsNow = (Double) acMap.get("pointsNow");
                        } else if (ac.type.equals("created")) {
                            ac.points = (Double) acMap.get("points");
                        }
                        ac.total = (Double) acMap.get("total");

                        item.assignmentChanges.add(ac);
                    }
                }

                for (Course c : returnStudent.courses) {
                    if (item.courseName.equals(c.courseName)) {
                        item.course = c;
                    }
                }

                inbox.inboxItems.add(item);
            }
        }
        returnStudent.inbox = inbox;


        for(Course c : returnStudent.courses){
            if(c.assignments.size() != 0) {
                c.sortAssignments();
            }
        }
        return returnStudent;
    }

    public static void deleteInboxItem(InboxItem item, Student student){
        String link = DELETE_INBOX_ITEM;
        ConnectionRequest r = new ConnectionRequest();
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        r.addArgument("id", item.index);
        r.addArgument("secret", SECRETKEY);
        log(NetworkManager.getInstance().isQueueIdle() ? "queue idle": "queue is not idle");
        NetworkManager.getInstance().addToQueue(r);
    }

    public static void deactivateUser(Student student){
        String link = DEACTIVATE_USER;
        ConnectionRequest r = new ConnectionRequest();
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        r.addArgument("secret", SECRETKEY);
        log("deactivating user");
        NetworkManager.getInstance().addToQueue(r);
    }

    public static Student updateUser(Student student){
        String link = UPDATE_USER;
        ConnectionRequest r = new ConnectionRequest();
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        r.addArgument("secret", SECRETKEY);
        log("updating user");
        NetworkManager.getInstance().addToQueueAndWait(r);

        try {
            Map<String, Object> studentJson = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            return createStudentFromMap((Map<String,Object>)studentJson.get("userNow"), student.getUsername(), student.getPassword());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }
}
