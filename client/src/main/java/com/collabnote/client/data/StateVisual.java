package com.collabnote.client.data;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import com.collabnote.client.data.entity.DocumentEntity;
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

public class StateVisual implements Closeable {
    static {
        Graphviz.useEngine(new GraphvizCmdLineEngine());
    }

    DocumentEntity entity;
    Thread visualizerThread;
    boolean closed = false;
    boolean render = true;

    public StateVisual(DocumentEntity entity, StateVisualListener listener) {
        this.entity = entity;
        this.visualizerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    do {
                        if (render) {
                            render = false;
                            listener.updateImage(generateGraph());
                        }
                        Thread.sleep(2000);
                    } while (!closed);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                closed = true;
                visualizerThread = null;
            }

        });
        this.visualizerThread.start();
    }

    public void triggerRender() {
        this.render = true;
    }

    public byte[] generateGraph() throws IOException {
        ArrayList<LinkSource> pureNodeList = new ArrayList<>();

        Node nullStart = node("null start"), nullEnd = node("null end");

        CRDTItem o = this.entity.getCrdtReplica().getStart();
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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Graphviz.fromGraph(g).render(Format.PNG).toOutputStream(outputStream);

        return outputStream.toByteArray();
    }

    @Override
    public void close() throws IOException {
        this.closed = true;
    }
}
