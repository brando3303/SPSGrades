package com.brandon.scraper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class Inbox {
    ArrayList<InboxItem> inboxItems;

    public Inbox(){
        inboxItems = new ArrayList<>();
    }

    public void sortInboxItems(){
        long[] epochData = new long[inboxItems.size()];

        for(int i = 0; i < inboxItems.size(); i++){
            epochData[i] = inboxItems.get(i).time.longValue();
        }
        Arrays.sort(epochData);
        ArrayList<InboxItem> sortedInboxItems = new ArrayList<>();
        for(long e : epochData){
            for(Iterator<InboxItem> iterator = inboxItems.listIterator(); iterator.hasNext();){
                InboxItem a = iterator.next();
                if(e == a.time.longValue()){
                    sortedInboxItems.add(a);
                    inboxItems.remove(a);
                    break;
                }
            }
        }
        Collections.reverse(sortedInboxItems);
        inboxItems = sortedInboxItems;

    }

    public int getNumberOfUndeletedInboxItems(){
        int i = 0;
        for(InboxItem ii : inboxItems){
            if(!ii.deleted){
                i++;
            }
        }
        return i;
    }
}
