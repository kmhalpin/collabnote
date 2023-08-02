package com.collabnote.crdt;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.model.Factory;
import guru.nidi.graphviz.model.Node;

public class CRDTItem {
    public String content;
    public CRDTID id;
    private CRDTItem originLeft;
    private CRDTItem originRight;
    public boolean isDeleted;
    public CRDTItem left;
    public CRDTItem right;

    public CRDTItem(String content, CRDTID id, boolean isDeleted,
            CRDTItem left, CRDTItem right) {
        this.content = content;
        this.id = id;
        this.originLeft = null;
        this.originRight = null;
        this.isDeleted = isDeleted;
        this.left = left;
        this.right = right;
    }

    public CRDTItemSerializable serialize() {
        return new CRDTItemSerializable(this.content,
                this.id,
                this.originLeft == null ? null : this.originLeft.id,
                this.originRight == null ? null : this.originRight.id, isDeleted, false);
    }

    public CRDTItem getOriginLeft() {
        return originLeft;
    }

    public void setOrigin(CRDTItem originLeft, CRDTItem originRight) {
        this.originLeft = originLeft;
        this.originRight = originRight;
    }

    public CRDTItem getOriginRight() {
        return originRight;
    }

    public void changeOriginLeft(CRDTItem originLeft) {
        this.originLeft = originLeft;
    }

    public void changeOriginRight(CRDTItem originRight) {
        this.originRight = originRight;
    }

    public Node renderNode() {
        Node node = Factory.node(this.id.agent + "-" + this.id.seq).with(Label.of(content));
        if (this.isDeleted)
            node = node.with(Color.RED);
        return node;
    }

}
