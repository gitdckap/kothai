package com.example.customkeyboard.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.customkeyboard.R;
import com.example.customkeyboard.model.Emoji;

import java.util.List;

public class EmojiAdapter extends RecyclerView.Adapter<EmojiAdapter.EmojiViewHolder> {

    private final Context context;
    private List<Emoji> emojis;
    LayoutInflater mInflater;
    OnEmojiClickListener listener;

    public interface OnEmojiClickListener {
        void onEmojiClick(String value);
    }

    public EmojiAdapter(Context context, List<Emoji> emojis, OnEmojiClickListener listener) {
        this.mInflater = LayoutInflater.from(context);
        this.context = context;
        this.emojis = emojis;
        this.listener = listener;
    }

    // Update data method
    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<Emoji> newData) {
        this.emojis = newData;
        notifyDataSetChanged(); // Or use DiffUtil for better performance
    }

    @NonNull
    @Override
    public EmojiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.single_emoji_item, parent, false);
        return new EmojiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmojiViewHolder holder, int position) {
        holder.emojiText.setText(emojis.get(position).getTitle());
        EmojiGridAdapter emojiGridAdapter = new EmojiGridAdapter(context, emojis.get(position).getEmojis());
        holder.gridView.setAdapter(emojiGridAdapter);
        holder.gridView.setOnItemClickListener((adapterView, view, i, l) -> {
            listener.onEmojiClick(emojis.get(position).getEmojis().get(i));
        });
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return emojis.size();
    }

    public static class EmojiViewHolder extends RecyclerView.ViewHolder {
        TextView emojiText;
        GridView gridView;

        public EmojiViewHolder(@NonNull View itemView) {
            super(itemView);
            emojiText = (TextView) itemView.findViewById(R.id.category_name);
            gridView = (GridView) itemView.findViewById(R.id.emojiGridView);
        }
    }
}

