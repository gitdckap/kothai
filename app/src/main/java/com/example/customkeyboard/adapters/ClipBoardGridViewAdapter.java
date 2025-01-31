package com.example.customkeyboard.adapters;

import static com.example.customkeyboard.utils.Utils.isLongPress;
import static com.example.customkeyboard.utils.Utils.isSelectAll;
import static com.example.customkeyboard.utils.Utils.spToPx;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.example.customkeyboard.R;
import com.example.customkeyboard.listners.OnClickListner;
import com.example.customkeyboard.model.Clipboard;

import java.util.List;

public class ClipBoardGridViewAdapter extends BaseAdapter {
    private final Context context;
    private final List<Clipboard> contentList;
    private final OnClickListner onClickListner;
    private final List<String> selectIDs;

    public ClipBoardGridViewAdapter(Context context, List<Clipboard> contentList, List<String> selectIDs, OnClickListner onClickListner) {
        this.context = context;
        this.contentList = contentList;
        this.onClickListner = onClickListner;
        this.selectIDs = selectIDs;
    }

    @Override
    public int getCount() {
        return contentList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 250));
        linearLayout.setBackground(context.getDrawable(R.drawable.clipboarditem));
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        CheckBox checkBox = new CheckBox(context);
        if (isSelectAll) {
            checkBox.setChecked(true);
        } else {
            boolean isChecked = selectIDs.stream().anyMatch(string -> string.equals(contentList.get(i).getId()));
            checkBox.setChecked(isChecked);
        }

        checkBox.setOnClickListener(view1 -> onClickListner.onCheckBoxClick(contentList.get(i).getId()));
        TextView textView = new TextView(context);
        textView.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f));
        textView.setText(contentList.get(i).getTextValue());
        textView.setPadding(10, 0, 10, 0);
        textView.setTextColor(Color.BLACK);
        textView.setTypeface(ResourcesCompat.getFont(context, R.font.inter));
        textView.setTextSize(context.getResources().getDimension(R.dimen.key_letter_8sp));
        linearLayout.addView(textView);
        if (isLongPress) linearLayout.addView(checkBox);
        linearLayout.setOnClickListener(view1 -> {
            if (isLongPress) {
                onClickListner.onCheckBoxClick(contentList.get(i).getId());
            } else {
                onClickListner.onPress(contentList.get(i).getTextValue());
            }
        });
        linearLayout.setOnLongClickListener((view1) -> {
            onClickListner.onLongClick(true);
            notifyDataSetChanged();
            return true;
        });
        return linearLayout;
    }
}
