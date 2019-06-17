package gossip;

import java.io.*;

public class Encoder {
    public static byte[] encode(GossipAction action) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        DataOutputStream bufWriter = new DataOutputStream(buf);
        bufWriter.writeByte(action.actionType.ordinal());
        bufWriter.writeByte(action.getSenderId().length());
        bufWriter.write(action.getSenderId().getBytes());
        bufWriter.writeByte(action.getTarget().length());
        bufWriter.write(action.getTarget().getBytes());

        bufWriter.writeInt(action.getMessages().length);
        for (GossipAction.Message message : action.getMessages()) {
            bufWriter.writeByte(message.messageType.ordinal());
            bufWriter.writeInt(message.incarnationNumber);
            bufWriter.writeByte(message.target.length());
            bufWriter.write(message.target.getBytes());
        }

        return buf.toByteArray();
    }

    public static GossipAction decode(byte[] buf) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        DataInputStream in = new DataInputStream(bis);

        GossipAction.ActionType actionType = GossipAction.ActionType.values()[in.readByte()];

        byte senderSize = in.readByte();
        byte[] senderBuf = new byte[senderSize];
        in.readFully(senderBuf);

        byte targetSize = in.readByte();
        byte[] targetBuf = new byte[targetSize];
        in.readFully(targetBuf);

        // right now only one message can be piggybacked on a GossipAction at a time
        int nMessages = in.readInt();

        GossipAction.Message[] messages = new GossipAction.Message[nMessages];
        for (int readMessages = 0; readMessages < nMessages; readMessages++){
            byte messageType;
            try {
                messageType = in.readByte();
            } catch (EOFException e) {
                break;
            }
            int incarnationNumber = in.readInt();
            byte msgTargetSize = in.readByte();
            byte[] msgTargetBuf = new byte[msgTargetSize];
            in.readFully(msgTargetBuf);

            messages[readMessages] = new GossipAction.Message(GossipAction.MessageType.values()[messageType],
                    incarnationNumber, new String(msgTargetBuf));
        }

        return new GossipAction(actionType, new String(senderBuf), new String(targetBuf), messages);
    }
}

