package com.example.customkeyboard.listners;

public interface OnClickListner {
    void onPress(String emoji);
    void onLongClick(boolean isLongClick);
    void onCheckBoxClick(String id);
}
