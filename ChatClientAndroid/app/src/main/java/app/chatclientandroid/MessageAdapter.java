package app.chatclientandroid;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<String[]> msgs;
    private List<MessageDispatcher.MsgState> msgsStates;

    public MessageAdapter(List<String[]> msgs, List<MessageDispatcher.MsgState> msgsStates) {
        validationData(msgs, msgsStates);

        this.msgs = msgs;
        this.msgsStates = msgsStates;
    }

    public void setupData(List<String[]> msgs, List<MessageDispatcher.MsgState> msgsTypes) {
        validationData(msgs, msgsTypes);

        this.msgs = msgs;
        this.msgsStates = msgsTypes;

        notifyDataSetChanged();
    }

    public List<String[]> getMessagesData() {
        return msgs;
    }

    public List<MessageDispatcher.MsgState> getMessagesTypesData() {
        return msgsStates;
    }

    public void clearData() {
        msgs.clear();
        msgsStates.clear();

        notifyDataSetChanged();
    }

    private void validationData(List<String[]> msgs, List<MessageDispatcher.MsgState> msgsTypes) {
        if (msgs == null || msgsTypes == null) {
            throw new NullPointerException("Lists is null");
        }

        if (msgs.size() != msgsTypes.size()) {
            throw new NullPointerException("MsgsList and MsgsTypesList length must be equal");
        }

        if (!msgs.isEmpty() && msgs.get(0).length != 3) {
            throw new NullPointerException("MsgsList item structure must be 0:datetime, 1:nickname, 2:msg");
        }
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
    public int getItemCount() {
        return msgs.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (msgsStates.get(position) == MessageDispatcher.MsgState.STATE_MY) ? 0 : 1;
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
