package com.collabnote.client.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.collabnote.client.data.entity.DocumentEntity;
import com.collabnote.client.data.entity.DocumentEntitySerializable;

public class DocumentModel {
    public void saveFile(DocumentEntity documentEntity, File targetFile) {
        DocumentEntitySerializable serializedDocument = documentEntity.serialize();

        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);) {
            objectOutputStream.writeObject(serializedDocument);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFile(DocumentEntity entity, File targetFile) {
        try (FileInputStream fileInputStream = new FileInputStream(targetFile);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);) {
            DocumentEntitySerializable document = (DocumentEntitySerializable) objectInputStream.readObject();
            document.deserialize(entity);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
