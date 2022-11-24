package com.collabnote.server.collaborate;

import java.util.HashMap;

import org.apache.commons.lang3.RandomStringUtils;

public class CollaborateDatabase {
    private HashMap<String, Collaborate> collaborateDatabase;

    public CollaborateDatabase() {
        this.collaborateDatabase = new HashMap<>();
    }

    public String create() {
        String shareID = RandomStringUtils.randomAlphanumeric(6);
        Collaborate collaborate = new Collaborate(shareID);
        this.collaborateDatabase.put(shareID, collaborate);
        return shareID;
    }

    public Collaborate get(String shareID) {
        return this.collaborateDatabase.get(shareID);
    }

}
