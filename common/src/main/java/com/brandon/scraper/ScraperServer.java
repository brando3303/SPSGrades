package com.brandon.scraper;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Dialog;
import com.codename1.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static com.codename1.ui.CN.log;

public class ScraperServer {

    public interface ProgressUpdateRunnable{
        public void WhileLoadingRunnable(Student incompleteUser);
    }

    private static final String GET_USER = "https://the-source-scraper.herokuapp.com/get_user?username=USERNAME";
    private static final String DELETE_INBOX_ITEM = "https://the-source-scraper.herokuapp.com/delete_inbox_item";
    private static final String CREATE_USER = "https://the-source-scraper.herokuapp.com/create_user"; //requires username and password arguments (username & pwd)
    private static final String LOAD_USER_ASSIGNMENTS = "https://the-source-scraper.herokuapp.com/load_user_assignments"; //requires username and password arguments (username & pwd)
    private static final String DEACTIVATE_USER = "https://the-source-scraper.herokuapp.com/deactivate_user"; //requires a username argument
    private static final String UPDATE_USER = "https://the-source-scraper.herokuapp.com/update_user"; //requires username arguments (username)
    private static final String SECRETKEY = "BEipdUg9yp";


    public static Student getStudentFromDataBase(String username, String password) throws InvalidLoginInfo{
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
        r.setUrl(StringUtil.replaceAll(GET_USER,"USERNAME",username) + "&secret=" + SECRETKEY);
        r.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(r);
        try {
            Map<String, Object> studentJson = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            //checks that the Json actually contains a Student, rare case that that the login info no longer works in the db
            if(studentJson.containsKey("status") && (double)studentJson.get("status") != 0){
                log("there was an error trying to get this user from the db, probably no matching username");
                throw new InvalidLoginInfo("no user");
            }
            return createStudentFromMap(studentJson, username, password);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Student createNewUser(String username, String password, ProgressUpdateRunnable pur) throws InvalidLoginInfo{
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
        r.setUrl(CREATE_USER);
        r.addArgument("username", username);
        r.addArgument("pwd",password);
        r.addArgument("secret",SECRETKEY);
        NetworkManager.getInstance().addToQueueAndWait(r);

        Map<String, Object> studentJson = new LinkedHashMap<>();
        try {
            studentJson = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            log("there was an error trying to get this user from the db, probably no username");
        }
            //if there was no user with this login
            if(studentJson.containsKey("status") && (double)studentJson.get("status") == 3){
                throw new InvalidLoginInfo("no user with this login info");
            }
            if(studentJson.get("existingUser").equals("true")){
                return createStudentFromMap((Map<String, Object>) studentJson.get("user"), username, password);
            }
            log("it was a brand new user");
            return loadNewUser(pur, createStudentFromMap((Map<String,Object>)studentJson.get("user"),username,password));

    }

    private static Student loadNewUser(ProgressUpdateRunnable pur, Student incompleteStudent){
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
        r.setUrl(LOAD_USER_ASSIGNMENTS);
        r.addArgument("username", incompleteStudent.getUsername());
        r.addArgument("secret",SECRETKEY);
        Thread executeDuring = new Thread(() -> pur.WhileLoadingRunnable(incompleteStudent));
        executeDuring.start();
        NetworkManager.getInstance().addToQueueAndWait(r);

        Map<String,Object> studentJson = new HashMap<>();
        try {
            studentJson = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            Dialog.show("There was an unknown error","There was an unknown error");
            return null;
        }

        return createStudentFromMap(studentJson, incompleteStudent.getUsername(), incompleteStudent.getPassword());
    }

    private static Student createStudentFromMap(Map<String,Object> studentJson, String username, String password){
        Student returnStudent = new Student(username, password);

        //deserializing the course information (including assignments)
        LinkedHashMap<String, Object> courses = (LinkedHashMap<String, Object>) studentJson.get("lastCourses");
        for(String period : courses.keySet()){
            //makes sure that the current object is a class with a grade
            if(courses.get(period) instanceof LinkedHashMap && ((LinkedHashMap<String,Object>)courses.get(period)).containsKey("gradePercent")) {
                Course course = new Course(returnStudent);

                LinkedHashMap<String, Object> courseMap = (LinkedHashMap<String, Object>) courses.get(period);
                course.courseName = (String) courseMap.get("courseName");
                course.frn = (String) courseMap.get("frn");
                course.gradeLetter = (String) courseMap.get("gradeLetter");
                course.gradePercent = (String) courseMap.get("gradePercent");
                course.period = (String) courseMap.get("period");
                course.teacher = (String) courseMap.get("teacher");

                //getting Assignments for this class
                if (studentJson.containsKey("lastAssignments") && ((LinkedHashMap<String,Object>)studentJson.get("lastAssignments")).keySet().size() > 1) {
                    ArrayList<LinkedHashMap<String, Object>> assignmentList = (ArrayList<LinkedHashMap<String, Object>>) ((LinkedHashMap<String, Object>) studentJson.get("lastAssignments")).get(period);
                    course.assignments = deserializeSingleCourseAssignments(assignmentList);
                }
                course.sortAssignments();
                returnStudent.courses.add(course);
            }
        }
        //deserialize the inbox
        if(studentJson.containsKey("inbox")) {
            LinkedHashMap<String, Object> inboxMap = (LinkedHashMap<String, Object>) studentJson.get("inbox");
            returnStudent.inbox = deserializeInbox(inboxMap);
            returnStudent.inbox.sortInboxItems();
            //give each inbox item a reference to it's course
            for(InboxItem ii : returnStudent.inbox.getInboxItems()) {
                for (Course c : returnStudent.courses) {
                    if (ii.courseName.equals(c.courseName)) {
                        ii.course = c;
                    }
                }
            }
        } else{
            returnStudent.inbox = new Inbox();
        }
        //returnStudent.calculateGPA();
        return returnStudent;
    }

    private static ArrayList<Assignment> deserializeSingleCourseAssignments(ArrayList<LinkedHashMap<String,Object>> assignmentList){
        ArrayList<Assignment> returnAssignments = new ArrayList<>();
        for (LinkedHashMap<String, Object> assignmentMap : assignmentList) {
            Assignment a = new Assignment();
            a.modifiedDate = (String) assignmentMap.get("modifiedDate");

            if (a.modifiedDate != null) {
                try {
                    a.epochDate = new SimpleDateFormat("yyyy-MM-dd").parse(a.modifiedDate).getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            a.name = (String) assignmentMap.get("name");
            if (assignmentMap.containsKey("points")) {
                a.points = (Double) assignmentMap.get("points");
            }
            a.total = (Double) assignmentMap.get("total");
            returnAssignments.add(a);
        }
        return returnAssignments;
    }

    private static Inbox deserializeInbox(LinkedHashMap<String,Object> inboxMap){
        Inbox returnInbox = new Inbox();
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
                    } else if (ac.type.equals("created") && acMap.containsKey("points")) {
                        ac.points = (Double) acMap.get("points");
                    }
                    ac.total = (Double) acMap.get("total");

                    item.assignmentChanges.add(ac);
                }
            }
            returnInbox.getInboxItems().add(item);
        }
        return returnInbox;
    }


    public static void deleteInboxItem(InboxItem item, Student student){
        String link = DELETE_INBOX_ITEM;
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
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
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        r.addArgument("secret", SECRETKEY);
        log("deactivating user");
        NetworkManager.getInstance().addToQueue(r);
    }

    public static Student updateUser(Student student){
        String link = UPDATE_USER;
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
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

    public static void sendDeviceID(String username, String deviceID){
        ConnectionRequest r = new ConnectionRequest(){
            @Override
            protected void handleErrorResponseCode(int code, String message) {}
        };
        r.setUrl(CREATE_USER);
        r.addArgument("username", username);
        r.addArgument("deviceId", deviceID);
        r.addArgument("secret",SECRETKEY);
        NetworkManager.getInstance().addToQueue(r);
    }
}
