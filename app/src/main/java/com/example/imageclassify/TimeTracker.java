package com.example.imageclassify;

public class TimeTracker {
    private long timeElapsed;

    public long getTimeElapsed(){
        return timeElapsed;
    }

    public void setTimeElapsed(long startTime, long endTime){
        timeElapsed = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds
    }


}
