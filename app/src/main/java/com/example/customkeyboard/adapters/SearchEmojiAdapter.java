package com.example.customkeyboard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.customkeyboard.R;
import com.vdurmont.emoji.Emoji;

import java.util.List;

public class SearchEmojiAdapter extends RecyclerView.Adapter<SearchEmojiAdapter.SearchEmojiViewHolder> {

    Context context;
    List<Emoji> emojiList;
    LayoutInflater mInflater;
    OnEmojiClickListener listener;

    public interface OnEmojiClickListener {
        void onEmojiClick(String value);
    }

    public SearchEmojiAdapter(Context context, List<Emoji> emojiList, OnEmojiClickListener listener) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.emojiList = emojiList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SearchEmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.single_search_emoji_item, parent, false);
        return new SearchEmojiAdapter.SearchEmojiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchEmojiViewHolder holder, int position) {
        holder.emojiText.setText(emojiList.get(position).getUnicode());
        holder.emojiText.setOnClickListener(view -> listener.onEmojiClick(emojiList.get(position).getUnicode()));
    }

    @Override
    public int getItemCount() {
        return emojiList.size();
    }

    public static class SearchEmojiViewHolder extends RecyclerView.ViewHolder {
        TextView emojiText;

        public SearchEmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiText = (TextView) itemView.findViewById(R.id.emoji);
        }
    }
}
