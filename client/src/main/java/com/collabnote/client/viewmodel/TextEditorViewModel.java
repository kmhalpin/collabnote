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
    private HashMap<Integer, Object> userCarets;

    private CollaborationRepository collaborationRepository;
    private DocumentModel documentModel;

    // data listener
    private TextEditorViewModelCaretListener caretListener;
    private TextEditorViewModelCollaborationListener collaborationListener;

    private DocumentEntity documentEntity;

    public TextEditorViewModel(CRDTDocument document) {
        this.collaborationRepository = new CollaborationRepository();
        this.documentModel = new DocumentModel();
        this.initDocument(document);
    }

    public void setCollaborationListener(TextEditorViewModelCollaborationListener collaborationListener) {
        this.collaborationListener = collaborationListener;
    }

    public void setCaretListener(TextEditorViewModelCaretListener caretListener) {
        this.caretListener = caretListener;
    }

    // create new document
    public DocumentEntity initDocument(CRDTDocument document) {
        if (this.collaborationRepository.isConnected())
            this.collaborationRepository.closeConnection();

        this.documentEntity = new DocumentEntity(new GCCRDT(this.agent, document, this));
        this.userCarets = new HashMap<>();

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
        this.documentModel.loadFile(entity, targetFile);

        if (entity.isCollaborating() && !this.collaborationRepository.isConnected()) {
            toggleConnection(document);
        }
    }

    public void toggleConnection(CRDTDocument document) {
        if (this.documentEntity == null) {
            return;
        }

        DocumentEntity oldEntity = this.documentEntity;

        if (oldEntity.isCollaborating()) {
            if (this.collaborationRepository.isConnected()) {
                this.collaborationRepository.closeConnection();
            } else {
                ArrayList<CRDTItemSerializable> operationBuffer = oldEntity.getOperationBuffer();

                // recreate document to re integrate
                DocumentEntity entity = initDocument(document);
                entity.setCollaboration(oldEntity.getShareID(), oldEntity.getServerHost(), operationBuffer);

                this.collaborationRepository.connectCRDT(operationBuffer,
                        entity.getServerHost(),
                        entity.getShareID(), this.agent, this);
            }
        }
    }

    public void shareDocument(String host) {
        if (this.documentEntity == null) {
            return;
        }

        this.documentEntity.setCollaboration(null, host, new ArrayList<>());

        collaborationRepository.shareCRDT(this.documentEntity.getCrdtReplica(), host, this.agent, this);
    }

    public void connectDocument(CRDTDocument document, String host, String shareID) {
        DocumentEntity entity = initDocument(document);

        entity.setCollaboration(shareID, host, new ArrayList<>());

        collaborationRepository.connectCRDT(entity.getOperationBuffer(), host, shareID, this.agent, this);
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
                if (this.caretListener == null)
                    return;

                int index = data.getCaretIndex();
                int agent = data.getAgent();

                if (index == -1) {
                    Object tag = this.userCarets.remove(agent);
                    if (tag != null)
                        this.caretListener.removeCaretListener(tag);
                    return;
                }

                Object tag = this.userCarets.get(agent);
                if (tag != null)
                    this.caretListener.removeCaretListener(tag);
                try {
                    this.userCarets.put(agent, this.caretListener.addCaretListener(index));
                } catch (BadLocationException e1) {
                }
                break;
            case DELETE:
                this.documentEntity.getCrdtReplica().tryRemoteDelete(data.getCrdtItem());
                break;
            case INSERT:
                this.documentEntity.getCrdtReplica().tryRemoteInsert(data.getCrdtItem());
                break;
            // acknowledge sent
            case DONE:
                for (int i = 0; i < this.documentEntity.getOperationBuffer().size(); i++) {
                    if (this.documentEntity.getOperationBuffer().get(i).id
                            .equals(data.getCrdtItem().id)) {
                        this.documentEntity.getOperationBuffer().remove(i);
                    }
                }
                break;
            // after connected
            case CONNECT:
                this.collaborationListener.collaborationStatusListener(true);
                break;
            // after shared
            case SHARE:
                this.documentEntity.setShareID(data.getShareID());
                this.collaborationListener.collaborationStatusListener(true);
                break;
            case GC:
                boolean success = ((GCCRDT) this.documentEntity.getCrdtReplica()).GC(data.getCrdtList());
                if (success) {
                    this.collaborationRepository.sendGCAck(this.documentEntity.getShareID());
                }
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
        this.collaborationListener.collaborationStatusListener(false);
        this.userCarets = new HashMap<>();
    }
}
