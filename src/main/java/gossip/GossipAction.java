package gossip;

import java.util.ArrayList;
import java.util.List;

public class GossipAction {
    ActionType actionType;
    private String senderId;
    // If this is a PING_REQ this is the node that the ping should be passed on to
    // If this is an ACK, this is the node whose aliveness is being reported
    private String target;
    private List<Message> messages;

    static int MAX_MESSAGES = 1;

    public GossipAction(ActionType actionType, String senderId, String target) {
        this.actionType = actionType;
        this.senderId = senderId;
        this.target = target;
        this.messages = new ArrayList<>();
    }

    public GossipAction(ActionType actionType, String senderId, String target, List<Message> messages) {
        this.actionType = actionType;
        this.senderId = senderId;
        this.target = target;
        this.messages = messages;
    }

    public String getTarget() {
        return target;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public String getSenderId() {
        return senderId;
    }

    public void addMessage(int i, Message message) {
        this.messages.add(i, message);
    }

    public enum ActionType {
        PING, ACK, PING_REQ
    }

    public enum MessageType {
        ALIVE, SUSPECT, CONFIRM
    }

    public static class Message {
        MessageType messageType;
        int incarnationNumber;
        String target; // the node whose liveness this message is describing

        public Message(MessageType messageType, int incarnationNumber, String target) {
            this.messageType = messageType;
            this.incarnationNumber = incarnationNumber;
            this.target = target;
        }

        @Override
        public String toString() {
            return "GossipAction.Message{messageType=" + messageType + ", incarnationNumber=" + incarnationNumber +
                    ", target=" + target + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Message m = (Message) o;
            return messageType == m.messageType && incarnationNumber == m.incarnationNumber
                    && target.equals(m.target);
        }
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("GossipAction{actionType=" + actionType + ", senderId=" + getSenderId() + ", target=" + getTarget() +
                ", messages=[");
            for (int i = 0; i < messages.size(); i++) {
                buf.append(messages.get(i).toString());
                if (i < messages.size() - 1) {
                    buf.append(", ");
                }
            }
        buf.append("]}");
        return buf.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GossipAction a = (GossipAction) o;
        if (messages.size() != a.messages.size()) {
            return false;
        }
        for (int i = 0; i < messages.size(); i++) {
            if (!messages.get(i).equals(a.messages.get(i))) {
                return false;
            }
        }

        return actionType == a.actionType && getSenderId().equals(a.getSenderId())
                && getTarget().equals(a.getTarget());
    }
}

// 1b   | 1b          | up to 255   | 1b
// type | sender size | sender size | target size

// Maximum total of 261
// 1b      | 4b          | 1b          | up to 255
// type    | incarnation | target size | target
