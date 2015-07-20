package basilica2.social.data;

public class Message {

    private String from;
    private String text;

    public Message(String f, String t) {
        from = f;
        text = t;
    }

    @Override
    public String toString() {
        return "<Message from=\"" + from + "\">" + text + "</Message>";
    }
}
