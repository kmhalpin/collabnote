package com.collabnote.newcrdt;

import java.rmi.UnexpectedException;

public class Position {
    CRDTItem left;
    CRDTItem right;
    int index;

    public Position(CRDTItem left, CRDTItem right, int index) {
        this.left = left;
        this.right = right;
        this.index = index;
    }

    public void forward() throws UnexpectedException {
        if (this.right == null) {
            throw new UnexpectedException("null");
        }
        if (!this.right.isDeleted) {
            this.index += 1;
        }
        this.left = this.right;
        this.right = this.right.right;
    }
}
