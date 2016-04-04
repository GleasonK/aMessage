package me.kevingleason.amessage;

/**
 * Created by GleasonK on 3/17/16.
 */
public class Config {
    public static final String PUB_KEY = "pub-c-0181d825-aa47-448a-bd4c-bcf1ba2a8623";  // Your Pub Key
    public static final String SUB_KEY = "sub-c-e4f06386-ec67-11e5-be6a-02ee2ddab7fe";  // Your Sub Key
    public static final String SECRET_KEY = "sec-c-YTA5NzM3YzQtYjc5Yi00ZjBlLTkxOGYtZGExNzhiZGY5YmEx"; // Your Secret Key

    public static final String PN_TYPE     = "type";
    public static final String PN_NAME     = "name";
    public static final String PN_DISABLE  = "disabled"; //To unsubscribe
    public static final String PN_INCOMING = "incoming"; //For outgoing messages
    public static final String PN_OUTGOING = "outgoing"; //For outgoing messages
    public static final String PN_RECEIPT  = "receipt";  //For Sent/Failure
    public static final String PN_ISSENT   = "is_sent";  //For Sent/Failure
    public static final String PN_TIMESTAMP= "timestamp";//For Sent/Failure
    public static final String PN_MESSAGE  = "message";
    public static final String PN_SENDER   = "sender";
    public static final String PN_NUMBER   = "number";
}
