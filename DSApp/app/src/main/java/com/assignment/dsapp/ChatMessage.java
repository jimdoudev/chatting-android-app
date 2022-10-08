package com.assignment.dsapp;

import java.io.File;
import java.io.Serializable;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    int ID;
    String SenderName;
    String Date;
    String Time;
    byte [] FileContent;
    String message;
    int ValueID;
    String TopicName;
    int SenderID;
    int NoChunks;
    int fileLength;
    String FileName;

    public ChatMessage(int ID, int SenderID, String SenderName, String Date, String Time, String TopicName, int ValueID, String message) {
        this.ID = ID;
        this.SenderName = SenderName;
        this.Date = Date;
        this.Time = Time;
        this.message = message;
        this.ValueID = ValueID;
        this.TopicName = TopicName;
        this.SenderID = SenderID;
    }

    public ChatMessage(int SenderID, String SenderName, String Date, String Time, String TopicName, int ValueID, String message, byte[] FileContent, int NoChunks, int fileLength, String Filename) {
        this.SenderName = SenderName;
        this.Date = Date;
        this.Time = Time;
        this.message = message;
        this.ValueID = ValueID;
        this.TopicName = TopicName;
        this.SenderID = SenderID;
        this.FileContent = FileContent;
        this.NoChunks = NoChunks;
        this.fileLength = fileLength;
        this.FileName = Filename;
    }

    public ChatMessage(int SenderID, String SenderName, String Date, String Time, String TopicName, int ValueID, String message) {
        this.SenderName = SenderName;
        this.Date = Date;
        this.Time = Time;
        this.message = message;
        this.ValueID = ValueID;
        this.TopicName = TopicName;
        this.SenderID = SenderID;
    }

    public ChatMessage(int ID, int SenderID, String SenderName, String Date, String Time, String TopicName, int ValueID, String message, String Filename, int NoChunks) {
        this.ID = ID;
        this.SenderName = SenderName;
        this.Date = Date;
        this.Time = Time;
        this.message = message;
        this.ValueID = ValueID;
        this.TopicName = TopicName;
        this.SenderID = SenderID;
        this.NoChunks = NoChunks;
        this.FileName = Filename;
    }
}
