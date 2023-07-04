package com.collabnote.crdt;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

// optimized vector clocks
public class VersionVectors implements Serializable {
    public HashMap<Integer, ArrayList<CRDTItem>> versionVector;

    public VersionVectors() {
        this.versionVector = new HashMap<>();
    }

    public void remove(CRDTItem item) {
        ArrayList<CRDTItem> version = this.versionVector.get(item.id.agent);
        // cannot remove last version
        if (version.get(version.size() - 1) != item)
            version.remove(item);
    }

    // optimize with binary search
    public void recover(CRDTItem item) {
        ArrayList<CRDTItem> version = this.versionVector.get(item.id.agent);
        if (version == null) {
            version = new ArrayList<>();
            this.versionVector.put(item.id.agent, version);
        }
        for (int i = 0; i < version.size(); i++) {
            if (version.get(i).id.seq >= item.id.seq) {
                version.add(i, item);
                return;
            }
        }
        version.add(item);
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
        if (version == null || version.size() == 0) {
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
            if (item == null)
                break;
            if (item.id.equals(id)) {
                return item;
            }
        }

        throw new NoSuchElementException("unexpected");
    }

    public ArrayList<CRDTItemSerializable> serialize() {
        ArrayList<CRDTItemSerializable> items = new ArrayList<>();
        for (Entry<Integer, ArrayList<CRDTItem>> entry : this.versionVector.entrySet()) {
            int nextseq = 1;
            for (CRDTItem i : entry.getValue()) {
                for (int it = nextseq; it < i.id.seq; it++) { // populate removed item with gc item
                    items.add(new CRDTItemSerializable(
                            null,
                            new CRDTID(entry.getKey(), it),
                            null,
                            null,
                            true,
                            true));
                }
                items.add(i.serialize());
                nextseq = i.id.seq + 1;
            }
        }
        return items;
    }
}
