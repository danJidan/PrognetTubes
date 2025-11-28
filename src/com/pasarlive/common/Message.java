package com.pasarlive.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Message class untuk protokol komunikasi client-server
 * Implements Serializable untuk transfer via ObjectInputStream/ObjectOutputStream
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Protocol Types
    public enum Type {
        REQUEST,
        RESPONSE,
        BROADCAST
    }
    
    // Actions
    public enum Action {
        GET_COMMODITY_LIST,
        FILTER_COMMODITY,
        UPDATE_PRICE,
        CREATE_COMMODITY,
        DELETE_COMMODITY,
        SEND_REPORT,
        GET_REPORTS,
        PRICE_UPDATE  // untuk broadcast
    }
    
    private Type type;
    private Action action;
    private Map<String, Object> data;
    private boolean success;
    private String errorMessage;
    private long timestamp;
    
    /**
     * Constructor untuk membuat message baru
     */
    public Message(Type type, Action action) {
        this.type = type;
        this.action = action;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
        this.success = true;
    }
    
    /**
     * Constructor untuk response dengan status
     */
    public Message(Type type, Action action, boolean success) {
        this(type, action);
        this.success = success;
    }
    
    // Getters and Setters
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
    
    public Action getAction() {
        return action;
    }
    
    public void setAction(Action action) {
        this.action = action;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    /**
     * Tambah data ke message
     */
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
    
    /**
     * Ambil data dari message
     */
    public Object getData(String key) {
        return this.data.get(key);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.success = false;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", action=" + action +
                ", success=" + success +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}
