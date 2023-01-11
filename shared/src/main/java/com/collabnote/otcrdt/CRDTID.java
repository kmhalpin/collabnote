package com.collabnote.otcrdt;

import java.io.Serializable;

public class CRDTID implements Serializable {
    String agent;
    int seq;

    CRDTID(String agent, int seq) {
        this.agent = agent;
        this.seq = seq;
    }

    boolean equals(CRDTID a) {
        return a == this || (a != null && this != null && a.agent.equals(this.agent) && a.seq == this.seq);
    }

    boolean equals(String agent, int seq) {
        return this != null && agent.equals(this.agent) && seq == this.seq;
    }
}
