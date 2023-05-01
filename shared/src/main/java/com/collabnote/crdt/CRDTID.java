package com.collabnote.crdt;

import java.io.Serializable;

public class CRDTID implements Serializable {
    public int agent;
    public int seq;

    public CRDTID(int agent, int seq) {
        this.agent = agent;
        this.seq = seq;
    }

    public boolean equals(CRDTID a) {
        return a == this || (a != null && this != null && a.agent == this.agent && a.seq == this.seq);
    }

    public boolean equals(int agent, int seq) {
        return this != null && agent == this.agent && seq == this.seq;
    }
}
