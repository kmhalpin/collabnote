package com.collabnote.client.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.LinkSource;
import guru.nidi.graphviz.model.Node;

import static guru.nidi.graphviz.model.Factory.*;

public class OpsResultRender {
    static {
        Graphviz.useEngine(new GraphvizCmdLineEngine());
    }

    static void render(CRDT replica, File file) throws IOException {
        ArrayList<LinkSource> pureNodeList = new ArrayList<>();

        Node nullStart = node("null start"), nullEnd = node("null end");

        CRDTItem o = replica.getStart();
        while (o != null) {
            pureNodeList.add(o.renderNode().link(
                    to(o.getOriginLeft() != null ? o.getOriginLeft().renderNode() : nullStart).with(Style.BOLD,
                            Color.RED,
                            Label.of("ol")),
                    to(o.getOriginRight() != null ? o.getOriginRight().renderNode() : nullEnd).with(Style.BOLD,
                            Color.RED,
                            Label.of("or")),
                    o.right != null ? o.right.renderNode() : nullEnd));
            o = o.right;
        }
        pureNodeList.add(0, nullStart);
        pureNodeList.add(nullEnd);
        Graph g = graph("crdt state").directed().with(pureNodeList);

        Graphviz.fromGraph(g).render(Format.PNG).toFile(file);
    }
}
