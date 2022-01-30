package com.brandon.scraper;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.push.Push;
import com.codename1.ui.Dialog;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static com.codename1.ui.CN.log;

public class ScraperServer {

    public static class SilentConnectionRequest extends ConnectionRequest{
        @Override
        protected void handleErrorResponseCode(int code, String message) {log("caught networking code error");}
        @Override
        protected void handleException(Exception err) {log("caught networking exception");}
        @Override
        protected void handleIOException(IOException err) {log("caught networking IOException");}
        @Override
        protected void handleRuntimeException(RuntimeException err) {log("caught networking exception");}
    }

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
        SilentConnectionRequest r = new SilentConnectionRequest();
        r.setFailSilently(true);
        r.setUrl(GET_USER);
        r.addArgument("username", username);
        r.addArgument("secret", SECRETKEY);
        r.setPost(true);
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
        SilentConnectionRequest r = new SilentConnectionRequest();
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
        SilentConnectionRequest r = new SilentConnectionRequest();
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

        if(studentJson.containsKey("deviceId")) {
            Main.getInstance().setDeviceId((String)studentJson.get("deviceId"));
        } else
            if(Push.getPushKey() != null){
            log("sent deviceId: " + Push.getPushKey());
            ScraperServer.sendDeviceID(username,password,Push.getPushKey());
        }


        //deserializing the course information (including assignments)
        LinkedHashMap<String, Object> courses = (LinkedHashMap<String, Object>) studentJson.get("lastCourses");
        for(String period : courses.keySet()){
            //makes sure that the current object is a class with a grade
            Course course = new Course(returnStudent);

            LinkedHashMap<String, Object> courseMap = (LinkedHashMap<String, Object>) courses.get(period);
            course.courseName = (String) courseMap.get("courseName");
            course.frn = (String) courseMap.get("frn");
            course.gradeLetter = (String) courseMap.get("gradeLetter");
            if (course.gradeLetter == null)
                course.gradeLetter = "NA";
            course.gradePercent = (String) courseMap.get("gradePercent");
            if (course.gradePercent == null)
                course.gradePercent = "NA";
            course.period = (String) courseMap.get("period");
            course.teacher = (String) courseMap.get("teacher");

            //getting Assignments for this class
            if (studentJson.containsKey("lastAssignments") && ((LinkedHashMap<String,Object>)studentJson.get("lastAssignments")).keySet().size() != 0) {
                ArrayList<LinkedHashMap<String, Object>> assignmentList = (ArrayList<LinkedHashMap<String, Object>>) ((LinkedHashMap<String, Object>) studentJson.get("lastAssignments")).get(period);
                course.assignments = deserializeSingleCourseAssignments(assignmentList);
                course.sortAssignments();
            }
            if (studentJson.containsKey("inbox") && ((LinkedHashMap<String,Object>)studentJson.get("inbox")).containsKey(period) && ((LinkedHashMap<String,Object>)studentJson.get("inbox")).keySet().size() != 0){
                LinkedHashMap<String, Object> assignmentList = (LinkedHashMap<String, Object>) ((LinkedHashMap<String, Object>) studentJson.get("inbox")).get(period);
                course.assignmentChanges = deserializeCourseInbox(assignmentList);
                course.sortAssignmentChanges();
                for(AssignmentChange ac : course.assignmentChanges){
                    ac.course = course;
                }
            }
            returnStudent.courses.add(course);
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

    private static ArrayList<AssignmentChange> deserializeCourseInbox(LinkedHashMap<String,Object> inboxMap){
        ArrayList<AssignmentChange> assignmentChanges = new ArrayList<>();
        Set<String> set = inboxMap.keySet();
        for (String s : set) {
            LinkedHashMap<String, Object> inboxItemMap = (LinkedHashMap<String, Object>) inboxMap.get(s);
            AssignmentChange assignmentChange = new AssignmentChange();

            assignmentChange.assignmentChangeType = (String)inboxItemMap.get("assignmentChangeType");
            assignmentChange.assignmentName = (String)inboxItemMap.get("assignmentName");
            assignmentChange.assignmentTotal = (Double)inboxItemMap.get("assignmentTotal");
            assignmentChange.courseName = (String) inboxItemMap.get("courseName");
            assignmentChange.deleted = inboxItemMap.get("deleted") == "true";
            assignmentChange.id = s;
            assignmentChange.overallGradeBefore = (String) inboxItemMap.get("overallGradeBefore");
            assignmentChange.overallGradeNow = (String) inboxItemMap.get("gradeNow");
            assignmentChange.time = (Double) inboxItemMap.get("time");
            assignmentChange.timeReadable = (String) inboxItemMap.get("timeReadable");
            if(assignmentChange.assignmentChangeType.equals("created")){
                assignmentChange.assignmentPoints = (Double)inboxItemMap.get("assignmentPoints");
            }
            if(assignmentChange.assignmentChangeType.equals("modified")){
                assignmentChange.assignmentPointsBefore = (Double)inboxItemMap.get("assignmentPointsBefore");
                assignmentChange.assignmentPointsNow = (Double)inboxItemMap.get("assignmentPointsNow");
            }
            assignmentChanges.add(assignmentChange);
            log("found assignment change: " + assignmentChange.assignmentName + ": deleted = " + (assignmentChange.deleted ? "true": "false"));

        }
        return assignmentChanges;
    }


    public static void deleteAssignmentChange(AssignmentChange ac, Student student){
        String link = DELETE_INBOX_ITEM;
        SilentConnectionRequest r = new SilentConnectionRequest();
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        //removes that pesky P
        r.addArgument("period", ac.course.period.substring(1));
        r.addArgument("id", ac.id);
        r.addArgument("secret", SECRETKEY);
        NetworkManager.getInstance().addToQueue(r);
    }

    public static void deactivateUser(Student student){
        String link = DEACTIVATE_USER;
        SilentConnectionRequest r = new SilentConnectionRequest();
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        r.addArgument("secret", SECRETKEY);
        log("deactivating user");
        NetworkManager.getInstance().addToQueue(r);
    }

    public static Student updateUser(Student student){
        String link = UPDATE_USER;
        SilentConnectionRequest r = new SilentConnectionRequest();
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

    public static void sendDeviceID(String username, String pwd, String deviceId){
        SilentConnectionRequest r = new SilentConnectionRequest();
        r.setUrl(CREATE_USER);
        r.addArgument("username", username);
        r.addArgument("pwd", pwd);
        r.addArgument("deviceId", deviceId);
        r.addArgument("secret",SECRETKEY);
        NetworkManager.getInstance().addToQueue(r);
    }
}
