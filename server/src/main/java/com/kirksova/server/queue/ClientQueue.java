package com.kirksova.server.queue;


import com.kirksova.server.model.User;
import org.springframework.stereotype.Component;

@Component
public class ClientQueue {

    private ObjectBox first = null;
    private ObjectBox last = null;
    private int size = 0;

    public void push(User client) {
        ObjectBox ob = new ObjectBox();
        ob.setClient(client);
        if (first == null) {
            first = ob;
        } else {
            last.setNext(ob);
        }
        last = ob;
        size++;
    }

    public User pull() {
        if (size == 0) {
            return null;
        }
        User client = first.getClient();
        first = first.getNext();
        if (first == null) {
            last = null;
        }
        size--;
        return client;
    }

    public User get(int index) {
        if(size == 0 || index >= size || index < 0) {
            return null;
        }
        ObjectBox current = first;
        int pos = 0;
        while(pos < index) {
            current = current.getNext();
            pos++;
        }
        return current.getClient();
    }

    public int size() {
        return size;
    }

    private class ObjectBox
    {
        private User client;
        private ObjectBox next;

        public User getClient() {
            return client;
        }

        public void setClient(User client) {
            this.client = client;
        }

        public ObjectBox getNext() {
            return next;
        }

        public void setNext(ObjectBox next) {
            this.next = next;
        }
    }
}
