package kg.prosoft.chatqal.app;

/**
 * Created by ProsoftPC on 4/17/2017.
 */

public class EndPoints {

    public static final String BASE_URL = "http://api.temirbek.com";
    public static final String LOGIN = BASE_URL + "/chatusers/login";
    public static final String USER = BASE_URL + "/chatusers/_ID_";
    public static final String RECEIVERS = BASE_URL + "/chatusers";
    public static final String USER_MESSAGE = BASE_URL + "/chatusers/message";
    public static final String CHAT_ROOMS = BASE_URL + "/chatrooms";
    public static final String CHATROOM_THREAD = BASE_URL + "/chatrooms/_ID_";
    public static final String CHAT_THREAD = BASE_URL + "/chatusers/private";
    public static final String MSG_STATUS = BASE_URL + "/chatusers/status";
    public static final String CHAT_ROOM_MESSAGE = BASE_URL + "/chatrooms/message";
}
