package com.divinepet.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class State {
    public String updID;
    public String senderID;
    public String receiverID;
    public boolean waitAnswer;
    public long editMsgID;
    public String editMsg;
}
