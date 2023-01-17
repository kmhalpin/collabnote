package com.collabnote.crdt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.util.Pair;

public class CRDTGC {
    CRDTGCListener crdtListener;
    List<CRDTItem> content;
    int length;
    // vector clock
    HashMap<String, Integer> version;
    ArrayList<CRDTItem> WaitListInsert;
    ArrayList<CRDTItem> WaitListDelete;
    ArrayList<CRDTItem> WaitListInsertAck;
    ArrayList<CRDTItem> WaitListDeleteAck;
    ReentrantLock lock = new ReentrantLock();

    public CRDTGC(CRDTGCListener crdtListener) {
        this.crdtListener = crdtListener;
        content = Collections.synchronizedList(new ArrayList<>(1024));
        length = 0;
        version = new HashMap<>();
        WaitListInsert = new ArrayList<>(0);
        WaitListDelete = new ArrayList<>(0);
    }

    boolean foundItem(CRDTItem a, String agent, int seq, boolean atEnd) {
        if (a == null)
            return false;
        return a.id.equals(agent, seq) && (!atEnd || a.value != null);
    }

    public int findRealIndex(CRDTItem needle) throws NoSuchElementException {
        int idx = 0;
        for (CRDTItem item : content) {
            if (needle == item)
                return idx;
            if (!item.isDeleted)
                idx++;
        }
        throw new NoSuchElementException();
    }

    int findItem(CRDTID needle, boolean atEnd, int idx_hint) throws NoSuchElementException {
        if (needle == null)
            return -1;
        String agent = needle.agent;
        int seq = needle.seq;
        if (idx_hint >= 0 && idx_hint < content.size()) {
            CRDTItem hintItem = content.get(idx_hint);
            if (foundItem(hintItem, agent, seq, atEnd)) {
                return idx_hint;
            }
        }
        for (int i = 0; i < content.size(); ++i) {
            if (foundItem(content.get(i), agent, seq, atEnd))
                return i;
        }
        throw new NoSuchElementException();
    }

    int findItem(CRDTID needle, int idx_hint) {
        return findItem(needle, false, idx_hint);
    }

    int findItem(CRDTID needle) {
        return findItem(needle, false, -1);
    }

    int findItemAtPos(int pos, boolean stick_end) throws NoSuchElementException {
        int cntValid = 0;
        int i = 0;
        for (i = 0; i < content.size(); ++i) {
            CRDTItem item = content.get(i);
            if (stick_end && pos == cntValid)
                return i;
            if (item.isDeleted == false && item.value != null) {
                if (pos == cntValid)
                    return i;
                ++cntValid;
            }
        }
        if (pos == cntValid)
            return i;
        throw new NoSuchElementException();
    }

    int findItemAtPos(int pos) {
        return findItemAtPos(pos, false);
    }

    int getNextSeq(String agent) {
        return version.get(agent) == null ? 0 : version.get(agent) + 1;
    }

    CRDTID getItemIDAtPos(int pos) {
        if (pos < 0 || pos >= content.size())
            return null;
        return content.get(pos).id;
    }

    public boolean isInDoc(CRDTID id) {
        if (id == null || id.agent == null)
            return true;
        return version.get(id.agent) != null && version.get(id.agent) >= id.seq;
    }

    boolean shouldInsertNow(CRDTItem item) {
        return !isInDoc(item.id) &&
                (item.id.seq == 0 || isInDoc(new CRDTID(item.id.agent, item.id.seq - 1))) &&
                isInDoc(item.originLeft) && isInDoc(item.originRight);
    }

    void TryClearWaitListInsert() {
        while (WaitListInsert.size() > 0) {
            boolean shouldContinue = false;
            ArrayList<CRDTItem> CannotInsert = new ArrayList<>(0);
            for (CRDTItem item : WaitListInsert) {
                if (shouldInsertNow(item)) {
                    Insert(item, true);
                    shouldContinue = true;
                } else {
                    if (isInDoc(item.id))
                        continue;
                    CannotInsert.add(item);
                }
            }
            WaitListInsert = CannotInsert;
            if (shouldContinue == false)
                break;
        }
    }

    void TryClearWaitListDelete() {
        ArrayList<CRDTItem> CannotDelete = new ArrayList<>(0);
        for (CRDTItem item : WaitListDelete) {
            if (isInDoc(item.id)) {
                Delete(item, true);
            } else {
                CannotDelete.add(item);
            }
        }
        WaitListDelete = CannotDelete;
    }

    public void addInsertOperationToWaitList(CRDTItem item) {
        if (isInDoc(item.id))
            return;
        WaitListInsert.add(item);
        TryClearWaitListInsert();
        TryClearWaitListDelete();
    }

    public void addDeleteOperationToWaitList(CRDTItem item) {
        if (isInDoc(item.id))
            Delete(item, true);
        else
            WaitListDelete.add(item);
    }

    // public CRDTItem localInsert(String agent, int pos, String value) {
    // int i = findItemAtPos(pos);
    // CRDTItem item = new CRDTItem(value,
    // new CRDTID(agent, getNextSeq(agent)),
    // getItemIDAtPos(i - 1),
    // getItemIDAtPos(i),
    // false);
    // integrate(item, i, false);
    // this.WaitListInsertAck.add(item);
    // return item;
    // }

    // public CRDTItem localDelete(String agent, int pos) {
    // int i = findItemAtPos(pos);
    // if (i >= content.size())
    // return null;
    // CRDTItem item = content.get(i);
    // if (!item.isDeleted) {
    // item.isDeleted = true;
    // length--;
    // this.WaitListDeleteAck.add(item);
    // }
    // return item;
    // }

    public void Insert(CRDTItem item, boolean fromWait) {
        integrate(item, -1, fromWait);
    }

