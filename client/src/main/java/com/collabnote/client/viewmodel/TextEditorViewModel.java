package com.collabnote.client.viewmodel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.text.BadLocationException;

import org.apache.commons.lang3.RandomUtils;

import com.collabnote.client.data.CollaborationRepository;
import com.collabnote.client.data.DocumentModel;
import com.collabnote.client.data.StateVisual;
import com.collabnote.client.data.StateVisualListener;
import com.collabnote.client.data.entity.DocumentEntity;
import com.collabnote.client.socket.ClientSocketListener;
import com.collabnote.client.ui.document.CRDTDocument;
import com.collabnote.crdt.gc.DeleteGroupSerializable;
import com.collabnote.crdt.gc.GCCRDT;
import com.collabnote.crdt.CRDT;
import com.collabnote.crdt.CRDTItem;
import com.collabnote.crdt.CRDTItemSerializable;
import com.collabnote.crdt.CRDTLocalListener;
import com.collabnote.socket.DataPayload;
import com.collabnote.socket.Type;

public class TextEditorViewModel implements CRDTLocalListener, ClientSocketListener {
    // user id
    private int agent = RandomUtils.nextInt();
    private CRDTDocument document;
    private HashMap<Integer, Object> userCarets;

    private CollaborationRepository collaborationRepository;
    private DocumentModel documentModel;

    // data listener
    private TextEditorViewModelCaretListener caretListener;
    private TextEditorViewModelCollaborationListener collaborationListener;
    private TextEditorViewModelImageListener imageListener;

    private DocumentEntity documentEntity;

    private StateVisual stateVisual;

    public TextEditorViewModel(CRDTDocument document) {
        this.collaborationRepository = new CollaborationRepository();
        this.documentModel = new DocumentModel();
        this.document = document;
        this.initDocument();
    }

    // set listeners
    public void setCollaborationListener(TextEditorViewModelCollaborationListener collaborationListener) {
        this.collaborationListener = collaborationListener;
    }

    public void setCaretListener(TextEditorViewModelCaretListener caretListener) {
        this.caretListener = caretListener;
    }

    public void setImageListener(TextEditorViewModelImageListener imageListener) {
        this.imageListener = imageListener;
    }

    public void setStateVisualizer() throws IOException {
        if (this.stateVisual != null) {
            unsetStateVisualizer();
            return;
        }

        if (this.documentEntity == null || this.imageListener == null)
            return;

        this.stateVisual = new StateVisual(this.documentEntity.getCrdtReplica(), new StateVisualListener() {

            @Override
            public void updateImage(byte[] image) {
                imageListener.updateImage(image);
            }

        });
    }

    public void unsetStateVisualizer() {
        if (this.stateVisual == null)
            return;

        try {
            this.stateVisual.close();
            this.stateVisual = null;
            imageListener.updateImage(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // create new document
    public DocumentEntity initDocument() {
        if (this.collaborationRepository.isConnected())
            this.collaborationRepository.closeConnection();

        this.documentEntity = new DocumentEntity(createCRDTReplica());
        this.userCarets = new HashMap<>();

        this.document.bindCrdt(this.documentEntity.getCrdtReplica());
        return this.documentEntity;
    }

    public CRDT createCRDTReplica() {
        if (this.stateVisual != null)
            unsetStateVisualizer();
        return new GCCRDT(this.agent, this.document, this);
    }

    // save load
    public void saveDocument(File targetFile) {
        if (this.documentEntity == null) {
            return;
        }

        this.documentModel.saveFile(this.documentEntity, targetFile);
    }

    public void loadDocument(File targetFile) {
        DocumentEntity entity = initDocument();
        this.documentModel.loadFile(entity, targetFile);

        if (entity.isCollaborating() && !this.collaborationRepository.isConnected()) {
            toggleConnection();
        }
    }

    // offline online toggle
    public void toggleConnection() {
        if (this.documentEntity == null) {
            return;
        }

        if (this.documentEntity.isCollaborating()) {
            if (this.collaborationRepository.isConnected()) {
                this.collaborationRepository.closeConnection();
            } else {
                this.collaborationRepository.connectCRDT(this.documentEntity.getOperationBuffer(),
                        this.documentEntity.getServerHost(),
                        this.documentEntity.getShareID(), this.agent, this);
            }
        }
    }

    // collaboration
    public void shareDocument(String host) {
        if (this.documentEntity == null) {
            return;
        }

        this.documentEntity.setCollaboration(null, host, new ArrayList<>());

        collaborationRepository.shareCRDT(this.documentEntity.getCrdtReplica(), host, this.agent, this);
    }

    public void connectDocument(String host, String shareID) {
        DocumentEntity entity = initDocument();

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
        if (this.stateVisual != null)
            this.stateVisual.triggerRender();

        if (!this.documentEntity.isCollaborating())
            return;

        CRDTItemSerializable serializedItem = item.serialize();

        this.documentEntity.getOperationBuffer().add(serializedItem);
        // might try to send in network thread by the queue
        this.collaborationRepository.sendInsert(this.documentEntity.getShareID(), serializedItem);
    }

    @Override
    public void afterLocalCRDTDelete(List<CRDTItem> item) {
        if (this.stateVisual != null)
            this.stateVisual.triggerRender();

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
                this.documentEntity.setShareID(data.getShareID());
                this.collaborationListener.collaborationStatusListener(true);
                break;
            // server going to share to client
            case SHARE:
                // empty replica
                this.documentEntity.setCrdtReplica(createCRDTReplica());
                this.document.bindCrdt(this.documentEntity.getCrdtReplica());
                break;
            // gc crdt protocol
            case GC:
                List<DeleteGroupSerializable> success = ((GCCRDT) this.documentEntity.getCrdtReplica())
                        .GC(data.getDeleteGroupList());
                if (success.size() > 0) {
                    this.collaborationRepository.sendGCAck(this.documentEntity.getShareID(), success);
                }
                break;
            case RECOVER:
                ((GCCRDT) this.documentEntity.getCrdtReplica()).recover(data.getDeleteGroupList(), data.getCrdtItem());
                break;
            default:
                break;
        }
        if (this.stateVisual != null
                && (data.getType() == Type.INSERT
                        || data.getType() == Type.DELETE
                        || data.getType() == Type.GC
                        || data.getType() == Type.RECOVER))
            this.stateVisual.triggerRender();
    }

    @Override
    public void onFinished() {
        this.collaborationListener.collaborationStatusListener(false);
        this.userCarets = new HashMap<>();
    }
}
