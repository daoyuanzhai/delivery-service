package com.dzhai.deliverservice.util;

import lombok.val;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Util {
    // function to sort hashmap by values
    public HashMap<String, Double> sortByValue(HashMap<String, Double> hm)
    {
        // Create a list from elements of HashMap
        val list = new LinkedList<Map.Entry<String, Double> >(hm.entrySet());

        // Sort the list
        Collections.sort(list, (o1, o2) -> (o1.getValue()).compareTo(o2.getValue()));

        // put data from sorted list to hashmap
        val temp = new LinkedHashMap<String, Double>();
        for (Map.Entry<String, Double> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }
}