    public void Delete(CRDTItem item, boolean fromWait) {
        int pos = findItem(item.id, -1);
        CRDTItem myItem = content.get(pos);
        if (!myItem.isDeleted) {
            myItem.isDeleted = true;
            length--;

            // remove removables
            ArrayList<CRDTItem> remove = new ArrayList<>();

            if (myItem.isRemovable()) {
                remove.add(myItem);

                // decrease reference
                if (myItem.originLeft != null) {
                    int left = findItem(myItem.originLeft);
                    content.get(left).decreaseReference();
                }
                if (myItem.originRight != null) {
                    int right = findItem(myItem.originRight);
                    content.get(right).decreaseReference();
                }
            }

            // find and remove removables
            if (myItem.isRemovable()) {
                for (int left = pos - 1; left >= 0; left--) {
                    CRDTItem leItem = content.get(left);
                    // skip removed item
                    if (leItem.isPermaRemove())
                        continue;

                    // remove removable item
                    if (leItem.isRemovable()) {
                        remove.add(leItem);

                        // decrease reference
                        if (leItem.originLeft != null) {
                            int oleft = findItem(leItem.originLeft);
                            content.get(oleft).decreaseReference();
                        }
                        if (leItem.originRight != null) {
                            int oright = findItem(leItem.originRight);
                            content.get(oright).decreaseReference();
                        }
                    }
                }

                for (int right = pos + 1; right < content.size(); right++) {
                    CRDTItem riItem = content.get(right);
                    if (riItem.isPermaRemove())
                        continue;

                    if (riItem.isRemovable()) {
                        remove.add(riItem);

                        if (riItem.originLeft != null) {
                            int oleft = findItem(riItem.originLeft);
                            content.get(oleft).decreaseReference();
                        }
                        if (riItem.originRight != null) {
                            int oright = findItem(riItem.originRight);
                            content.get(oright).decreaseReference();
                        }
                    }
                }
            }

            // perma remove
            for (int i = 0; i < remove.size(); i++) {
                remove.get(i).permaRemove();
            }

            if (fromWait && remove.size() > 0)
                crdtListener.onCRDTRemove(remove.toArray(new CRDTItem[] {}));
        }
    }

    Pair<Integer, CRDTItem> checkLeftItem(int leftpos) {
        for (; leftpos >= 0; leftpos--) {
            CRDTItem lItem = content.get(leftpos);
            if (!lItem.isPermaRemove()) {
                return new Pair<Integer, CRDTItem>(leftpos, lItem);
            }
        }
        return new Pair<Integer, CRDTItem>(leftpos, null);
    }

    Pair<Integer, CRDTItem> checkRightItem(int rightpos) {
        for (; rightpos < content.size(); rightpos++) {
            CRDTItem rItem = content.get(rightpos);
            if (!rItem.isPermaRemove()) {
                return new Pair<Integer, CRDTItem>(rightpos, rItem);
            }
        }
        return new Pair<Integer, CRDTItem>(rightpos, null);
    }

    void integrate(CRDTItem item, int idx_hint, boolean fromWait) {
        int shouldProcessSeq = getNextSeq(item.id.agent);
        if (shouldProcessSeq != item.id.seq) {
            System.out.println(
                    String.format("Should see operation seq #%v, but saw #%v instead", shouldProcessSeq, item.id.seq));
            return;
        }
        // System.out.println(item.id.agent);
        version.put(item.id.agent, item.id.seq);
        // if(item.originLeft != null)System.out.println(item.originLeft.agent);
        int left = findItem(item.originLeft, idx_hint - 1);
        // System.out.println(left);
        int destIdx = left + 1;
        int right = item.originRight == null ? content.size() : findItem(item.originRight, idx_hint);

        // shift origin if tombstone and increase reference
        if (item.originLeft != null) {
            Pair<Integer, CRDTItem> itemLeft = checkLeftItem(left);
            left = itemLeft.getFirst();
            if (itemLeft.getSecond() != null) {
                item.originLeft = itemLeft.getSecond().id;
                itemLeft.getSecond().increaseReference();
            }
        }
        if (item.originRight != null) {
            Pair<Integer, CRDTItem> itemRight = checkRightItem(right);
            right = itemRight.getFirst();
            if (itemRight.getSecond() != null) {
                item.originRight = itemRight.getSecond().id;
                itemRight.getSecond().increaseReference();
            }
        }

        boolean scanning = false;
        // System.out.println(right);
        for (int i = destIdx;; ++i) {
            if (!scanning)
                destIdx = i;
            if (i == content.size())
                break;
            if (i == right)
                break;
            CRDTItem other = content.get(i);
            int oleft = findItem(other.originLeft, idx_hint - 1);
            int oright = other.originRight == null ? content.size() : findItem(other.originRight, idx_hint);
            if (oleft < left)
                break;
            else if (oleft == left) {
                if (oright < right) {
                    scanning = true;
                    continue;
                } else if (oright == right) {
                    if (item.id.agent.compareTo(other.id.agent) > 0) // compare site winner
                        break;
                    else {
                        scanning = false;
                        continue;
                    }
                } else {
                    scanning = false;
                    continue;
                }
            }
        }
        content.add(destIdx, item);
        if (!item.isDeleted) {
            length++;
        }
        if (fromWait)
            crdtListener.onCRDTInsert(item);
    }

    public List<CRDTItem> returnCopy() {
        try {
            lock.lock();
            return content;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        try {
            lock.lock();
            StringBuilder result = new StringBuilder();
            for (CRDTItem item : content) {
                if (item.isDeleted == false) {
                    result.append(item.value);
                }
            }
            return result.toString();
        } finally {
            lock.unlock();
        }

    }
}