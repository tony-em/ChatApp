package app.chatclientandroid;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static MessageAdapter messageAdapter;

    private List<String[]> msgs = new ArrayList<>();
    private List<MsgType> msgsTypes = new ArrayList<>();

    public enum MsgType {
        TYPE_SEND, TYPE_RECEIVE
    }

    private MessageAdapter() {
    }

    public static MessageAdapter getInstance() {
        if (messageAdapter == null)
            return new MessageAdapter();
        else
            return messageAdapter;
    }

    public void addMsg(String datetime, String nickname, String msg, MsgType type) {
        msgs.add(new String[]{datetime, nickname, msg});
        msgsTypes.add(type);
        notifyDataSetChanged();
    }

    @Override
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;

        if (viewType == 0) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_send, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_receive, parent, false);
        }

        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MessageViewHolder holder, int position) {
        holder.datetime.setText(msgs.get(position)[0]);
        holder.nickname.setText(msgs.get(position)[1]);
        holder.msg.setText(msgs.get(position)[2]);
    }

    @Override
    public int getItemViewType(int position) {
        return (msgsTypes.get(position) == MsgType.TYPE_SEND) ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return msgs.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView datetime;
        private TextView nickname;
        private TextView msg;

        public MessageViewHolder(View itemView) {
            super(itemView);

            datetime = (TextView) itemView.findViewById(R.id.datetime);
            nickname = (TextView) itemView.findViewById(R.id.nickname);
            msg = (TextView) itemView.findViewById(R.id.msg);
        }
    }
}
