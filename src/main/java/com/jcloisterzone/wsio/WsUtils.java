package com.jcloisterzone.wsio;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.jcloisterzone.wsio.message.CreateGameMessage;
import com.jcloisterzone.wsio.message.ErrorMessage;
import com.jcloisterzone.wsio.message.FlierDiceMessage;
import com.jcloisterzone.wsio.message.GameMessage;
import com.jcloisterzone.wsio.message.GameSetupMessage;
import com.jcloisterzone.wsio.message.GetRandSampleMessage;
import com.jcloisterzone.wsio.message.HelloMessage;
import com.jcloisterzone.wsio.message.JoinGameMessage;
import com.jcloisterzone.wsio.message.LeaveSlotMessage;
import com.jcloisterzone.wsio.message.RandSampleMessage;
import com.jcloisterzone.wsio.message.RmiMessage;
import com.jcloisterzone.wsio.message.RollFlierDiceMessage;
import com.jcloisterzone.wsio.message.SetExpansionMessage;
import com.jcloisterzone.wsio.message.SetRuleMessage;
import com.jcloisterzone.wsio.message.SlotMessage;
import com.jcloisterzone.wsio.message.StartGameMessage;
import com.jcloisterzone.wsio.message.TakeSlotMessage;
import com.jcloisterzone.wsio.message.WelcomeMessage;
import com.jcloisterzone.wsio.message.WsMessage;

public final class WsUtils {

    private Gson gson = new Gson();
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, Class<? extends WsMessage>> types = new HashMap<>();

    public static String getCmdName(Class<? extends WsMessage> msgType) {
        return msgType.getAnnotation(Cmd.class).value();
    }


    public WsUtils() {
        registerMsgType(ErrorMessage.class);
        registerMsgType(HelloMessage.class);
        registerMsgType(WelcomeMessage.class);
        registerMsgType(CreateGameMessage.class);
        registerMsgType(JoinGameMessage.class);
        registerMsgType(GameMessage.class);
        registerMsgType(GameSetupMessage.class);
        registerMsgType(TakeSlotMessage.class);
        registerMsgType(LeaveSlotMessage.class);
        registerMsgType(SlotMessage.class);
        registerMsgType(SetExpansionMessage.class);
        registerMsgType(SetRuleMessage.class);
        registerMsgType(StartGameMessage.class);
        registerMsgType(GetRandSampleMessage.class);
        registerMsgType(RandSampleMessage.class);
        registerMsgType(RollFlierDiceMessage.class);
        registerMsgType(FlierDiceMessage.class);
        registerMsgType(RmiMessage.class);
    }

    private void registerMsgType(Class<? extends WsMessage> type) {
        types.put(getCmdName(type), type);
    }

    public WsMessage fromJson(String payload) {
        String s[] = payload.split(" ", 2); //command, arg
        Class<? extends WsMessage> type = types.get(s[0]);
        if (type == null) {
            throw new IllegalArgumentException("Mapping type is not declared for "+s[0]);
        }
        return (WsMessage) gson.fromJson(s[1], type);
    }

    public String toJson(WsMessage arg) {
        return getCmdName(arg.getClass()) + " " + gson.toJson(arg);
    }

    //TODO what about cache targets
    public void delegate(Object target, Object subject, WsMessage msg) {
        Class<?> type = target.getClass();
        while (true) {
            if (delegateScanClass(target, type, subject, msg)) break;
            type = type.getSuperclass();
            if (Object.class.equals(type)) break;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean delegateScanClass(Object target, Class<?> type, Object subject,  WsMessage msg) {
        for (Method m : type.getDeclaredMethods()) {
            CmdHandler handler = m.getAnnotation(CmdHandler.class);
            if (handler != null) {
                Class<? extends WsMessage> cls = (Class<? extends WsMessage>) m.getParameterTypes()[1];
                if (cls.equals(msg.getClass())) {
                    try {
                        m.invoke(target, subject, msg);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    return true;
                }
            }
        }
        return false;
    }

}