package com.collabnote.client.viewmodel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.RandomUtils;

import com.collabnote.client.data.CollaborationRepository;
import com.collabnote.client.data.DocumentModel;
import com.collabnote.client.data.entity.DocumentEntity;
import com.collabnote.client.socket.ClientSocketListener;
import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.crdt.gc.GCCRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.CRDTLocalListener;
import com.collabnote.socket.DataPayload;

public class TextEditorViewModel implements CRDTLocalListener, ClientSocketListener {
    // user id
    private int agent = RandomUtils.nextInt();

    private CollaborationRepository collaborationRepository;
    private DocumentModel documentModel;
    private TextEditorViewModelListener listener;

    private DocumentEntity documentEntity;

    public TextEditorViewModel(CRDTDocument document) {
        this.collaborationRepository = new CollaborationRepository();
        this.documentModel = new DocumentModel();
        this.initDocument(document);
    }

    public void setListener(TextEditorViewModelListener listener) {
        this.listener = listener;
    }

    // create new document
    public DocumentEntity initDocument(CRDTDocument document) {
        this.documentEntity = new DocumentEntity(new GCCRDT(0, document, this));

        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        document.setCrdt(this.documentEntity.getCrdtReplica());
        return this.documentEntity;
    }

    public void saveDocument(File targetFile) {
        if (this.documentEntity == null) {
            return;
        }

        this.documentModel.saveFile(this.documentEntity, targetFile);
    }

    public void loadDocument(CRDTDocument document, File targetFile) {
        DocumentEntity entity = initDocument(document);
        this.documentModel.loadFile(entity.getCrdtReplica(), targetFile);

        if (entity.isCollaborating()) {
            collaborationRepository.connectCRDT(entity.getServerHost(), entity.getShareID(), this.agent, this);
        }
    }

    public void shareDocument(String host) {
        if (this.documentEntity == null) {
            return;
        }

        this.documentEntity.setCollaboration(null, host, new HashMap<>(), new ArrayList<>());

        collaborationRepository.shareCRDT(this.documentEntity.getCrdtReplica(), host, this.agent, this);
    }

    public void connectDocument(CRDTDocument document, String host, String shareID) {
        DocumentEntity entity = initDocument(document);

        entity.setCollaboration(shareID, host, new HashMap<>(), new ArrayList<>());

        collaborationRepository.connectCRDT(host, shareID, this.agent, this);
    }

    public void updateCaret(int index) {
        if (!this.documentEntity.isCollaborating())
            return;

        this.collaborationRepository.sendCaret(this.documentEntity.getShareID(), index);
    }

    // local CRDT update listener
    @Override
    public void afterLocalCRDTInsert(CRDTItem item) {
        if (!this.documentEntity.isCollaborating())
            return;

        CRDTItemSerializable serializedItem = item.serialize();

        this.documentEntity.getOperationBuffer().add(serializedItem);
        // might try to send in network thread by the queue
        this.collaborationRepository.sendInsert(this.documentEntity.getShareID(), serializedItem);
    }

    @Override
    public void afterLocalCRDTDelete(List<CRDTItem> item) {
        if (!this.documentEntity.isCollaborating())
            return;

        for (CRDTItem i : item) {
            CRDTItemSerializable serializedItem = i.serialize();

            this.documentEntity.getOperationBuffer().add(serializedItem);
            this.collaborationRepository.sendDelete(this.documentEntity.getShareID(), serializedItem);
        }
    }

    // socket listener
    @Override
    public void onStart() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onReceiveData(DataPayload data) {
        switch (data.getType()) {
            case CARET:
                if (this.listener == null)
                    return;

                int index = data.getCaretIndex();
                int agent = data.getAgent();

                if (index == -1) {
                    Object tag = this.documentEntity.getUserCarets().remove(agent);
                    if (tag != null)
                        this.listener.removeCaretListener(tag);
                    return;
                }

                Object tag = this.documentEntity.getUserCarets().get(agent);
                if (tag != null)
                    this.listener.removeCaretListener(tag);
                try {
                    this.documentEntity.getUserCarets().put(agent, this.listener.addCaretListener(index));
                } catch (BadLocationException e1) {
                }
                break;
            case DELETE:
                this.documentEntity.getCrdtReplica().tryRemoteDelete(data.getCrdtItem());
                break;
            case INSERT:
                this.documentEntity.getCrdtReplica().tryRemoteInsert(data.getCrdtItem());
                break;
            case DONE:
                this.documentEntity.getOperationBuffer().remove(data.getCrdtItem());
                break;
            case CONNECT:
                break;
            case SHARE:
                this.documentEntity.setShareID(data.getShareID());
                break;
            case GC:
                ((GCCRDT) this.documentEntity.getCrdtReplica()).GC(data.getCrdtList());
                break;
            case RECOVER:
                ((GCCRDT) this.documentEntity.getCrdtReplica()).recover(data.getCrdtList(), data.getCrdtItem());
                break;
            default:
                break;
        }
    }

    @Override
    public void onFinished() {
        // TODO Auto-generated method stub
    }
}