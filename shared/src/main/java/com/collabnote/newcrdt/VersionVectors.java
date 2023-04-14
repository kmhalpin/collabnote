package com.collabnote.newcrdt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

// optimized vector clocks
public class VersionVectors {
    HashMap<Integer, ArrayList<CRDTItem>> versionVector;

    public VersionVectors() {
        this.versionVector = new HashMap<>();
    }

    public void remove(CRDTItem item) {
        ArrayList<CRDTItem> version = this.versionVector.get(item.id.agent);
        version.remove(item);
    }

    // optimize with binary search
    public void recover(CRDTItem item) {
        ArrayList<CRDTItem> version = this.versionVector.get(item.id.agent);
        for (int i = 0; i < version.size(); i++) {
            if (version.get(i).id.seq > item.id.seq) {
                version.add(i, item);
                break;
            }
        }
    }

    public void put(CRDTItem item) {
        ArrayList<CRDTItem> version = this.versionVector.get(item.id.agent);
        if (version == null) {
            version = new ArrayList<>();
            this.versionVector.put(item.id.agent, version);
        }
        version.add(item);
    }

    public int get(int agent) {
        ArrayList<CRDTItem> version = this.versionVector.get(agent);
        if (version == null) {
            return 0;
        }
        return version.get(version.size() - 1).id.seq;
    }

    public boolean exists(CRDTID id) {
        return this.get(id.agent) >= id.seq;
    }

    // can be improved with binary search, version sorted by id.clock / (index + 1)
    public CRDTItem find(CRDTID id) {
        ArrayList<CRDTItem> version = this.versionVector.get(id.agent);
        for (int i = 0; i < version.size(); i++) {
            CRDTItem item = version.get(i);
            if (item.id.equals(id)) {
                return item;
            }
        }

        throw new NoSuchElementException("unexpected");
    }
}
