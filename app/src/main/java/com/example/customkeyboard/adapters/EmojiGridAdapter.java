package com.example.customkeyboard.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.customkeyboard.R;

import java.util.List;

public class EmojiGridAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> emojis;

    public EmojiGridAdapter(Context context, List<String> emojis) {
        this.context = context;
        this.emojis = emojis;
    }

    @Override
    public int getCount() {
        return emojis.size();
    }

    @Override
    public Object getItem(int i) {
        return emojis.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.emoji_preview, viewGroup, false);
        }

        TextView emojiText = (TextView) view.findViewById(R.id.preview_text);
        emojiText.setText(String.valueOf(emojis.get(i)));

        return view;
    }
}