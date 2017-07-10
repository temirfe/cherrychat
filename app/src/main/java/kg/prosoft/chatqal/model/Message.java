package kg.prosoft.chatqal.model;

/**
 * Created by ProsoftPC on 4/17/2017.
 */

import java.io.Serializable;

public class Message implements Serializable {
    String id, message, createdAt, sender_id, receiver_id, uniqueId;
    int status;
    User user;

    public Message() {
    }

    public Message(String id, String message, String createdAt, String user_id, int status, String uniqueId) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
        this.sender_id = user_id;
        this.status=status;
        this.uniqueId=uniqueId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getSenderId() {
        return sender_id;
    }
    public void setSenderId(String sender_id) {
        this.sender_id = sender_id;
    }
    public String getReceiverId() {
        return sender_id;
    }
    public void setReceiverId(String receiver_id) {
        this.receiver_id = receiver_id;
    }

    public String getUniqueId() {
        return uniqueId;
    }
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}