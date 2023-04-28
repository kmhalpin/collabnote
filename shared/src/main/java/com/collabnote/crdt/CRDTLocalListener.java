package com.collabnote.crdt;

import java.util.List;

public interface CRDTLocalListener {
    public void afterLocalCRDTInsert(CRDTItem item);
    public void afterLocalCRDTDelete(List<CRDTItem> item);

}
