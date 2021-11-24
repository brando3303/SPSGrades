package com.brandon.scraper;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.util.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.codename1.ui.CN.log;

public class Deserializer {

    private static final String COURSES_LINK = "https://the-source-scraper.herokuapp.com/get_courses?username=USERNAME&pwd=PASSWORD";
    private static final String LAST_COURSES_LINK = "https://the-source-scraper.herokuapp.com/get_last_courses?username=USERNAME";
    private static final String ASSIGNMENTS_LINK = "https://the-source-scraper.herokuapp.com/get_assignments?username=USERNAME&pwd=PASSWORD&frn=FRN";
    private static final String LAST_ASSIGNMENTS_LINK = "https://the-source-scraper.herokuapp.com/get_last_assignments?username=USERNAME&frn=FRN";
    private static final String INBOX_LINK = "https://the-source-scraper.herokuapp.com/get_inbox?username=USERNAME";
    private static final String DELETE_INBOX_ITEM_LINK = "https://the-source-scraper.herokuapp.com/delete_inbox_item?username=USERNAME&id=ID";

    //takes in a username and password, sends a get request to the "LINK" address, and then deserializes the Json Byte InputStream into a student class
    public static Student getStudent(String user, String pass){
        //puts the username and password into the LINK
        String link = StringUtil.replaceAll(StringUtil.replaceAll(COURSES_LINK,"USERNAME",user),"PASSWORD",pass);

        //sends a get request to the link and gets a list of courses in the form of LinkedHashMaps
        ArrayList<LinkedHashMap<String, Object>> courseList;
        try {
            ConnectionRequest r = new ConnectionRequest();
            r.setPost(false);
            r.setUrl(link);
            NetworkManager.getInstance().addToQueueAndWait(r);
            Map<String, Object> result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            courseList = (ArrayList<LinkedHashMap<String, Object>>) result.get("courses");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return createStudentFromMapArray(courseList,user,pass);

    }


    //this function is currently only present in the method above, but may be useful if getting a student from the local storage
    private static Student createStudentFromMapArray(ArrayList<LinkedHashMap<String,Object>> courseList, String user, String pass){
        //deserializes the hashmaps into the Student class
        Student student = new Student(user,pass);
        for (LinkedHashMap<String, Object> courseMap : courseList) {
            Course course = new Course();
            course.period = (Double) (courseMap.get("period"));
            course.absences = (double) (courseMap.get("absences"));
            course.tardies = (double) (courseMap.get("tardies"));

            LinkedHashMap<String, Object> courseInfo = (LinkedHashMap<String, Object>) courseMap.get("courseInfo");
            LinkedHashMap<String, Object> semester1Grade = (LinkedHashMap<String, Object>) courseMap.get("semester1Grade");

            course.courseInfo.name = (String) courseInfo.get("name");
            course.courseInfo.teacher = (String) courseInfo.get("teacher");
            course.courseInfo.room = (double) courseInfo.get("room");

            if (semester1Grade != null) {
                course.semester1Grade.letter = (String) semester1Grade.get("letter");
                course.semester1Grade.percent = (String) semester1Grade.get("percent");
            } else {
                course.semester1Grade.letter = "NA";
                course.semester1Grade.percent = "NA";
            }

            course.frn = (String) (courseMap.get("frn"));

            student.courses.add(course);
        }

        return student;

    }

    public static ArrayList<Assignment> getClassAssignments(Course course, Student student){
        //puts the username and password and frn into the LINK
        String link = StringUtil.replaceAll(
                StringUtil.replaceAll(
                        StringUtil.replaceAll(ASSIGNMENTS_LINK,
                                "USERNAME",student.getUsername()),
                        "PASSWORD",student.getPassword()),
                "FRN", course.frn);

        //sends a get request to the link and gets a list of courses in the form of LinkedHashMaps
        ArrayList<LinkedHashMap<String, Object>> assignmentList;
        try {
            ConnectionRequest r = new ConnectionRequest();
            r.setPost(false);
            r.setUrl(link);

            course.addInfiniteProgressBar();

            NetworkManager.getInstance().addToQueueAndWait(r);
            course.removeInfiniteProgressBar();

            Map<String, Object> result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            assignmentList = (ArrayList<LinkedHashMap<String, Object>>) result.get("assignments");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return hashMapListToAssignmentList(assignmentList);

    }

    public static Inbox getInboxItems(Student student){
        String link = StringUtil.replaceAll(INBOX_LINK, "USERNAME",student.getUsername());
        Map<String,Object> result;

        ConnectionRequest r = new ConnectionRequest();
        r.setPost(false);
        r.setUrl(link);

        NetworkManager.getInstance().addToQueueAndWait(r);
        try {
            result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        Inbox inbox = new Inbox();

        Set<String> set = result.keySet();
        log("" + set.size());
        for(String s : set){
            log("ID: " + s);
            Object o = result.get(s);
            InboxItem item = new InboxItem();

            item.index = s;

            item.courseName = (String)((LinkedHashMap<String,Object>)o).get("courseName");
            item.gradeBefore = (String)((LinkedHashMap<String,Object>)o).get("gradeBefore");
            item.gradeNow = (String)((LinkedHashMap<String,Object>)o).get("gradeNow");
            item.time = (Double)((LinkedHashMap<String,Object>)o).get("time");
            item.deleted = ((LinkedHashMap<String,Object>)o).get("deleted") == "true";


            inbox.inboxItems.add(item);
        }
        for(InboxItem ii : inbox.inboxItems){
            for(Course c : student.courses){
                if(ii.courseName.equals(c.courseInfo.name)){
                    ii.course = c;
                }
            }
        }
        return inbox;


    }

    public static void deleteInboxItem(InboxItem inboxItem,Student student){
        String link = StringUtil.replaceAll(StringUtil.replaceAll(DELETE_INBOX_ITEM_LINK,"USERNAME",student.getUsername()),"ID",inboxItem.index);
        ConnectionRequest r = new ConnectionRequest();
        r.setPost(true);
        r.setUrl(link);
        r.addArgument("username", student.getUsername());
        r.addArgument("id", inboxItem.index);
        log(NetworkManager.getInstance().isQueueIdle() ? "queue idle": "queue is not idle");
        NetworkManager.getInstance().addToQueue(r);
    }

    public static ArrayList<Assignment> hashMapListToAssignmentList(ArrayList<LinkedHashMap<String,Object>> assignmentList){
        ArrayList<Assignment> assignments = new ArrayList<>();
        for(LinkedHashMap<String,Object> aHash : assignmentList){
            Assignment assignment = new Assignment();
            assignment.name = (String)aHash.get("name");
            assignment.total = (double)aHash.get("total");
            assignment.points = (Double)aHash.get("points");

            assignments.add(assignment);
        }

        return assignments;
    }

    public static void registerForPush(String deviceID){


    }

    //this function goes from an ArrayList<Map> to a Student Class, but does not include adding a username and password,
    // as that was originally meant to avoid needing to send requests. That turned out to be pretty important  for testing however...
    @Deprecated
    private static Student deserialize(ArrayList<LinkedHashMap<String, Object>> mapList){
        Student student = new Student(null,null);
        for(int i = 0; i< mapList.size();i++){
            Course course = new Course();
            course.period = (double)(mapList.get(i).get("period"));
            course.absences = (double)(mapList.get(i).get("absences"));
            course.tardies = (double)(mapList.get(i).get("tardies"));

            LinkedHashMap<String,Object> courseInfo = (LinkedHashMap<String,Object>)mapList.get(i).get("courseInfo");
            LinkedHashMap<String,Object> semester1Grade = (LinkedHashMap<String,Object>)mapList.get(i).get("semester1Grade");

            course.courseInfo.name = (String)courseInfo.get("name");
            course.courseInfo.teacher = (String)courseInfo.get("teacher");
            course.courseInfo.room = (double)courseInfo.get("room");

            if(semester1Grade != null){
                course.semester1Grade.letter = (String)semester1Grade.get("letter");
                course.semester1Grade.percent = (String)semester1Grade.get("percent");
            } else{
                course.semester1Grade.letter = "NA";
                course.semester1Grade.percent = "NA";
            }

            course.frn = (String)(mapList.get(i).get("frn"));

            if(mapList.get(i).get("assignments") != null) {
                try {
                    ArrayList<LinkedHashMap<String, Object>> assignmentHashArray = (ArrayList<LinkedHashMap<String, Object>>) mapList.get(i).get("assignments");


                    for (LinkedHashMap<String, Object> stringObjectLinkedHashMap : assignmentHashArray) {
                        Assignment assignment = new Assignment();
                        assignment.name = (String) stringObjectLinkedHashMap.get("name");
                        assignment.points = (Double) stringObjectLinkedHashMap.get("points");
                        assignment.total = (double) stringObjectLinkedHashMap.get("total");
                        log(assignment.name);

                        course.assignments.add(assignment);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            course.createCoursePage();
            student.courses.add(course);
        }

        return student;

    }



    //|--------------------------------------|
    //|Everything below is for test purposes.|
    //|--------------------------------------|
//    public static Student getStudentFromFile(String path){
//        try {
//            InputStreamReader stream = new InputStreamReader(Display.getInstance().getResourceAsStream(null,path), "UTF-8");
//            Map<String, Object> result = new JSONParser().parseJSON(stream);
//            log("successfully got test student");
//            return Deserializer.deserialize((ArrayList<LinkedHashMap<String, Object>>) result.get("courses"));
//        }catch(Exception e){
//            e.printStackTrace();
//            log("testStudentFile was incorrect");
//        }
//        return null;
//
//    }


}
