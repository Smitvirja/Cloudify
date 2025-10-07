package com.example.notes_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.notes_app.Models.users;
import com.example.notes_app.R;
import com.example.notes_app.listeners.UserListener;

import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<users> userList;
    private final UserListener userListener;

    public UserAdapter(UserListener userListener) {
        this.userList = new ArrayList<>();
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(userList.get(position));
        holder.resetPasswordButton.setOnClickListener(v -> userListener.onResetPasswordClicked(userList.get(position)));
        holder.deleteUserButton.setOnClickListener(v -> userListener.onDeleteUserClicked(userList.get(position)));
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public void setUsers(List<users> users) {
        this.userList = users;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userEmail, userRole;
        ImageView userAvatar;
        Button resetPasswordButton, deleteUserButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            userEmail = itemView.findViewById(R.id.user_email);
            userRole = itemView.findViewById(R.id.user_role);
            userAvatar = itemView.findViewById(R.id.user_avatar); // Make sure this exists in user_card.xml
            resetPasswordButton = itemView.findViewById(R.id.reset_password_button);
            deleteUserButton = itemView.findViewById(R.id.delete_user_button);
        }

        void bind(users user) {
            userName.setText(user.getUsername());
            userEmail.setText(user.getEmail());
            userRole.setText("Role: " + (user.isAdmin() ? "Admin" : "User"));
            Glide.with(itemView.getContext())
                    .load(user.getProfile_icon()) // Make sure users class has `getProfileImageUrl()`
                    .placeholder(R.drawable.avatar)
                    .apply(RequestOptions.circleCropTransform())
                    .error(R.drawable.avatar) // Error image in case of failure
                    .into(userAvatar);// Assuming there's a method isAdmin in your users class
        }
    }
}
