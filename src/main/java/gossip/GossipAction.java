package gossip;

public class GossipAction {
    ActionType actionType;
    private String senderId;
    // If this is a PING_REQ this is the node that the ping should be passed on to
    // If this is an ACK, this is the node whose aliveness is being reported
    private String target;
    private Message[] messages;

    public GossipAction(ActionType actionType, String senderId, String target) {
        this.actionType = actionType;
        this.senderId = senderId;
        this.target = target;
        this.messages = new Message[0];
    }

    public GossipAction(ActionType actionType, String senderId, String target, Message[] messages) {
        this.actionType = actionType;
        this.senderId = senderId;
        this.target = target;
        this.messages = messages;
    }

    public String getTarget() {
        return target;
    }

    public Message[] getMessages() {
        return messages;
    }

    public String getSenderId() {
        return senderId;
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
        return "GossipAction{actionType=" + actionType + ", senderId=" + getSenderId() + ", target=" + getTarget() +
                ", messages=" + getMessages() + "}";
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
        if (messages.length != a.messages.length) {
            return false;
        }
        for (int i = 0; i < messages.length; i++) {
            if (!messages[i].equals(a.messages[i])) {
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
