package app.chatclientandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter msgAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.msgs_pane);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        msgAdapter = MessageAdapter.getInstance();
        recyclerView.setAdapter(msgAdapter);
        msgAdapter.addMsg("2017", "Unknown", "Hello!", MessageAdapter.MsgType.TYPE_RECEIVE);
        msgAdapter.addMsg("2017", "Tony", "t", MessageAdapter.MsgType.TYPE_SEND);
        msgAdapter.addMsg("2017 15 02", "Tony", "test send! HHHHHHHHHHHHHHHHH", MessageAdapter.MsgType.TYPE_SEND);
    }
}
