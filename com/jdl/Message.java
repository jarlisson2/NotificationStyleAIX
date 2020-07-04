package com.jdl.NotificationStyle;

public class Message {
    private CharSequence text;
    private long timestamp;
    private CharSequence sender;
    public Message(CharSequence text, CharSequence sender, long timestamp) {
        this.text = text;
        this.sender = sender;
        this.timestamp = timestamp;
    }
    public CharSequence getText() {
        return text;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public CharSequence getSender() {
        return sender;
    }
}