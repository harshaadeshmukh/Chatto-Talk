package com.example.chatto;

import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatto.adapter.ChatRecyclerAdapter;
import com.example.chatto.adapter.SearchUserRecyclerAdapter;
import com.example.chatto.model.ChatMessageModel;
import com.example.chatto.model.ChatroomModel;
import com.example.chatto.model.UserModel;
import com.example.chatto.utils.AndroidUtil;
import com.example.chatto.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class ChatActivity extends AppCompatActivity {


    UserModel otherUser;
    String chatroomID;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter adapter;

    EditText messageInput;
    ImageButton sendMessageBtn , backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomID = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(),otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_image_view);


        FirebaseUtil.getOtherProfilePicStorageRef(otherUser.getUserId()).getDownloadUrl()
                .addOnCompleteListener(t -> {
                    if(t.isSuccessful()){
                        Uri uri  = t.getResult();
                        AndroidUtil.setProfilePic(this,uri,imageView);
                    }
                });


        backBtn.setOnClickListener(v -> onBackPressed());

        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if(message.isEmpty())
            {
                return;
            }
            sendMessageToUser(message);
        });

        getOrCreateChatroomModel();

        setUpChatRecyclerView();

    }
    void setUpChatRecyclerView()
    {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomID)
                .orderBy("timestamp" , Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }
    void sendMessageToUser(String message)
    {
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomID).set(chatroomModel);



        ChatMessageModel chatMessageModel = new ChatMessageModel(message,FirebaseUtil.currentUserId(),Timestamp.now());


        FirebaseUtil.getChatroomMessageReference(chatroomID).add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful())
                    {
                        messageInput.setText("");
                    }
                });
    }

    void getOrCreateChatroomModel()
    {
        FirebaseUtil.getChatroomReference(chatroomID).get().addOnCompleteListener(task -> {

            if(task.isSuccessful())
            {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if(chatroomModel == null)
                {
                    chatroomModel = new ChatroomModel(
                            chatroomID,
                            Arrays.asList(FirebaseUtil.currentUserId(),otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatroomReference(chatroomID).set(chatroomModel);
                }
            }
        });
    }
}